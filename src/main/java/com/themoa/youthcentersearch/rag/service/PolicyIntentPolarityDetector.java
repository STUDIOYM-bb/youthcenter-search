package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class PolicyIntentPolarityDetector {
    private static final Map<SearchDomain, List<String>> DOMAIN_TERMS = new EnumMap<>(SearchDomain.class);
    private static final List<String> EXCLUSION_PHRASES = List.of(
            "생각은 없어", "생각 없어", "생각은 없음", "생각 없음", "관심은 없어", "관심 없어", "관심 없음",
            "생각은 아직 없어", "생각 아직 없어", "관심 없",
            "계획은 없어", "계획 없어", "계획 없음", "원하지 않아", "원하지 않음", "필요 없어", "필요 없음", "필요 없",
            "말고", "제외", "빼줘", "빼고", "싫어", "쪽은 아직 생각 없어", "쪽은 생각 없어"
    );
    private static final List<String> DESIRE_PHRASES = List.of(
            "지원", "찾고", "찾아", "알려", "추천", "필요", "받고 싶", "정책", "혜택", "프로그램"
    );
    private static final Set<String> PERSON_POSITIVE_TERMS = Set.of("대학생", "재학생", "휴학생", "청년", "사회초년생");

    static {
        DOMAIN_TERMS.put(SearchDomain.EMPLOYMENT, List.of("취업", "취준", "구직", "일자리", "채용", "면접", "직업훈련"));
        DOMAIN_TERMS.put(SearchDomain.HOUSING, List.of("주거", "월세", "전세", "임대", "보증금"));
        DOMAIN_TERMS.put(SearchDomain.EDUCATION, List.of("교육", "훈련", "강의", "프로그램", "역량"));
        DOMAIN_TERMS.put(SearchDomain.FINANCE, List.of("금융", "대출", "저축", "계좌", "통장", "자산", "지원금", "수당", "생활비"));
        DOMAIN_TERMS.put(SearchDomain.STARTUP, List.of("창업", "사업화", "벤처"));
        DOMAIN_TERMS.put(SearchDomain.CULTURE, List.of("문화", "예술", "공연"));
        DOMAIN_TERMS.put(SearchDomain.HEALTH, List.of("건강", "의료", "심리", "상담"));
        DOMAIN_TERMS.put(SearchDomain.CARE, List.of("돌봄", "가족", "보육"));
        DOMAIN_TERMS.put(SearchDomain.WELFARE, List.of("복지", "생활안정"));
    }

    public IntentPolarityResult detect(String query) {
        String text = query == null ? "" : query;
        Set<SearchDomain> desiredDomains = new LinkedHashSet<>();
        Set<SearchDomain> excludedDomains = new LinkedHashSet<>();
        Set<String> positiveTerms = new LinkedHashSet<>();
        Set<String> excludedTerms = new LinkedHashSet<>();

        for (String personal : PERSON_POSITIVE_TERMS) {
            if (text.contains(personal)) {
                positiveTerms.add(personal);
            }
        }

        for (String clause : clauses(text)) {
            for (var entry : DOMAIN_TERMS.entrySet()) {
                List<String> matchedTerms = entry.getValue().stream().filter(clause::contains).toList();
                if (matchedTerms.isEmpty()) continue;
                boolean excluded = isExclusionClause(clause, matchedTerms);
                boolean desired = isDesiredClause(clause, matchedTerms);
                if (excluded) {
                    excludedDomains.add(entry.getKey());
                    excludedTerms.addAll(matchedTerms);
                } else if (desired) {
                    desiredDomains.add(entry.getKey());
                    positiveTerms.addAll(matchedTerms);
                }
            }
        }

        desiredDomains.removeAll(excludedDomains);
        positiveTerms.removeAll(excludedTerms);
        return new IntentPolarityResult(desiredDomains, excludedDomains, positiveTerms, excludedTerms);
    }

    public PolicyQuerySemantics merge(String query, PolicyQuerySemantics openAiSemantics) {
        IntentPolarityResult rule = detect(query);
        Set<SearchDomain> desired = new LinkedHashSet<>();
        Set<SearchDomain> excluded = new LinkedHashSet<>();
        Set<String> positive = new LinkedHashSet<>();
        Set<String> negative = new LinkedHashSet<>();
        String normalizedGoal = null;

        if (openAiSemantics != null) {
            desired.addAll(openAiSemantics.desiredDomains());
            excluded.addAll(openAiSemantics.excludedDomains());
            positive.addAll(openAiSemantics.positiveKeywords());
            negative.addAll(openAiSemantics.excludedKeywords());
            normalizedGoal = openAiSemantics.normalizedGoal();
        }

        desired.addAll(rule.desiredDomains());
        positive.addAll(rule.positiveTerms());
        if (!rule.excludedDomains().isEmpty()) {
            excluded.addAll(rule.excludedDomains());
            negative.addAll(rule.excludedTerms());
            desired.removeAll(rule.excludedDomains());
            positive.removeAll(rule.excludedTerms());
        }
        if (positive.isEmpty()) {
            positive.add("청년");
        }
        if (!StringUtils.hasText(normalizedGoal) || containsAny(normalizedGoal, negative)) {
            normalizedGoal = normalizedGoal(positive, desired);
        }
        return new PolicyQuerySemantics(normalizedGoal, desired, excluded, positive, negative, !excluded.isEmpty());
    }

    public static Set<String> termsFor(SearchDomain domain) {
        return new LinkedHashSet<>(DOMAIN_TERMS.getOrDefault(domain, List.of()));
    }

    private boolean isExclusionClause(String clause, List<String> matchedTerms) {
        return matchedTerms.stream().anyMatch(term -> EXCLUSION_PHRASES.stream().anyMatch(phrase -> {
            int termIndex = clause.indexOf(term);
            int phraseIndex = clause.indexOf(phrase);
            return termIndex >= 0 && phraseIndex > termIndex && phraseIndex - termIndex <= 18;
        }));
    }

    private boolean isDesiredClause(String clause, List<String> matchedTerms) {
        boolean supportIntent = matchedTerms.stream().anyMatch(term -> DESIRE_PHRASES.stream()
                .filter(phrase -> !"정책".equals(phrase))
                .anyMatch(phrase -> {
            int termIndex = clause.indexOf(term);
            int phraseIndex = clause.indexOf(phrase, termIndex + term.length());
            return phraseIndex >= 0 && phraseIndex - termIndex <= 18;
        }));
        if (supportIntent) {
            return true;
        }
        return matchedTerms.stream().anyMatch(term -> clause.contains(term + " 준비 중") || clause.contains(term + " 중"));
    }

    private List<String> clauses(String text) {
        if (!StringUtils.hasText(text)) return List.of();
        String[] split = text.split("[\\n.!?]|(?:\\s+단\\s+)|(?:\\s+그리고\\s+)|(?:\\s+하지만\\s+)|(?:\\s+인데\\s+)|(?:\\s+고\\s+)");
        List<String> clauses = new ArrayList<>();
        for (String part : split) {
            if (StringUtils.hasText(part)) {
                clauses.add(part.trim());
            }
        }
        return clauses;
    }

    private boolean containsAny(String text, Set<String> terms) {
        if (!StringUtils.hasText(text)) return false;
        return terms.stream().filter(StringUtils::hasText).anyMatch(text::contains);
    }

    private String normalizedGoal(Set<String> positive, Set<SearchDomain> desired) {
        if (desired.contains(SearchDomain.EMPLOYMENT)) return "청년 취업 준비 및 구직 활동을 지원하는 정책";
        if (desired.contains(SearchDomain.HOUSING)) return "청년 주거 월세 지원 정책";
        if (desired.contains(SearchDomain.FINANCE)) return "청년 생활비 금융 지원 정책";
        if (positive.contains("대학생") || positive.contains("재학생") || positive.contains("휴학생")) {
            return "대학생이 신청 가능한 청년 지원 정책";
        }
        return "청년 지원 정책";
    }

    public record IntentPolarityResult(
            Set<SearchDomain> desiredDomains,
            Set<SearchDomain> excludedDomains,
            Set<String> positiveTerms,
            Set<String> excludedTerms
    ) {
    }
}
