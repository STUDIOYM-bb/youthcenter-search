package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class PolicyLexicalIndex {
    private static final Set<String> WEAK_TERMS = Set.of(
            "청년", "정책", "지원", "지원정책", "받을", "있는", "알려줘", "추천", "대상"
    );

    private final List<DocumentEntry> entries;
    private final PolicyKeywordNormalizer normalizer;
    private final Instant builtAt;

    public PolicyLexicalIndex(List<PolicySearchProjection> projections, PolicyKeywordNormalizer normalizer) {
        this.normalizer = normalizer;
        this.entries = projections.stream().map(projection -> DocumentEntry.from(projection, normalizer)).toList();
        this.builtAt = Instant.now();
    }

    public List<PolicyLexicalCandidate> search(PolicySearchCondition condition, PolicySearchIntent intent, int limit) {
        Set<String> terms = terms(condition, intent);
        if (terms.isEmpty()) {
            return List.of();
        }
        String rawQuery = normalizer.normalize(intent == null ? "" : intent.originalQuery());
        List<PolicyLexicalCandidate> candidates = new ArrayList<>();
        for (DocumentEntry entry : entries) {
            Score score = score(entry, terms, rawQuery);
            if (score.lexicalScore > 0 || score.titleScore > 0) {
                candidates.add(new PolicyLexicalCandidate(entry.policyId, score.lexicalScore, score.titleScore, score.sources));
            }
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble((PolicyLexicalCandidate c) -> c.lexicalScore() + c.titleScore()).reversed())
                .limit(limit)
                .toList();
    }

    public int size() {
        return entries.size();
    }

    public Instant builtAt() {
        return builtAt;
    }

    private Score score(DocumentEntry entry, Set<String> terms, String rawQuery) {
        double lexical = 0;
        double title = 0;
        EnumSet<CandidateSource> sources = EnumSet.noneOf(CandidateSource.class);
        if (StringUtils.hasText(rawQuery)) {
            if (entry.normalizedTitle.equals(rawQuery)) {
                title = 1.0;
                sources.add(CandidateSource.MYSQL_TITLE);
            } else if (entry.normalizedTitle.startsWith(rawQuery) || entry.normalizedTitle.contains(rawQuery)) {
                title = Math.max(title, 0.85);
                sources.add(CandidateSource.MYSQL_TITLE);
            }
        }
        for (String term : terms) {
            if (!StringUtils.hasText(term)) continue;
            boolean weak = WEAK_TERMS.contains(term);
            if (entry.normalizedTitle.contains(term)) {
                lexical += weak ? 0.05 : 0.75;
                title = Math.max(title, weak ? 0.05 : 0.75);
                sources.add(CandidateSource.MYSQL_TITLE);
            }
            if (entry.keywordText.contains(term)) {
                lexical += weak ? 0.03 : 0.55;
                sources.add(CandidateSource.MYSQL_KEYWORD);
            }
            if (entry.categoryText.contains(term)) {
                lexical += weak ? 0.02 : 0.35;
                sources.add(CandidateSource.MYSQL_CATEGORY);
            }
            if (entry.supportText.contains(term) || entry.targetText.contains(term) || entry.qualificationText.contains(term)) {
                lexical += weak ? 0.02 : 0.30;
                sources.add(CandidateSource.MYSQL_SUMMARY);
            }
            if (entry.descriptionText.contains(term)) {
                lexical += weak ? 0.01 : 0.20;
                sources.add(CandidateSource.MYSQL_SUMMARY);
            }
            if (entry.institutionText.contains(term)) {
                lexical += weak ? 0.0 : 0.05;
                sources.add(CandidateSource.MYSQL_KEYWORD);
            }
        }
        return new Score(Math.min(1.0, lexical), Math.min(1.0, title), sources);
    }

    private Set<String> terms(PolicySearchCondition condition, PolicySearchIntent intent) {
        Set<String> terms = new LinkedHashSet<>();
        if (intent != null) {
            intent.expandedIntentTerms().forEach(term -> add(terms, term));
            intent.intentTerms().forEach(term -> add(terms, term));
        }
        condition.expandedKeywords().forEach(term -> add(terms, term));
        condition.keywords().forEach(term -> add(terms, term));
        return terms;
    }

    private void add(Set<String> terms, String term) {
        String normalized = normalizer.normalize(term);
        if (StringUtils.hasText(normalized) && normalized.length() >= 2) {
            terms.add(normalized);
        }
    }

    private record Score(double lexicalScore, double titleScore, EnumSet<CandidateSource> sources) {
    }

    private record DocumentEntry(Integer policyId, String normalizedTitle, String keywordText, String categoryText,
                                 String descriptionText, String supportText, String targetText,
                                 String qualificationText, String institutionText) {
        static DocumentEntry from(PolicySearchProjection projection, PolicyKeywordNormalizer normalizer) {
            return new DocumentEntry(projection.getPolicyId(),
                    safe(projection.getNormalizedTitle()),
                    normalize(normalizer, projection.getKeywordText()),
                    normalize(normalizer, projection.getCategoryText()),
                    normalize(normalizer, projection.getDescriptionText()),
                    normalize(normalizer, projection.getSupportText()),
                    normalize(normalizer, projection.getTargetText()),
                    normalize(normalizer, projection.getQualificationText()),
                    normalize(normalizer, projection.getInstitutionText()));
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }

        private static String normalize(PolicyKeywordNormalizer normalizer, String value) {
            return value == null ? "" : normalizer.normalize(value);
        }
    }
}
