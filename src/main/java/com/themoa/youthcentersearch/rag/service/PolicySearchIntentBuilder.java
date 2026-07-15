package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class PolicySearchIntentBuilder {
    /**
     * 확정된 SearchPlan에서 Vector/BM25에 사용할 긍정 검색 의도만 생성한다.
     * 제외 단어는 후보 생성어로 쓰지 않고 Preference Filter에서만 사용한다.
     */
    public PolicySearchIntent build(PolicySearchPlan plan) {
        PolicyQuerySemantics semantics = new PolicyQuerySemantics(
                plan.normalizedGoal(),
                plan.desiredDomains(),
                plan.excludedDomains(),
                plan.positiveTerms(),
                plan.excludedTerms(),
                plan.explicitExclusion()
        );
        return build(plan.originalQuery(), plan.condition(), semantics);
    }

    public PolicySearchIntent build(String query, PolicySearchCondition condition) {
        return build(query, condition, PolicyQuerySemantics.empty());
    }

    public PolicySearchIntent build(String query, PolicySearchCondition condition, PolicyQuerySemantics semantics) {
        String original = query == null ? "" : query.trim();
        PolicyQuerySemantics effectiveSemantics = semantics == null ? PolicyQuerySemantics.empty() : semantics;
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
        if (condition.studentExplicit() && Boolean.TRUE.equals(condition.studentStatus())) {
            intentTerms.add("대학생");
            expanded.add("대학생");
        }
        effectiveSemantics.positiveKeywords().forEach(expanded::add);
        if (financialIntent(original, condition)) {
            intentTerms.add("금융 지원");
            intentTerms.add(financialAssetIntent(original) ? "자산형성 저축 계좌 지원" : "생활비 지원");
            expanded.addAll(Set.of("금융", "생활비", "지원금", "수당", "보조금", "장려금", "자립", "자산형성",
                    "사회초년생", "사회 초년생", "청년 금융", "청년 지원금"));
            if (financialAssetIntent(original)) {
                expanded.addAll(Set.of("계좌", "통장", "저축", "자산", "자산형성", "적립", "매칭", "목돈", "정부기여"));
            }
        }
        if (employmentIntent(original, condition, effectiveSemantics)) {
            intentTerms.add("취업 지원");
            intentTerms.add("구직 지원");
            intentTerms.add("취업 준비");
            expanded.addAll(Set.of("취업", "구직", "일자리", "채용", "취업준비", "구직활동", "취업역량", "직업훈련",
                    "취업 지원", "구직 지원", "취업 준비", "미취업 청년", "구직자"));
        }
        if (interviewIntent(effectiveSemantics)) {
            intentTerms.add("면접 지원");
            expanded.addAll(Set.of("면접", "면접수당", "면접 수당", "면접비", "면접 비용", "면접 지원금", "면접 정장", "면접 교통비"));
        } else if (employmentIntent(original, condition, effectiveSemantics)) {
            expanded.addAll(Set.of("면접", "면접수당", "면접비"));
        }
        if (StringUtils.hasText(condition.category())) {
            intentTerms.add(condition.category());
            expanded.add(condition.category());
        }
        expanded.addAll(condition.keywords());
        expanded.addAll(condition.expandedKeywords());
        effectiveSemantics.excludedKeywords().forEach(expanded::remove);
        for (SearchDomain excludedDomain : effectiveSemantics.excludedDomains()) {
            PolicyIntentPolarityDetector.termsFor(excludedDomain).forEach(expanded::remove);
        }
        expanded.removeIf(term -> !StringUtils.hasText(term));

        String semanticQuery = semanticQuery(intentTerms, expanded, condition, effectiveSemantics);
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

    private boolean employmentIntent(String query, PolicySearchCondition condition, PolicyQuerySemantics semantics) {
        if (semantics.excludedDomains().contains(SearchDomain.EMPLOYMENT)) {
            return false;
        }
        return semantics.desiredDomains().contains(SearchDomain.EMPLOYMENT)
                || semantics.positiveKeywords().stream().anyMatch(term -> containsAny(term, "취업", "취준", "구직", "일자리", "채용", "면접"))
                || "일자리".equals(condition.category()) || "취업 지원".equals(condition.category());
    }

    private boolean interviewIntent(PolicyQuerySemantics semantics) {
        return !semantics.excludedDomains().contains(SearchDomain.EMPLOYMENT)
                && semantics.positiveKeywords().stream().anyMatch(term -> containsAny(term, "면접", "면접수당", "면접비", "면접 비용"));
    }

    private boolean financialIntent(String query, PolicySearchCondition condition) {
        return containsAny(query, "금융", "지원금", "수당", "보조금", "장려금", "생활비", "사회 초년생", "사회초년생",
                "계좌", "통장", "저축", "자산")
                || "financialSupport".equalsIgnoreCase(condition.category())
                || "금융".equals(condition.category())
                || condition.supportTypes().stream().anyMatch(type -> Set.of("CASH", "ALLOWANCE", "SUBSIDY").contains(type));
    }

    private String semanticQuery(Set<String> intentTerms, Set<String> expanded, PolicySearchCondition condition,
                                 PolicyQuerySemantics semantics) {
        if (semantics.explicitExclusion() && StringUtils.hasText(semantics.normalizedGoal())) {
            return semantics.normalizedGoal();
        }
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
