package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyLexicalSearchService {
    private final PolicyRepository policyRepository;
    private final PolicyKeywordNormalizer normalizer;
    private final PolicyLexicalIndexBuilder indexBuilder;

    public PolicyLexicalSearchService(PolicyRepository policyRepository, PolicyKeywordNormalizer normalizer,
                                      PolicyLexicalIndexBuilder indexBuilder) {
        this.policyRepository = policyRepository;
        this.normalizer = normalizer;
        this.indexBuilder = indexBuilder;
    }

    public LexicalSearchResult search(PolicySearchCondition condition, int limit) {
        return search(condition, null, limit);
    }

    public LexicalSearchResult search(PolicySearchCondition condition, PolicySearchIntent intent, int limit) {
        List<PolicyLexicalCandidate> candidates = indexBuilder.current().search(condition, intent, limit);
        if (candidates.isEmpty()) {
            return LexicalSearchResult.empty();
        }
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>();
        Map<Integer, Double> titleScores = new LinkedHashMap<>();
        Map<Integer, Set<CandidateSource>> candidateSources = new LinkedHashMap<>();
        Map<CandidateSource, Integer> sourceCounts = new EnumMap<>(CandidateSource.class);
        for (PolicyLexicalCandidate candidate : candidates) {
            lexicalScores.put(candidate.policyId(), candidate.lexicalScore());
            titleScores.put(candidate.policyId(), candidate.titleScore());
            Set<CandidateSource> sources = candidate.sources();
            candidateSources.put(candidate.policyId(), sources);
            for (CandidateSource source : sources) {
                sourceCounts.merge(source, 1, Integer::sum);
            }
        }
        List<Integer> sortedIds = candidates.stream().map(PolicyLexicalCandidate::policyId).toList();
        return new LexicalSearchResult(sortedIds, lexicalScores, titleScores, candidateSources, sourceCounts);
    }

    public double lexicalScore(Policy policy, PolicySearchCondition condition) {
        double score = 0;
        String title = normalized(policy.getTitle());
        String summary = normalized(policy.getSummary());
        String agency = normalized(policy.getAgencyName());
        PolicyCondition pc = policy.getCondition();
        String conditionSummary = pc == null ? "" : normalized(pc.getConditionSummary());
        for (String keyword : condition.expandedKeywords()) {
            String normalized = normalizer.normalize(keyword);
            if (!StringUtils.hasText(normalized)) continue;
            if (title.contains(normalized)) score += 0.55;
            if (summary.contains(normalized)) score += 0.25;
            if (conditionSummary.contains(normalized)) score += 0.15;
            if (agency.contains(normalized)) score += 0.05;
        }
        return Math.min(1.0, score);
    }

    public double lexicalScore(Policy policy, Set<String> terms) {
        double score = 0;
        String title = normalized(policy.getTitle());
        String summary = normalized(policy.getSummary());
        String agency = normalized(policy.getAgencyName());
        String category = policy.getCategory() == null ? "" : normalized(policy.getCategory().name());
        PolicyCondition pc = policy.getCondition();
        String conditionSummary = pc == null ? "" : normalized(pc.getConditionSummary());
        for (String term : terms) {
            String normalized = normalizer.normalize(term);
            if (!StringUtils.hasText(normalized)) continue;
            if (title.contains(normalized)) score += 0.55;
            if (summary.contains(normalized)) score += 0.25;
            if (conditionSummary.contains(normalized)) score += 0.2;
            if (category.contains(normalized)) score += 0.15;
            if (agency.contains(normalized)) score += 0.05;
        }
        return Math.min(1.0, score);
    }

    public double titleExactScore(Policy policy, PolicySearchCondition condition) {
        String title = normalized(policy.getTitle());
        Set<String> keywords = condition.keywords();
        java.util.List<String> meaningful = keywords.stream()
                .map(normalizer::normalize)
                .filter(StringUtils::hasText)
                .filter(term -> !Set.of("청년", "정책", "지원").contains(term))
                .distinct()
                .toList();
        if (!meaningful.isEmpty() && meaningful.stream().allMatch(title::contains)) {
            return meaningful.size() >= 2 ? 0.95 : 0.85;
        }
        double score = 0;
        for (String keyword : keywords) {
            String normalized = normalizer.normalize(keyword);
            if (!StringUtils.hasText(normalized)) continue;
            if (title.equals(normalized)) {
                return 1.0;
            }
            if (title.startsWith(normalized) || normalized.startsWith(title)) {
                score = Math.max(score, 0.9);
                continue;
            }
            if (title.contains(normalized)) {
                score += "청년".equals(keyword) ? 0.05 : 0.6;
            }
        }
        return Math.min(1.0, score);
    }

    public Set<CandidateSource> lexicalSources(Policy policy, List<String> terms) {
        Set<CandidateSource> sources = EnumSet.noneOf(CandidateSource.class);
        String title = normalized(policy.getTitle());
        String summary = normalized(policy.getSummary());
        String agency = normalized(policy.getAgencyName());
        String category = policy.getCategory() == null ? "" : normalized(policy.getCategory().name());
        PolicyCondition pc = policy.getCondition();
        String conditionSummary = pc == null ? "" : normalized(pc.getConditionSummary());
        for (String term : terms) {
            String normalized = normalizer.normalize(term);
            if (!StringUtils.hasText(normalized)) continue;
            if (title.contains(normalized)) sources.add(CandidateSource.MYSQL_TITLE);
            if (summary.contains(normalized)) sources.add(CandidateSource.MYSQL_SUMMARY);
            if (conditionSummary.contains(normalized) || agency.contains(normalized)) sources.add(CandidateSource.MYSQL_KEYWORD);
            if (category.contains(normalized)) sources.add(CandidateSource.MYSQL_CATEGORY);
        }
        return sources;
    }

    private String normalized(String value) {
        return normalizer.normalize(value);
    }

    public record LexicalSearchResult(List<Integer> policyIds,
                                      Map<Integer, Double> lexicalScores,
                                      Map<Integer, Double> titleExactScores,
                                      Map<Integer, Set<CandidateSource>> candidateSources,
                                      Map<CandidateSource, Integer> sourceCounts) {
        public static LexicalSearchResult empty() {
            return new LexicalSearchResult(List.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        public LexicalSearchResult {
            policyIds = policyIds == null ? List.of() : List.copyOf(policyIds);
            lexicalScores = lexicalScores == null ? Map.of() : Map.copyOf(lexicalScores);
            titleExactScores = titleExactScores == null ? Map.of() : Map.copyOf(titleExactScores);
            candidateSources = candidateSources == null ? Map.of() : Map.copyOf(candidateSources);
            sourceCounts = sourceCounts == null ? Map.of() : Map.copyOf(sourceCounts);
        }
    }
}
