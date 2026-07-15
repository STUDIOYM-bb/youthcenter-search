package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.RecommendationTier;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RecommendationTierTest {
    @Test
    void enumOrderKeepsPrimaryBeforeNeedsConfirmationAndMismatch() {
        assertThat(RecommendationTier.PRIMARY.ordinal()).isLessThan(RecommendationTier.NEEDS_CONFIRMATION.ordinal());
        assertThat(RecommendationTier.NEEDS_CONFIRMATION.ordinal()).isLessThan(RecommendationTier.MISMATCH.ordinal());
    }

    @Test
    void employedAndUnemployedAudiencesRemainDistinct() {
        PolicyEmploymentAudience employedOnly = new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.EMPLOYED), true, 0.9, List.of("재직자"));
        PolicyEmploymentAudience unemployedOnly = new PolicyEmploymentAudience(Set.of(UserEmploymentStatus.UNEMPLOYED), true, 0.9, List.of("미취업"));

        assertThat(employedOnly.allowedStatuses()).containsExactly(UserEmploymentStatus.EMPLOYED);
        assertThat(unemployedOnly.allowedStatuses()).containsExactly(UserEmploymentStatus.UNEMPLOYED);
    }
}
