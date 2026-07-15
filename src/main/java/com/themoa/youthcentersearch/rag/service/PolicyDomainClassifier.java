package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class PolicyDomainClassifier {
    private final PolicySearchProjectionRepository projectionRepository;

    public PolicyDomainClassifier() {
        this.projectionRepository = null;
    }

    @Autowired
    public PolicyDomainClassifier(PolicySearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    /**
     * 정책의 주요 분야와 지원 목적을 판정한다.
     * 신청 자격 문구에 등장한 단어는 보조 근거로만 쓰고, 제목/공식 키워드/지원 내용의 목적 단어를 우선한다.
     */
    public PolicyDomainClassification classify(Policy policy) {
        List<String> evidence = new ArrayList<>();
        SearchDomain categoryDomain = fromCategory(policy.getCategory());
        ProjectionText projection = projectionText(policy);
        String primaryText = projection.primaryPurposeText();
        String qualificationText = projection.qualificationText();
        String allText = primaryText + " " + qualificationText + " " + (policy.getCategory() == null ? "" : policy.getCategory().name());
        Set<SearchDomain> secondary = new LinkedHashSet<>();
        Set<SupportIntent> supportIntents = supportIntents(primaryText, allText);

        SearchDomain primary = categoryDomain;
        double confidence = categoryDomain == SearchDomain.GENERAL ? 0.45 : 0.8;
        if (categoryDomain != SearchDomain.GENERAL) {
            evidence.add("정책 카테고리: " + policy.getCategory().name());
        }
        if (categoryDomain == SearchDomain.EMPLOYMENT && employmentPurpose(primaryText)) {
            confidence = 0.95;
            evidence.add("정책명/지원내용이 취업·구직·면접 지원 중심");
        }
        if (categoryDomain != SearchDomain.EMPLOYMENT && employmentPurpose(primaryText)) {
            secondary.add(SearchDomain.EMPLOYMENT);
            supportIntents.add(SupportIntent.EMPLOYMENT_SUPPORT);
            evidence.add("교육/훈련 정책이지만 정책 목적에 취업·채용·면접 지원이 포함됨");
        } else if (categoryDomain != SearchDomain.EMPLOYMENT && employmentPurpose(qualificationText)) {
            secondary.add(SearchDomain.EMPLOYMENT);
            evidence.add("자격 조건에 취업 관련 표현 존재: 주 목적 판정에는 사용하지 않음");
        }
        if (primary == SearchDomain.GENERAL) {
            primary = inferFromText(primaryText, evidence);
            confidence = primary == SearchDomain.GENERAL ? confidence : 0.7;
        }
        return new PolicyDomainClassification(primary, secondary, supportIntents, confidence, evidence);
    }

    private SearchDomain fromCategory(PolicyCategory category) {
        if (category == null) return SearchDomain.GENERAL;
        return switch (category) {
            case 일자리 -> SearchDomain.EMPLOYMENT;
            case 주거 -> SearchDomain.HOUSING;
            case 교육 -> SearchDomain.EDUCATION;
            case 금융 -> SearchDomain.FINANCE;
            case 창업 -> SearchDomain.STARTUP;
            case 문화 -> SearchDomain.CULTURE;
            case 건강 -> SearchDomain.HEALTH;
            case 돌봄 -> SearchDomain.CARE;
            case 복지, 생활지원 -> SearchDomain.WELFARE;
            case 기타 -> SearchDomain.GENERAL;
        };
    }

    private SearchDomain inferFromText(String text, List<String> evidence) {
        if (employmentPurpose(text)) {
            evidence.add("정책명/지원내용의 취업 관련 주제어");
            return SearchDomain.EMPLOYMENT;
        }
        if (containsAny(text, "주거", "월세", "전세", "임대")) return SearchDomain.HOUSING;
        if (containsAny(text, "교육", "훈련", "강의")) return SearchDomain.EDUCATION;
        if (containsAny(text, "금융", "대출", "저축", "계좌", "생활비", "지원금")) return SearchDomain.FINANCE;
        if (containsAny(text, "창업", "사업화")) return SearchDomain.STARTUP;
        if (containsAny(text, "문화", "예술")) return SearchDomain.CULTURE;
        if (containsAny(text, "건강", "의료", "심리")) return SearchDomain.HEALTH;
        return SearchDomain.GENERAL;
    }

    private boolean employmentPurpose(String text) {
        return containsAny(text, "취업 지원", "취업지원", "구직", "일자리", "면접", "채용", "직업훈련", "취업 준비", "취업준비");
    }

    private Set<SupportIntent> supportIntents(String primaryText, String text) {
        Set<SupportIntent> intents = new LinkedHashSet<>();
        if (containsAny(text, "자산형성", "목돈", "적립", "매칭")) intents.add(SupportIntent.ASSET_BUILDING);
        if (containsAny(text, "저축", "계좌", "통장")) intents.add(SupportIntent.SAVINGS);
        if (containsAny(text, "매칭", "정부지원금", "적립지원")) intents.add(SupportIntent.MATCHED_SAVINGS);
        if (containsAny(text, "현금", "지원금", "수당", "바우처")) intents.add(SupportIntent.CASH_ASSISTANCE);
        if (containsAny(text, "수당", "장려금")) intents.add(SupportIntent.ALLOWANCE);
        if (containsAny(text, "대출", "융자", "이자")) intents.add(SupportIntent.LOAN);
        if (containsAny(text, "월세", "주거비", "임차료")) intents.add(SupportIntent.HOUSING_COST);
        if (employmentPurpose(primaryText)) intents.add(SupportIntent.EMPLOYMENT_SUPPORT);
        if (containsAny(text, "교육", "훈련", "강의")) intents.add(SupportIntent.EDUCATION);
        if (intents.isEmpty()) intents.add(SupportIntent.GENERAL);
        return intents;
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) return true;
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private ProjectionText projectionText(Policy policy) {
        PolicySearchProjection projection = projectionRepository == null || policy.getId() == null
                ? null
                : projectionRepository.findByPolicyId(policy.getId()).orElse(null);
        if (projection == null) {
            String title = nullToEmpty(policy.getTitle());
            String summary = nullToEmpty(policy.getSummary());
            String condition = policy.getCondition() == null ? "" : nullToEmpty(policy.getCondition().getConditionSummary());
            return new ProjectionText(title + " " + summary, condition);
        }
        String primary = String.join(" ",
                nullToEmpty(projection.getTitleText()),
                nullToEmpty(projection.getKeywordText()),
                nullToEmpty(projection.getCategoryText()),
                nullToEmpty(projection.getSupportText()),
                nullToEmpty(projection.getTargetText()),
                nullToEmpty(projection.getDescriptionText()),
                nullToEmpty(projection.getInstitutionText()));
        return new ProjectionText(primary, nullToEmpty(projection.getQualificationText()));
    }

    private record ProjectionText(String primaryPurposeText, String qualificationText) {
    }
}
