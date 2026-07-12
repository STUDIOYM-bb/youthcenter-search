package com.themoa.youthcentersearch.policy.service;

public record PolicyUpsertResult(
        int policyId,
        boolean inserted
) {
}
