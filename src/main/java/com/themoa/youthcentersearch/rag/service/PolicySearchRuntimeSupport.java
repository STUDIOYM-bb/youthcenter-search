package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.region.RegionMatchEvaluator;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatusResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 검색 오케스트레이터가 직접 보유하지 않아도 되는 런타임 보조 작업을 모은다.
 *
 * <p>Plan 생성 이후 후보 수집 전후로 호출된다. 입력은 SearchPlan, query, 정책 id 목록, 정렬 전 결과이며
 * 출력은 intent, 사용자 지역, audience 분류, 사용자 취업 상태, 지역 보장 선택 결과다.</p>
 *
 * <p>정책 단건 조회 외에는 DB를 직접 조회하지 않고, 외부 API를 호출하지 않는다. 새 hard filter나 점수 계산은
 * 이 클래스가 아니라 PolicyEligibilityEvaluator 또는 PolicyRankingService에 추가해야 한다.</p>
 */
@Component
public class PolicySearchRuntimeSupport {
    private final PolicyRepository policyRepository;
    private final RegionMatchEvaluator regionMatchEvaluator;
    private final PolicySearchIntentBuilder intentBuilder;
    private final PolicyTargetAudienceClassifier targetAudienceClassifier;
    private final PolicyEmploymentAudienceClassifier employmentAudienceClassifier;
    private final UserEmploymentStatusDetector userEmploymentStatusDetector;
    private final RegionCoverageResultSelector regionCoverageResultSelector;

    public PolicySearchRuntimeSupport(PolicyRepository policyRepository,
                                      RegionMatchEvaluator regionMatchEvaluator,
                                      PolicySearchIntentBuilder intentBuilder,
                                      PolicyTargetAudienceClassifier targetAudienceClassifier,
                                      PolicyEmploymentAudienceClassifier employmentAudienceClassifier,
                                      UserEmploymentStatusDetector userEmploymentStatusDetector,
                                      RegionCoverageResultSelector regionCoverageResultSelector) {
        this.policyRepository = policyRepository;
        this.regionMatchEvaluator = regionMatchEvaluator;
        this.intentBuilder = intentBuilder;
        this.targetAudienceClassifier = targetAudienceClassifier;
        this.employmentAudienceClassifier = employmentAudienceClassifier;
        this.userEmploymentStatusDetector = userEmploymentStatusDetector;
        this.regionCoverageResultSelector = regionCoverageResultSelector;
    }

    PolicySearchIntent buildIntent(PolicySearchPlan plan) {
        return intentBuilder.build(plan);
    }

    ResolvedUserRegion resolveUserRegion(PolicySearchCondition condition) {
        return regionMatchEvaluator.resolveUserRegion(condition.province(), condition.city(),
                condition.district(), condition.regionLevel());
    }

    Map<Integer, PolicyTargetAudienceClassification> classifyTargetAudiences(List<Integer> policyIds) {
        return targetAudienceClassifier == null ? Map.of() : targetAudienceClassifier.classify(policyIds);
    }

    Map<Integer, PolicyEmploymentAudience> classifyEmploymentAudiences(List<Integer> policyIds) {
        return employmentAudienceClassifier == null ? Map.of() : employmentAudienceClassifier.classify(policyIds);
    }

    UserEmploymentStatusResult detectEmploymentStatus(String query) {
        return userEmploymentStatusDetector.detect(query);
    }

    RegionCoverageResultSelector.Selection selectRegionCoverage(List<PolicySearchResultItem> results,
                                                                int page,
                                                                int size,
                                                                SearchQueryType queryType) {
        return regionCoverageResultSelector.select(results, page, size, queryType);
    }

    Policy findPolicy(Integer policyId, String sourcePolicyId) {
        return policyId != null
                ? policyRepository.findById(policyId).orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."))
                : policyRepository.findBySourcePolicyId(sourcePolicyId).orElseThrow(() -> new IllegalArgumentException("정책을 찾을 수 없습니다."));
    }

    com.themoa.youthcentersearch.policy.region.RegionMatchResult evaluateRegion(Policy policy, ResolvedUserRegion userRegion) {
        return regionMatchEvaluator.evaluate(policy, userRegion);
    }
}
