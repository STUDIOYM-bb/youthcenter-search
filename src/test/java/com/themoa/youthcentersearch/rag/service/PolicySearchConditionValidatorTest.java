package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.RegionAliasCatalog;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionNormalizer;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicySearchConditionValidatorTest {
    private final RegionCatalog regionCatalog = mock(RegionCatalog.class);
    private final RegionNormalizer regionNormalizer = new RegionNormalizer(new RegionAliasCatalog());
    private final PolicyKeywordNormalizer keywordNormalizer = new PolicyKeywordNormalizer();
    private final PolicySearchConditionValidator validator = new PolicySearchConditionValidator(
            new ExplicitConditionDetector(regionCatalog, regionNormalizer),
            new PolicyKeywordExtractor(new PolicyKeywordSynonymCatalog(), keywordNormalizer));

    @Test
    void keywordQueryDoesNotCreateHardConditions() {
        noRegionInQuery();
        var parsed = new PolicySearchCondition(null, null, null, 19, "UNEMPLOYED", null,
                null, "일자리", Set.of(), Set.of("청년", "면접", "수당"), 20);

        var condition = validator.validate("청년 면접 수당", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.KEYWORD);
        assertThat(condition.regionExplicit()).isFalse();
        assertThat(condition.ageExplicit()).isFalse();
        assertThat(condition.employmentExplicit()).isFalse();
        assertThat(condition.age()).isNull();
        assertThat(condition.employmentStatus()).isNull();
        assertThat(condition.keywords()).contains("청년", "면접수당");
        assertThat(condition.expandedKeywords()).contains("면접비", "면접 지원금");
    }

    @Test
    void regionAndKeywordQueryIsHybrid() {
        regionInQuery();
        var parsed = new PolicySearchCondition("경기도", null, null, null, null, null,
                null, "일자리", Set.of(), Set.of("청년", "면접", "수당"), 20);

        var condition = validator.validate("경기도 청년 면접 수당", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.HYBRID);
        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.province()).isEqualTo("경기도");
        assertThat(condition.ageExplicit()).isFalse();
    }

    @Test
    void interviewAllowanceDoesNotExpandToGenericGrantOnlyBecauseItContainsAllowance() {
        noRegionInQuery();
        var parsed = new PolicySearchCondition(null, null, null, null, null, null,
                null, null, Set.of(), Set.of(), 10);

        var condition = validator.validate("면접수당", parsed, 10);

        assertThat(condition.keywords()).contains("면접수당");
        assertThat(condition.keywords()).doesNotContain("지원금");
        assertThat(condition.expandedKeywords()).contains("면접비");
        assertThat(condition.expandedKeywords()).doesNotContain("지원금", "보조금", "장려금");
    }


    @Test
    void situationQueryKeepsExplicitConditions() {
        shortCityRegionInQuery();
        var parsed = new PolicySearchCondition("경기도", "수원시", null, 27, "UNEMPLOYED", null,
                null, "금융", Set.of("CASH"), Set.of("청년", "지원금"), 20);

        var condition = validator.validate("수원 사는 27살 무직 청년 지원금", parsed, 20);

        assertThat(condition.searchMode()).isEqualTo(PolicySearchMode.HYBRID);
        assertThat(condition.regionExplicit()).isTrue();
        assertThat(condition.ageExplicit()).isTrue();
        assertThat(condition.employmentExplicit()).isTrue();
        assertThat(condition.age()).isEqualTo(27);
        assertThat(condition.employmentStatus()).isEqualTo("UNEMPLOYED");
    }

    private void noRegionInQuery() {
        when(regionCatalog.findInText(anyString())).thenReturn(Set.of());
        when(regionCatalog.allSpecificRegionsByLongestName()).thenReturn(java.util.List.of());
    }

    private void regionInQuery() {
        when(regionCatalog.findInText(anyString())).thenReturn(Set.of(new RegionCode(null, "41", "경기도", null, "PROVINCE")));
        when(regionCatalog.allSpecificRegionsByLongestName()).thenReturn(java.util.List.of());
    }

    private void shortCityRegionInQuery() {
        when(regionCatalog.findInText(anyString())).thenReturn(Set.of());
        when(regionCatalog.allSpecificRegionsByLongestName())
                .thenReturn(java.util.List.of(new RegionCode(null, "41110", "경기도", "수원시", "CITY")));
    }
}
