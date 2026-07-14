package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.CandidateSource;

import java.util.Set;

public record PolicyLexicalCandidate(
        Integer policyId,
        double lexicalScore,
        double titleScore,
        Set<CandidateSource> sources
) {
}
