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
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class PolicyLexicalIndex {
    private static final Set<String> WEAK_TERMS = Set.of(
            "청년", "정책", "지원", "지원정책", "사업", "프로그램", "운영", "신청", "받을", "있는", "알려줘", "추천", "대상"
    );

    private final List<DocumentEntry> entries;
    private final PolicyKeywordNormalizer normalizer;
    private final Instant builtAt;
    private final Map<String, Integer> documentFrequency;
    private final Map<Field, Double> averageFieldLength;

    public PolicyLexicalIndex(List<PolicySearchProjection> projections, PolicyKeywordNormalizer normalizer) {
        this.normalizer = normalizer;
        this.entries = projections.stream().map(projection -> DocumentEntry.from(projection, normalizer)).toList();
        this.documentFrequency = documentFrequency(entries);
        this.averageFieldLength = averageFieldLength(entries);
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
            for (String token : queryTokens(term)) {
                lexical += bm25(entry, token, sources);
            }
        }
        return new Score(lexical, Math.min(1.0, title), sources);
    }

    private Set<String> terms(PolicySearchCondition condition, PolicySearchIntent intent) {
        Set<String> terms = new LinkedHashSet<>();
        if (intent != null) {
            intent.expandedIntentTerms().forEach(term -> add(terms, term));
            intent.intentTerms().forEach(term -> add(terms, term));
            return terms;
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

    private double bm25(DocumentEntry entry, String token, EnumSet<CandidateSource> sources) {
        if (!StringUtils.hasText(token)) return 0;
        int df = documentFrequency.getOrDefault(token, 0);
        if (df == 0) return 0;
        // 흔한 단어는 df가 커져 idf가 낮아진다. "청년/정책/지원"이 모든 후보를 포화시키던 문제를 막는다.
        double idf = Math.log(1.0 + (entries.size() - df + 0.5) / (df + 0.5));
        double score = 0;
        for (Field field : Field.values()) {
            int tf = entry.termFrequency(field, token);
            if (tf == 0) continue;
            double length = Math.max(1, entry.fieldLength(field));
            double avgLength = Math.max(1.0, averageFieldLength.getOrDefault(field, 1.0));
            double k1 = 1.2;
            double b = 0.75;
            // 필드 길이 정규화를 적용해 긴 설명문에 우연히 등장한 단어가 제목/키워드 일치보다 커지지 않게 한다.
            score += field.weight * idf * (tf * (k1 + 1.0)) / (tf + k1 * (1.0 - b + b * length / avgLength));
            sources.add(field.source);
        }
        return score;
    }

    private List<String> queryTokens(String value) {
        String normalized = normalizer.normalize(value);
        if (!StringUtils.hasText(normalized)) return List.of();
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        addToken(tokens, normalized);
        for (String raw : value.split("[^가-힣a-zA-Z0-9]+")) {
            String token = normalizer.normalize(raw);
            addToken(tokens, token);
        }
        expandConceptTerms(tokens);
        tokens.addAll(hangulNgrams(normalized));
        return tokens.stream().filter(term -> term.length() >= 2).toList();
    }

    private void addToken(Set<String> tokens, String token) {
        if (StringUtils.hasText(token) && token.length() >= 2 && !WEAK_TERMS.contains(token)) {
            tokens.add(token);
            tokens.addAll(hangulNgrams(token));
        }
    }

    private void expandConceptTerms(Set<String> tokens) {
        if (tokens.stream().anyMatch(term -> containsAny(term, "자산형성", "목돈", "적립", "정부기여", "매칭"))) {
            tokens.addAll(Set.of("자산형성", "저축", "계좌", "통장", "적립", "매칭", "목돈", "정부기여"));
        }
        if (tokens.stream().anyMatch(term -> containsAny(term, "저축", "계좌", "통장"))) {
            tokens.addAll(Set.of("자산형성", "저축", "계좌", "통장", "적립", "매칭", "목돈"));
        }
        if (tokens.stream().anyMatch(term -> containsAny(term, "월세", "주거비", "임차료"))) {
            tokens.addAll(Set.of("월세", "주거비", "임차료", "주거"));
        }
        if (tokens.stream().anyMatch(term -> containsAny(term, "취업", "구직", "일자리", "면접", "채용"))) {
            tokens.addAll(Set.of("취업", "구직", "일자리", "면접", "채용", "직업훈련"));
        }
    }

    private static Set<String> hangulNgrams(String token) {
        if (!StringUtils.hasText(token) || !token.matches("[가-힣]+") || token.length() < 4) {
            return Set.of();
        }
        Set<String> ngrams = new LinkedHashSet<>();
        for (int n = 2; n <= 4; n++) {
            for (int i = 0; i <= token.length() - n; i++) {
                String ngram = token.substring(i, i + n);
                if (!WEAK_TERMS.contains(ngram)) {
                    ngrams.add(ngram);
                }
            }
        }
        return ngrams;
    }

    private Map<String, Integer> documentFrequency(List<DocumentEntry> entries) {
        Map<String, Integer> df = new HashMap<>();
        for (DocumentEntry entry : entries) {
            Set<String> seen = new LinkedHashSet<>();
            for (Field field : Field.values()) {
                seen.addAll(entry.terms(field));
            }
            seen.forEach(term -> df.merge(term, 1, Integer::sum));
        }
        return df;
    }

    private Map<Field, Double> averageFieldLength(List<DocumentEntry> entries) {
        Map<Field, Double> average = new java.util.EnumMap<>(Field.class);
        for (Field field : Field.values()) {
            average.put(field, entries.stream().mapToInt(entry -> entry.fieldLength(field)).average().orElse(1.0));
        }
        return average;
    }

    private record Score(double lexicalScore, double titleScore, EnumSet<CandidateSource> sources) {
    }

    private enum Field {
        TITLE(5.0, CandidateSource.MYSQL_TITLE),
        KEYWORD(4.0, CandidateSource.MYSQL_KEYWORD),
        CATEGORY(2.5, CandidateSource.MYSQL_CATEGORY),
        SUPPORT(2.5, CandidateSource.MYSQL_SUMMARY),
        TARGET(2.0, CandidateSource.MYSQL_SUMMARY),
        QUALIFICATION(1.5, CandidateSource.MYSQL_SUMMARY),
        DESCRIPTION(1.0, CandidateSource.MYSQL_SUMMARY),
        INSTITUTION(0.3, CandidateSource.MYSQL_KEYWORD);

        private final double weight;
        private final CandidateSource source;

        Field(double weight, CandidateSource source) {
            this.weight = weight;
            this.source = source;
        }
    }

    private record DocumentEntry(Integer policyId, String normalizedTitle, Map<Field, Map<String, Integer>> terms,
                                 Map<Field, Integer> lengths) {
        static DocumentEntry from(PolicySearchProjection projection, PolicyKeywordNormalizer normalizer) {
            Map<Field, Map<String, Integer>> terms = new java.util.EnumMap<>(Field.class);
            Map<Field, Integer> lengths = new java.util.EnumMap<>(Field.class);
            put(terms, lengths, normalizer, Field.TITLE, projection.getTitleText() + " " + projection.getNormalizedTitle());
            put(terms, lengths, normalizer, Field.KEYWORD, projection.getKeywordText());
            put(terms, lengths, normalizer, Field.CATEGORY, projection.getCategoryText());
            put(terms, lengths, normalizer, Field.SUPPORT, projection.getSupportText());
            put(terms, lengths, normalizer, Field.TARGET, projection.getTargetText());
            put(terms, lengths, normalizer, Field.QUALIFICATION, projection.getQualificationText());
            put(terms, lengths, normalizer, Field.DESCRIPTION, projection.getDescriptionText());
            put(terms, lengths, normalizer, Field.INSTITUTION, projection.getInstitutionText());
            return new DocumentEntry(projection.getPolicyId(),
                    safe(projection.getNormalizedTitle()),
                    terms, lengths);
        }

        private static String safe(String value) {
            return value == null ? "" : value;
        }

        private static void put(Map<Field, Map<String, Integer>> target, Map<Field, Integer> lengths,
                                PolicyKeywordNormalizer normalizer, Field field, String value) {
            Map<String, Integer> counts = new HashMap<>();
            int length = 0;
            String rawValue = value == null ? "" : value;
            String normalized = normalizer.normalize(rawValue);
                if (StringUtils.hasText(rawValue)) {
                for (String raw : rawValue.split("[^가-힣a-zA-Z0-9]+")) {
                    String token = normalizer.normalize(raw);
                    if (StringUtils.hasText(token) && token.length() >= 2) {
                        counts.merge(token, 1, Integer::sum);
                        for (String ngram : hangulNgrams(token)) {
                            counts.merge(ngram, 1, Integer::sum);
                        }
                        length++;
                    }
                }
                if (StringUtils.hasText(normalized) && normalized.length() >= 2) {
                    counts.merge(normalized, 1, Integer::sum);
                    for (String ngram : hangulNgrams(normalized)) {
                        counts.merge(ngram, 1, Integer::sum);
                    }
                    length++;
                }
            }
            target.put(field, counts);
            lengths.put(field, Math.max(1, length));
        }

        int termFrequency(Field field, String term) {
            return terms.getOrDefault(field, Map.of()).getOrDefault(term, 0);
        }

        int fieldLength(Field field) {
            return lengths.getOrDefault(field, 1);
        }

        Set<String> terms(Field field) {
            return terms.getOrDefault(field, Map.of()).keySet();
        }
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) return true;
        }
        return false;
    }
}
