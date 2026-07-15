package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import com.themoa.youthcentersearch.rag.dto.UserEducationStageCondition;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyTargetEligibilityFilterTest {
    private final PolicyTargetEligibilityFilter filter = new PolicyTargetEligibilityFilter();
    private final UserEducationStageCondition university = new UserEducationStageCondition(Set.of(EducationStage.UNIVERSITY), true, List.of("대학생"));

    @Test
    void universityUserMismatchesHighSchoolOnlyPolicy() {
        var result = filter.match(university, target(Set.of(EducationStage.HIGH_SCHOOL), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MISMATCH);
    }

    @Test
    void universityUserMatchesUniversityPolicy() {
        var result = filter.match(university, target(Set.of(EducationStage.UNIVERSITY), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void universityUserMatchesGeneralYouthPolicy() {
        var result = filter.match(university, target(Set.of(EducationStage.GENERAL_YOUTH), false));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void universityUserMatchesMixedHighSchoolUniversityPolicy() {
        var result = filter.match(university, target(Set.of(EducationStage.HIGH_SCHOOL, EducationStage.UNIVERSITY), true));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.MATCH);
    }

    @Test
    void unknownPolicyTargetIsKeptAsUnknown() {
        var result = filter.match(university, target(Set.of(EducationStage.UNKNOWN), false));

        assertThat(result.status()).isEqualTo(ConditionMatchStatus.UNKNOWN);
    }

    private PolicyTargetAudienceClassification target(Set<EducationStage> stages, boolean exclusive) {
        return new PolicyTargetAudienceClassification(stages, Set.of(), exclusive, 0.9, List.of("test"));
    }
}
