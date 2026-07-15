package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 자연어 질의를 검색 실행 계획으로 확정한다.
 * OpenAI 구조화 결과와 Rule 기반 의미 분석을 요청당 한 번만 병합하고, 이후 단계는 PolicySearchPlan만 참조한다.
 */
@Service
public class PolicySearchPlanService {
    private static final Pattern EMPLOYMENT_POLICY_REQUEST = Pattern.compile(
            "(취업|구직|일자리|면접|채용|직업훈련).{0,12}(지원|정책|찾|추천|알려|필요|원해|받고)"
                    + "|(지원|정책|찾|추천|알려|필요|원해|받고).{0,12}(취업|구직|일자리|면접|채용|직업훈련)"
                    + "|취업\\s*준비\\s*중|취준생"
    );

    private final CompositePolicySearchConditionParser conditionParser;
    private final PolicyQueryClassifier queryClassifier;
    private final SearchDomainIntentPolicy domainIntentPolicy;
    private final UserEducationStageDetector educationStageDetector;

    public PolicySearchPlanService(CompositePolicySearchConditionParser conditionParser,
                                   PolicyQueryClassifier queryClassifier,
                                   SearchDomainIntentPolicy domainIntentPolicy,
                                   UserEducationStageDetector educationStageDetector) {
        this.conditionParser = conditionParser;
        this.queryClassifier = queryClassifier;
        this.domainIntentPolicy = domainIntentPolicy;
        this.educationStageDetector = educationStageDetector;
    }

    /**
     * 입력 질의를 구조화 조건과 긍정/부정 선호가 포함된 검색 계획으로 변환한다.
     * 취업 상태는 PolicySearchCondition에 남기고, 취업 정책 선호는 domain/support intent로 분리한다.
     */
    public PlannedSearch build(String query, int resultSize) {
        PolicySearchConditionParser.ParsedPolicySearchCondition parsed = conditionParser.parse(query, resultSize);
        PolicySearchCondition condition = parsed.condition();
        PolicyQuerySemantics semantics = parsed.semantics() == null ? PolicyQuerySemantics.empty() : parsed.semantics();
        SearchQueryType queryType = queryClassifier.classify(query, condition, semantics);
        Set<SearchDomain> desiredDomains = normalizeDesiredDomains(query, semantics);
        Set<SupportIntent> desiredSupportIntents = supportIntents(desiredDomains, semantics.positiveKeywords());
        Set<SupportIntent> excludedSupportIntents = supportIntents(semantics.excludedDomains(), semantics.excludedKeywords());
        var educationStage = educationStageDetector.detect(query);
        String mode = parsed.parserMode()
                + (parsed.fallback() ? ":fallback" : "")
                + (parsed.fallbackReason() == null ? "" : ":" + parsed.fallbackReason());
        PolicySearchPlan plan = new PolicySearchPlan(
                queryType,
                query,
                semantics.normalizedGoal(),
                desiredDomains,
                semantics.excludedDomains(),
                desiredSupportIntents,
                excludedSupportIntents,
                semantics.positiveKeywords(),
                semantics.excludedKeywords(),
                condition,
                educationStage.stages(),
                educationStage.explicit(),
                semantics.explicitExclusion(),
                mode
        );
        return new PlannedSearch(parsed, plan, semantics);
    }

    private Set<SearchDomain> normalizeDesiredDomains(String query, PolicyQuerySemantics semantics) {
        Set<SearchDomain> desired = EnumSet.noneOf(SearchDomain.class);
        desired.addAll(semantics.desiredDomains());
        if (desired.contains(SearchDomain.EMPLOYMENT) && !employmentPolicyRequested(query, semantics.positiveKeywords())) {
            // "현재 무직"은 사용자 상태이지 취업 정책 선호가 아니다. OpenAI가 이를 desired domain으로 올려도 Java rule이 분리한다.
            desired.remove(SearchDomain.EMPLOYMENT);
        }
        desired.removeIf(domain -> domain == SearchDomain.WELFARE && !domainRequested(query, semantics.positiveKeywords(), domain));
        return desired.isEmpty() ? Set.of() : Set.copyOf(desired);
    }

    private boolean domainRequested(String query, Set<String> positiveKeywords, SearchDomain domain) {
        String text = (query == null ? "" : query) + " " + String.join(" ", positiveKeywords == null ? Set.of() : positiveKeywords);
        return switch (domain) {
            case WELFARE -> containsAny(text, "복지", "생활지원", "생활 안정", "생활안정", "생활비", "돌봄");
            case FINANCE -> containsAny(text, "금융", "자산", "저축", "계좌", "통장", "대출", "목돈", "적립");
            case HOUSING -> containsAny(text, "주거", "월세", "전세", "임대", "주택");
            case EDUCATION -> containsAny(text, "교육", "대학생", "재학생", "훈련", "강의");
            case STARTUP -> containsAny(text, "창업", "사업화");
            case CULTURE -> containsAny(text, "문화", "예술");
            case HEALTH -> containsAny(text, "건강", "의료", "심리");
            case CARE -> containsAny(text, "돌봄", "가족");
            case EMPLOYMENT -> employmentPolicyRequested(query, positiveKeywords);
            case GENERAL -> true;
        };
    }

    private boolean employmentPolicyRequested(String query, Set<String> positiveKeywords) {
        String text = query == null ? "" : query;
        if (EMPLOYMENT_POLICY_REQUEST.matcher(text).find()) {
            return true;
        }
        return positiveKeywords != null && positiveKeywords.stream()
                .anyMatch(term -> EMPLOYMENT_POLICY_REQUEST.matcher(term == null ? "" : term).find());
    }

    private Set<SupportIntent> supportIntents(Set<SearchDomain> domains, Set<String> terms) {
        Set<SupportIntent> intents = EnumSet.noneOf(SupportIntent.class);
        for (SearchDomain domain : domains == null ? Set.<SearchDomain>of() : domains) {
            intents.addAll(domainIntentPolicy.supportIntentsFor(domain));
        }
        for (String term : terms == null ? Set.<String>of() : terms) {
            intents.addAll(inferSupportIntents(term));
        }
        if (intents.isEmpty()) {
            return Set.of();
        }
        return Set.copyOf(intents);
    }

    private Set<SupportIntent> inferSupportIntents(String rawTerm) {
        String term = rawTerm == null ? "" : rawTerm.replaceAll("\\s+", "");
        Set<SupportIntent> intents = new LinkedHashSet<>();
        if (containsAny(term, "취업", "구직", "일자리", "채용", "면접", "직업훈련")) {
            intents.add(SupportIntent.EMPLOYMENT_SUPPORT);
        }
        if (containsAny(term, "주거", "월세", "전세", "임차료")) {
            intents.add(SupportIntent.HOUSING_COST);
        }
        if (containsAny(term, "교육", "강의", "훈련")) {
            intents.add(SupportIntent.EDUCATION);
        }
        if (containsAny(term, "자산형성", "목돈", "적립")) {
            intents.add(SupportIntent.ASSET_BUILDING);
        }
        if (containsAny(term, "저축", "계좌", "통장")) {
            intents.add(SupportIntent.SAVINGS);
        }
        if (containsAny(term, "매칭", "정부기여")) {
            intents.add(SupportIntent.MATCHED_SAVINGS);
        }
        if (containsAny(term, "대출", "융자", "이자")) {
            intents.add(SupportIntent.LOAN);
        }
        return intents;
    }

    private boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    public record PlannedSearch(PolicySearchConditionParser.ParsedPolicySearchCondition parsed,
                                PolicySearchPlan plan,
                                PolicyQuerySemantics semantics) {
    }
}
