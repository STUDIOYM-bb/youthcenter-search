package com.themoa.youthcentersearch.rag.dto;

import java.util.Set;

public record PolicySearchIntent(
        String originalQuery,
        Set<String> conditionTerms,
        Set<String> intentTerms,
        Set<String> expandedIntentTerms,
        String semanticQuery,
        String lexicalQuery
) {
    public PolicySearchIntent {
        conditionTerms = conditionTerms == null ? Set.of() : Set.copyOf(conditionTerms);
        intentTerms = intentTerms == null ? Set.of() : Set.copyOf(intentTerms);
        expandedIntentTerms = expandedIntentTerms == null ? Set.of() : Set.copyOf(expandedIntentTerms);
    }
}
