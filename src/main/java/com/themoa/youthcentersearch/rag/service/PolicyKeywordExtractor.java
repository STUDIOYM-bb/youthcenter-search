package com.themoa.youthcentersearch.rag.service;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PolicyKeywordExtractor {
    private final PolicyKeywordSynonymCatalog synonymCatalog;
    private final PolicyKeywordNormalizer normalizer;

    public PolicyKeywordExtractor(PolicyKeywordSynonymCatalog synonymCatalog, PolicyKeywordNormalizer normalizer) {
        this.synonymCatalog = synonymCatalog;
        this.normalizer = normalizer;
    }

    public KeywordSet extract(String query, Set<String> parserKeywords) {
        Set<String> core = new LinkedHashSet<>();
        Set<String> expanded = new LinkedHashSet<>();
        String normalizedQuery = normalizer.normalize(query);
        addIfPresent(query, normalizedQuery, core, expanded, "청년");
        addIfPresent(query, normalizedQuery, core, expanded, "대학생");
        for (var entry : synonymCatalog.synonyms().entrySet()) {
            boolean matched = entry.getValue().stream()
                    .anyMatch(term -> contains(query, term) || normalizedQuery.contains(normalizer.normalize(term)));
            if ("지원금".equals(entry.getKey()) && core.contains("면접수당")
                    && !contains(query, "지원금") && !contains(query, "보조금") && !contains(query, "장려금")) {
                matched = false;
            }
            if (matched) {
                core.add(entry.getKey());
                expanded.add(entry.getKey());
                expanded.addAll(entry.getValue());
            }
        }
        if (parserKeywords != null) {
            parserKeywords.stream()
                    .filter(StringUtils::hasText)
                    .map(String::trim)
                    .filter(keyword -> contains(query, keyword) || normalizedQuery.contains(normalizer.normalize(keyword)))
                    .forEach(keyword -> {
                        core.add(keyword);
                        expanded.add(keyword);
                    });
        }
        if (core.isEmpty()) {
            core.add("청년");
            expanded.add("청년");
        }
        return new KeywordSet(core, expanded);
    }

    private void addIfPresent(String query, String normalizedQuery, Set<String> core, Set<String> expanded, String keyword) {
        if (contains(query, keyword) || normalizedQuery.contains(normalizer.normalize(keyword))) {
            core.add(keyword);
            expanded.add(keyword);
        }
    }

    private boolean contains(String query, String keyword) {
        return StringUtils.hasText(query) && StringUtils.hasText(keyword) && query.contains(keyword);
    }

    public record KeywordSet(Set<String> coreKeywords, Set<String> expandedKeywords) {
    }
}
