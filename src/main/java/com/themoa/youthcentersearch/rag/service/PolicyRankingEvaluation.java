package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.RecommendationTier;

import java.util.List;

public record PolicyRankingEvaluation(
        double topicScore,
        double semanticScore,
        double lexicalScore,
        double titleScore,
        double domainScore,
        double supportIntentScore,
        double regionScore,
        double finalScore,
        RecommendationTier recommendationTier,
        int finalRank,
        List<String> rankingReasons
) {
    public PolicyRankingEvaluation {
        rankingReasons = rankingReasons == null ? List.of() : List.copyOf(rankingReasons);
    }
}
