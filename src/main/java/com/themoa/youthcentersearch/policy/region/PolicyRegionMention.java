package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

public record PolicyRegionMention(
        RegionCode region,
        PolicyRegionMentionRole role,
        String rawText,
        int confidence,
        String reason
) {
}
