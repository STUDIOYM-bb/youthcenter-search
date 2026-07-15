package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.FakeRegionData;
import com.themoa.youthcentersearch.policy.region.RegionAliasCatalog;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.policy.region.RegionNormalizer;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.rag.dto.PolicySearchRequest;
import org.junit.jupiter.api.Test;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRagSearchServicePipelineTest {
    @Test
    void suwonJobSeekerSearchKeepsParentSidoInterviewPolicyAndDropsIrrelevantOrOtherRegionPolicies() {
        Policy gyeonggiInterview = policy(1, "경기도 청년 면접수당", "청년 면접수당과 취업 준비 비용 지원",
                PolicyCategory.일자리, condition(18, 39, null, "면접 취업 준비"), region("41"));
        Policy suwonFood = policy(2, "수원시 농식품 바우처", "식품 바우처와 영양 지원",
                PolicyCategory.복지, condition(18, 34, null, "식품 바우처"), region("41110"));
        Policy seoulInterview = policy(3, "서울 청년 면접수당", "청년 면접비 지원",
                PolicyCategory.일자리, condition(18, 39, null, "면접 구직"), region("11"));
        Policy nationwideJob = policy(4, "전국 청년 구직활동 지원", "구직활동과 취업 역량 강화",
                PolicyCategory.일자리, condition(18, 39, "UNEMPLOYED", "구직활동 취업"), region("KR"));

        PolicyRagSearchService service = service(List.of(gyeonggiInterview, suwonFood, seoulInterview, nationwideJob));

        var response = service.search(new PolicySearchRequest("수원 사는 27살 취준생 정책", 10));

        assertThat(response.results()).extracting("policyId").contains(1, 4).doesNotContain(3);
        assertThat(response.results()).extracting("policyId").doesNotContain(2);
        assertThat(response.results().get(0).policyId()).isEqualTo(1);
        var gyeonggi = response.results().stream().filter(item -> item.policyId().equals(1)).findFirst().orElseThrow();
        assertThat(gyeonggi.regionCompatibility()).isEqualTo("PARENT_SIDO");
        assertThat(gyeonggi.ageMatchStatus()).isEqualTo("MATCH");
        assertThat(gyeonggi.employmentMatchStatus()).isEqualTo("UNKNOWN");
        assertThat(gyeonggi.needCheckReasons()).noneMatch(reason -> reason.equals("EMPLOYMENT_NOT_MATCHED"));
        assertThat(response.diagnostics().topicThresholdFailedCount()).isGreaterThanOrEqualTo(1);
    }

    private PolicyRagSearchService service(List<Policy> policies) {
        CompositePolicySearchConditionParser parser = mock(CompositePolicySearchConditionParser.class);
        when(parser.parse(any(), any())).thenReturn(new PolicySearchConditionParser.ParsedPolicySearchCondition(condition(), "TEST", false, null));
        PolicyRepository repository = mock(PolicyRepository.class);
        Map<Integer, Policy> byId = new LinkedHashMap<>();
        policies.forEach(policy -> byId.put(policy.getId(), policy));
        when(repository.findWithRelationsByIdIn(any())).thenAnswer(invocation -> ((List<Integer>) invocation.getArgument(0)).stream()
                .map(byId::get)
                .filter(java.util.Objects::nonNull)
                .toList());
        when(repository.findById(anyInt())).thenAnswer(invocation -> Optional.ofNullable(byId.get(invocation.getArgument(0))));
        @SuppressWarnings("unchecked")
        ObjectProvider<VectorStore> vectorStoreProvider = mock(ObjectProvider.class);
        when(vectorStoreProvider.getIfAvailable()).thenReturn(null);

        RagProperties properties = new RagProperties();
        properties.setEnabled(true);
        PolicyLexicalSearchService lexical = mock(PolicyLexicalSearchService.class);
        when(lexical.search(any(), any(), anyInt())).thenReturn(new PolicyLexicalSearchService.LexicalSearchResult(
                List.of(1, 2, 3, 4),
                Map.of(1, 0.85, 2, 0.1, 3, 0.85, 4, 0.75),
                Map.of(1, 0.95, 2, 0.0, 3, 0.9, 4, 0.6),
                Map.of(1, Set.of(CandidateSource.MYSQL_TITLE), 2, Set.of(CandidateSource.MYSQL_KEYWORD),
                        3, Set.of(CandidateSource.MYSQL_TITLE), 4, Set.of(CandidateSource.MYSQL_KEYWORD)),
                Map.of(CandidateSource.MYSQL_TITLE, 2, CandidateSource.MYSQL_KEYWORD, 2)));

        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        RegionCatalog catalog = new RegionCatalog(regionRepository(), aliases, normalizer);
        return new PolicyRagSearchService(parser, repository, vectorStoreProvider, properties,
                new RegionMatchEvaluator(catalog, normalizer), lexical, new PolicySearchIntentBuilder());
    }

    private PolicySearchCondition condition() {
        return new PolicySearchCondition("경기도", "수원시", null, 27, "UNEMPLOYED", null, null, "일자리",
                Set.of(), Set.of("청년", "취업 지원", "구직 지원"), Set.of("청년", "취업", "구직", "면접", "면접수당"),
                "수원", "EXACT", "SIGUNGU", Set.of(), true, true, true, false, true, false,
                PolicySearchMode.HYBRID, 10);
    }

    private Policy policy(int id, String title, String summary, PolicyCategory category, PolicyCondition condition, RegionCode region) {
        Policy policy = new Policy("P" + id);
        ReflectionTestUtils.setField(policy, "id", id);
        policy.updateBasic(title, "기관", category, summary, null, null, null, true, true, "OPEN");
        policy.updateCondition(condition);
        policy.getRegions().add(new PolicyRegion(policy, region));
        return policy;
    }

    private PolicyCondition condition(Integer minAge, Integer maxAge, String employment, String summary) {
        return new PolicyCondition(minAge, maxAge, employment, null, null, summary, false);
    }

    private RegionCode region(String code) {
        return FakeRegionData.regions().stream().filter(region -> code.equals(region.getRegionCode())).findFirst().orElseThrow();
    }

    private RegionCodeRepository regionRepository() {
        RegionCodeRepository repo = mock(RegionCodeRepository.class);
        var regions = FakeRegionData.regions();
        when(repo.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repo.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
        }
        return repo;
    }
}
