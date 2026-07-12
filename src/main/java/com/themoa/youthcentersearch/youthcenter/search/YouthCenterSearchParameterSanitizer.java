package com.themoa.youthcentersearch.youthcenter.search;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class YouthCenterSearchParameterSanitizer {
    private static final Set<String> STOP_WORDS = Set.of("사는", "살", "세", "받을", "있는", "알려줘", "추천", "정책", "지원");
    private static final Set<String> DESCRIPTION_TERMS = Set.of("면접비", "면접", "월세", "생활비", "교육비", "취업", "창업", "주거", "지원금", "수당");

    public List<String> sanitizeKeywords(List<String> rawKeywords, int maxKeywords) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (rawKeywords == null) {
            return List.of();
        }
        for (String keyword : rawKeywords) {
            for (String token : tokenize(keyword)) {
                if (cleaned.size() >= maxKeywords) {
                    return new ArrayList<>(cleaned);
                }
                cleaned.add(token);
            }
        }
        return new ArrayList<>(cleaned);
    }

    public List<String> sanitizeKeywordPool(List<String> rawKeywords) {
        Set<String> cleaned = new LinkedHashSet<>();
        if (rawKeywords != null) {
            rawKeywords.forEach(keyword -> cleaned.addAll(tokenize(keyword)));
        }
        return new ArrayList<>(cleaned);
    }

    public String selectShortDescriptionTerm(List<String> rawTerms, int maxLength) {
        for (String term : sanitizeKeywordPool(rawTerms)) {
            if (term.length() <= maxLength && DESCRIPTION_TERMS.contains(term)) {
                return term;
            }
        }
        return null;
    }

    private List<String> tokenize(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        String normalized = value.trim().replaceAll("[\\p{Punct}·ㆍ]", " ").replaceAll("\\s+", " ");
        List<String> tokens = new ArrayList<>();
        for (String token : normalized.split(" ")) {
            String cleaned = token.trim();
            if (StringUtils.hasText(cleaned) && cleaned.length() <= 12 && !STOP_WORDS.contains(cleaned)) {
                tokens.add(cleaned);
            }
        }
        return tokens;
    }
}
