package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PolicySearchIntentBuilder {
    public PolicySearchIntent build(String query, PolicySearchCondition condition) {
        String original = query == null ? "" : query.trim();
        Set<String> conditionTerms = new LinkedHashSet<>();
        Set<String> intentTerms = new LinkedHashSet<>();
        Set<String> expanded = new LinkedHashSet<>();

        if (StringUtils.hasText(condition.rawRegionText())) {
            conditionTerms.add(condition.rawRegionText());
        } else {
            if (StringUtils.hasText(condition.province())) conditionTerms.add(condition.province());
            if (StringUtils.hasText(condition.city())) conditionTerms.add(condition.city());
        }
        if (condition.ageExplicit() && condition.age() != null) {
            conditionTerms.add(condition.age() + "살");
        }
        if (condition.employmentExplicit() && StringUtils.hasText(condition.employmentStatus())) {
            conditionTerms.addAll(employmentConditionTerms(original));
        }

        intentTerms.add("청년");
        if (financialIntent(original, condition)) {
            intentTerms.add("금융 지원");
            intentTerms.add(financialAssetIntent(original) ? "자산형성 저축 계좌 지원" : "생활비 지원");
            expanded.addAll(Set.of("금융", "생활비", "지원금", "수당", "보조금", "장려금", "자립", "자산형성",
                    "사회초년생", "사회 초년생", "청년 금융", "청년 지원금"));
            if (financialAssetIntent(original)) {
                expanded.addAll(Set.of("계좌", "통장", "저축", "자산", "자산형성", "청년도약계좌", "청년 통장"));
            }
        }
        if (employmentIntent(original, condition)) {
            intentTerms.add("취업 지원");
            intentTerms.add("구직 지원");
            intentTerms.add("취업 준비");
            expanded.addAll(Set.of("취업", "구직", "일자리", "채용", "취업준비", "구직활동", "취업역량", "직업훈련",
                    "취업 지원", "구직 지원", "취업 준비", "미취업 청년", "구직자"));
        }
        if (interviewIntent(original)) {
            intentTerms.add("면접 지원");
            expanded.addAll(Set.of("면접", "면접수당", "면접 수당", "면접비", "면접 비용", "면접 지원금", "면접 정장", "면접 교통비"));
        } else if (employmentIntent(original, condition)) {
            expanded.addAll(Set.of("면접", "면접수당", "면접비"));
        }
        if (StringUtils.hasText(condition.category())) {
            intentTerms.add(condition.category());
            expanded.add(condition.category());
        }
        expanded.addAll(condition.keywords());
        expanded.addAll(condition.expandedKeywords());
        expanded.removeIf(term -> !StringUtils.hasText(term));

        String semanticQuery = semanticQuery(intentTerms, expanded, condition);
        String lexicalQuery = String.join(" ", expanded);
        return new PolicySearchIntent(original, conditionTerms, intentTerms, expanded, semanticQuery, lexicalQuery);
    }

    private Set<String> employmentConditionTerms(String query) {
        Set<String> terms = new LinkedHashSet<>();
        for (String term : Set.of("취준생", "취업준비생", "무직", "미취업", "구직자", "취업 준비")) {
            if (query.contains(term)) {
                terms.add(term);
            }
        }
        return terms;
    }

    private boolean employmentIntent(String query, PolicySearchCondition condition) {
        return "UNEMPLOYED".equals(condition.employmentStatus())
                || containsAny(query, "취업", "취준", "구직", "일자리", "채용", "면접")
                || "일자리".equals(condition.category()) || "취업 지원".equals(condition.category());
    }

    private boolean interviewIntent(String query) {
        return containsAny(query, "면접", "면접수당", "면접비", "면접 비용");
    }

    private boolean financialIntent(String query, PolicySearchCondition condition) {
        return containsAny(query, "금융", "지원금", "수당", "보조금", "장려금", "생활비", "사회 초년생", "사회초년생",
                "계좌", "통장", "저축", "자산")
                || "financialSupport".equalsIgnoreCase(condition.category())
                || "금융".equals(condition.category())
                || condition.supportTypes().stream().anyMatch(type -> Set.of("CASH", "ALLOWANCE", "SUBSIDY").contains(type));
    }

    private String semanticQuery(Set<String> intentTerms, Set<String> expanded, PolicySearchCondition condition) {
        if (expanded.stream().anyMatch(term -> term.contains("계좌") || term.contains("통장") || term.contains("저축")
                || "자산".equals(term))) {
            return "청년 자산형성 저축 계좌 통장 금융 지원 정책";
        }
        if (expanded.stream().anyMatch(term -> term.contains("금융") || term.contains("생활비") || term.contains("사회초년생"))) {
            return "청년 사회초년생 생활비 금융 지원 정책";
        }
        if (expanded.stream().anyMatch(term -> term.contains("면접"))) {
            return "청년 취업 준비와 면접 비용 또는 면접수당을 지원하는 정책";
        }
        if (expanded.stream().anyMatch(term -> term.contains("취업") || term.contains("구직") || term.contains("일자리"))) {
            return "청년 취업 준비 및 구직 활동을 지원하는 정책";
        }
        if (!intentTerms.isEmpty()) {
            return String.join(" ", intentTerms) + " 정책";
        }
        return "청년 지원 정책";
    }

    private boolean financialAssetIntent(String query) {
        return containsAny(query, "계좌", "통장", "저축", "자산");
    }

    private boolean containsAny(String text, String... terms) {
        if (text == null) return false;
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }
}
