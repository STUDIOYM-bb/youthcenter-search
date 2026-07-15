package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import com.themoa.youthcentersearch.rag.dto.TargetStageMatchResult;
import com.themoa.youthcentersearch.rag.dto.UserEducationStageCondition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

/**
 * 사용자 교육 단계와 정책 대상 단계의 명확한 불일치를 Hard Filter로 판정한다.
 * UNKNOWN은 과잉 제거를 피하기 위해 유지하고 확인 필요로만 표시한다.
 */
@Component
public class PolicyTargetEligibilityFilter {
    public TargetStageMatchResult match(PolicySearchPlan plan, PolicyTargetAudienceClassification target) {
        if (plan == null || !plan.educationStageExplicit()) {
            return TargetStageMatchResult.unknown("사용자가 교육 단계를 명시하지 않았습니다.");
        }
        return match(new UserEducationStageCondition(plan.userEducationStages(), true, List.of()), target);
    }

    public TargetStageMatchResult match(UserEducationStageCondition user, PolicyTargetAudienceClassification target) {
        if (user == null || !user.explicit() || user.stages().contains(EducationStage.UNKNOWN)) {
            return TargetStageMatchResult.unknown("사용자 교육 단계가 명확하지 않습니다.");
        }
        if (target == null || target.includedStages().contains(EducationStage.UNKNOWN)) {
            return TargetStageMatchResult.unknown("정책 대상 단계 확인 필요");
        }
        if (target.excludedStages().stream().anyMatch(user.stages()::contains)) {
            return TargetStageMatchResult.mismatch("정책이 사용자 교육 단계를 명시적으로 제외합니다.");
        }
        Set<EducationStage> included = target.includedStages();
        if (included.contains(EducationStage.GENERAL_YOUTH) || included.contains(EducationStage.ALL_STUDENTS)) {
            return TargetStageMatchResult.match("일반 청년 또는 전체 학생 대상 정책입니다.");
        }
        if (included.stream().anyMatch(user.stages()::contains)) {
            return TargetStageMatchResult.match("사용자 교육 단계가 정책 대상에 포함됩니다.");
        }
        if (target.stageExclusive()) {
            return TargetStageMatchResult.mismatch("정책 대상 단계가 사용자 교육 단계와 다릅니다.");
        }
        return TargetStageMatchResult.unknown("정책 대상 단계가 배타적인지 확인이 필요합니다.");
    }
}
