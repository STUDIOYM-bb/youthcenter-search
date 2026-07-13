package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class PolicyLexicalSearchService {
    private final PolicyRepository policyRepository;
    private final PolicyKeywordNormalizer normalizer;

    @PersistenceContext
    private EntityManager entityManager;

    public PolicyLexicalSearchService(PolicyRepository policyRepository, PolicyKeywordNormalizer normalizer) {
        this.policyRepository = policyRepository;
        this.normalizer = normalizer;
    }

    public LexicalSearchResult search(PolicySearchCondition condition, int limit) {
        List<String> terms = condition.expandedKeywords().stream()
                .filter(StringUtils::hasText)
                .map(String::trim)
                .distinct()
                .limit(20)
                .toList();
        if (terms.isEmpty()) {
            return new LexicalSearchResult(List.of(), Map.of(), Map.of());
        }
        StringBuilder jpql = new StringBuilder("""
                select distinct p.id
                from Policy p
                left join p.condition c
                where p.active = true and (
                """);
        for (int i = 0; i < terms.size(); i++) {
            if (i > 0) jpql.append(" or ");
            jpql.append("lower(p.title) like :term").append(i)
                    .append(" or lower(p.summary) like :term").append(i)
                    .append(" or lower(p.agencyName) like :term").append(i)
                    .append(" or lower(c.conditionSummary) like :term").append(i);
        }
        jpql.append(")");
        int fetchLimit = Math.max(limit * 10, 1000);
        var query = entityManager.createQuery(jpql.toString(), Integer.class).setMaxResults(fetchLimit);
        for (int i = 0; i < terms.size(); i++) {
            query.setParameter("term" + i, "%" + terms.get(i).toLowerCase() + "%");
        }
        List<Integer> ids = query.getResultList();
        if (ids.isEmpty()) {
            return new LexicalSearchResult(List.of(), Map.of(), Map.of());
        }
        List<Policy> policies = policyRepository.findWithRelationsByIdIn(ids);
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>();
        Map<Integer, Double> titleScores = new LinkedHashMap<>();
        for (Policy policy : policies) {
            lexicalScores.put(policy.getId(), lexicalScore(policy, condition));
            titleScores.put(policy.getId(), titleExactScore(policy, condition));
        }
        policies.sort((a, b) -> Double.compare(
                lexicalScores.getOrDefault(b.getId(), 0.0) + titleScores.getOrDefault(b.getId(), 0.0),
                lexicalScores.getOrDefault(a.getId(), 0.0) + titleScores.getOrDefault(a.getId(), 0.0)));
        List<Integer> sortedIds = policies.stream().map(Policy::getId).limit(limit).toList();
        return new LexicalSearchResult(sortedIds, lexicalScores, titleScores);
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

    public double titleExactScore(Policy policy, PolicySearchCondition condition) {
        String title = normalized(policy.getTitle());
        Set<String> keywords = condition.keywords();
        if (keywords.contains("면접수당")) {
            if (title.contains("청년면접수당")) return 1.0;
            if (title.contains("면접수당")) return 0.9;
            if (title.contains("면접") && title.contains("수당")) return 0.8;
        }
        double score = 0;
        for (String keyword : keywords) {
            String normalized = normalizer.normalize(keyword);
            if (!StringUtils.hasText(normalized)) continue;
            if (title.contains(normalized)) {
                score += "청년".equals(keyword) ? 0.05 : 0.6;
            }
        }
        return Math.min(1.0, score);
    }

    private String normalized(String value) {
        return normalizer.normalize(value);
    }

    public record LexicalSearchResult(List<Integer> policyIds,
                                      Map<Integer, Double> lexicalScores,
                                      Map<Integer, Double> titleExactScores) {
        public LexicalSearchResult {
            policyIds = policyIds == null ? List.of() : List.copyOf(policyIds);
            lexicalScores = lexicalScores == null ? Map.of() : Map.copyOf(lexicalScores);
            titleExactScores = titleExactScores == null ? Map.of() : Map.copyOf(titleExactScores);
        }
    }
}
