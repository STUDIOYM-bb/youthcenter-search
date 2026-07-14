package com.themoa.youthcentersearch.rag.dto;

public record TopicRelevanceScore(
        double semanticScore,
        double lexicalScore,
        double titleScore,
        double categoryScore,
        double intentScore,
        double finalTopicScore
) {
    public boolean passes(double threshold) {
        return finalTopicScore >= threshold
                || titleScore >= 0.75
                || lexicalScore >= 0.65
                || semanticScore >= 0.75
                || categoryScore >= 0.9;
    }
}
