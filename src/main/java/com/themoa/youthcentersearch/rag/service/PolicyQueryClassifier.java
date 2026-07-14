package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Set;

@Component
public class PolicyQueryClassifier {
    private static final Set<String> BROAD_WORDS = Set.of("청년", "정책", "지원", "받을수있는", "알려줘", "추천");
    private static final Set<String> TOPIC_HINTS = Set.of(
            "취업", "구직", "면접", "월세", "주거", "교통", "계좌", "저축", "자산", "창업",
            "교육", "문화", "복지", "금융", "대출", "수당", "생활비", "일자리", "훈련", "상담",
            "취준생", "취업준비생", "미취업", "무직"
    );
    private final PolicyKeywordNormalizer normalizer;

    public PolicyQueryClassifier(PolicyKeywordNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public SearchQueryType classify(String query, PolicySearchCondition condition) {
        String normalized = normalizer.normalize(query);
        if (!StringUtils.hasText(normalized)) {
            return SearchQueryType.BROAD_DISCOVERY;
        }
        boolean hasCondition = condition.regionExplicit() || condition.ageExplicit()
                || condition.employmentExplicit() || condition.studentExplicit();
        long meaningful = condition.expandedKeywords().stream()
                .map(normalizer::normalize)
                .filter(StringUtils::hasText)
                .filter(term -> !BROAD_WORDS.contains(term))
                .count();
        if (looksLikePolicyName(query, normalized, condition, hasCondition)) {
            return SearchQueryType.POLICY_NAME;
        }
        if (hasCondition && !hasTopicHint(normalized)) {
            return SearchQueryType.BROAD_DISCOVERY;
        }
        if (hasCondition && meaningful > 0) {
            return SearchQueryType.ELIGIBILITY_SEARCH;
        }
        if (hasCondition) {
            return SearchQueryType.BROAD_DISCOVERY;
        }
        return meaningful <= 1 ? SearchQueryType.BROAD_DISCOVERY : SearchQueryType.TOPIC_SEARCH;
    }

    private boolean hasTopicHint(String normalizedQuery) {
        return TOPIC_HINTS.stream().map(normalizer::normalize).anyMatch(normalizedQuery::contains);
    }

    private boolean looksLikePolicyName(String query, String normalized, PolicySearchCondition condition, boolean hasCondition) {
        if (hasCondition && hasTopicHint(normalized)) {
            return false;
        }
        if (BROAD_WORDS.contains(normalized)) {
            return false;
        }
        if (query.length() <= 12 && !query.contains("사는") && !query.contains("받을") && !query.contains("정책")) {
            return true;
        }
        return condition.keywords().stream()
                .map(normalizer::normalize)
                .filter(StringUtils::hasText)
                .anyMatch(term -> term.length() >= 4 && normalized.equals(term));
    }
}
