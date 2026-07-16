package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import org.springframework.stereotype.Component;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 내부 검색 평가 결과를 기존 API 결과 DTO로 변환한다.
 *
 * <p>PolicyRankingService 이후, PolicySearchResponse 조립 전에 호출된다. 입력은 정책 엔티티,
 * 후보 검색 증거, Eligibility 평가 결과, Ranking 평가 결과이며 출력은 프론트가 소비하는
 * PolicySearchResultItem이다.</p>
 *
 * <p>Eligibility와 Ranking을 분리해 받는 이유는 자격 불일치 제거와 관련도 점수 계산의 책임이 다르기 때문이다.
 * 이 클래스는 DB 또는 외부 시스템을 호출하지 않고, 이미 계산된 내부 데이터를 JSON 호환 DTO로만 옮긴다.</p>
 *
 * <p>PolicySearchResultItem의 필드명과 null 처리 방식은 기존 REST API 호환성에 직접 영향을 준다.
 * 필드를 추가하거나 의미를 바꿀 때는 프론트 사용처와 Explain 응답을 함께 확인해야 한다.</p>
 */
@Component
public class PolicySearchResultAssembler {
    private final PolicyDomainClassifier domainClassifier;

    public PolicySearchResultAssembler(PolicyDomainClassifier domainClassifier) {
        this.domainClassifier = domainClassifier;
    }

    public PolicySearchResultItem assemble(RankedPolicyCandidate ranked,
                                           Map<Integer, PolicyTargetAudienceClassification> targetAudienceByPolicyId,
                                           Map<Integer, PolicyEmploymentAudience> employmentAudienceByPolicyId) {
        Policy policy = ranked.candidate().policy();
        PolicyEligibilityEvaluation eligibility = ranked.candidate().eligibility();
        PolicyRankingEvaluation ranking = ranked.ranking();
        CandidateEvidence evidence = ranked.candidate().candidateEvidence();
        PolicyTargetAudienceClassification targetAudience = targetAudienceByPolicyId
                .getOrDefault(policy.getId(), PolicyTargetAudienceClassification.unknown());
        PolicyEmploymentAudience employmentAudience = employmentAudienceByPolicyId
                .getOrDefault(policy.getId(), PolicyEmploymentAudience.unknown());
        PolicyDomainClassification domain = domainClassifier.classify(policy);
        PolicyCondition pc = policy.getCondition();

        return new PolicySearchResultDraft(
                policy.getId(),
                policy.getSourcePolicyId(),
                policy.getTitle(),
                policy.getCategory().name(),
                regionText(policy),
                eligibility.regionMatch().compatibility().name(),
                eligibility.regionMatch().label(),
                regionEvidence(policy),
                policy.getAgencyName(),
                policy.getSummary(),
                pc == null ? null : pc.getMinAge(),
                pc == null ? null : pc.getMaxAge(),
                pc == null ? null : pc.getEmploymentStatus(),
                policy.getStartDate(),
                policy.getDueDate(),
                policy.getStatus(),
                policy.getOfficialUrl(),
                ranking.semanticScore(),
                ranking.finalScore(),
                mergeReasons(eligibility.matchedReasons(), ranking.rankingReasons()),
                eligibility.confirmationReasons(),
                eligibility.regionMatch().compatibility().name(),
                eligibility.regionMatch().label(),
                eligibility.regionMatch().reason(),
                eligibility.regionMatch().score(),
                candidateSources(evidence, ranking),
                eligibility.ageMatch().status().name(),
                eligibility.ageMatch().reason(),
                eligibility.employmentMatch().status().name(),
                eligibility.employmentMatch().reason(),
                eligibility.studentMatch().status().name(),
                eligibility.studentMatch().reason(),
                ranking.topicScore(),
                domain.primaryDomain().name(),
                domain.secondaryDomains().stream().map(Enum::name).sorted().toList(),
                domain.supportIntents().stream().map(Enum::name).sorted().toList(),
                domain.evidence(),
                true,
                targetAudience.includedStages().stream().map(Enum::name).sorted().toList(),
                targetAudience.excludedStages().stream().map(Enum::name).sorted().toList(),
                targetAudience.evidence(),
                eligibility.educationStageMatch().status().name(),
                eligibility.educationStageMatch().reason(),
                employmentAudience.allowedStatuses().stream().map(Enum::name).sorted().toList(),
                employmentAudience.exclusive(),
                employmentAudience.evidence(),
                ranking.recommendationTier().name(),
                eligibility.preliminaryTier() == ranking.recommendationTier()
                        ? recommendationReason(ranking.recommendationTier().name())
                        : "랭킹 단계에서 추천 등급이 조정되었습니다."
        ).toResultItem();
    }

    private List<String> mergeReasons(List<String> eligibility, List<String> ranking) {
        return java.util.stream.Stream.concat(eligibility.stream(), ranking.stream())
                .filter(reason -> reason != null && !reason.isBlank())
                .distinct()
                .toList();
    }

    private List<String> candidateSources(CandidateEvidence evidence, PolicyRankingEvaluation ranking) {
        // Source Rank와 Final Rank는 서로 다른 값이므로 DTO에는 출처명만 싣고, 순위는 Explain에서 evidence/finalRank로 분리한다.
        Set<CandidateSource> sources = evidence.sourceEvidence().stream()
                .map(CandidateSourceEvidence::source)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(CandidateSource.class)));
        if (ranking.titleScore() >= 1.0) sources.add(CandidateSource.EXACT_TITLE);
        else if (ranking.titleScore() >= 0.75) sources.add(CandidateSource.TITLE_PHRASE);
        return sources.stream().map(Enum::name).sorted().toList();
    }

    private String recommendationReason(String tier) {
        if ("NEEDS_CONFIRMATION".equals(tier)) {
            return "명시되지 않은 자격 조건 확인이 필요합니다.";
        }
        if ("MISMATCH".equals(tier)) {
            return "명시 조건과 정책 대상이 불일치합니다.";
        }
        return "명시 조건과 충돌하지 않는 기본 추천 후보입니다.";
    }

    private String regionText(Policy policy) {
        return policy.getRegions().stream()
                .map(region -> region.getRegion().displayName())
                .distinct()
                .reduce((a, b) -> a + ", " + b)
                .orElse("확인 필요");
    }

    private List<String> regionEvidence(Policy policy) {
        return policy.getRegions().stream()
                .map(PolicyRegion::getRegion)
                .map(region -> region.getRegionLevel() + ": " + region.displayName())
                .distinct()
                .toList();
    }
}
