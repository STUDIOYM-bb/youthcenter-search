package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

record RegionTextMatchCandidate(
        RegionCode region,
        RegionTextMatchType matchType,
        String matchedText,
        int priority
) {
}
