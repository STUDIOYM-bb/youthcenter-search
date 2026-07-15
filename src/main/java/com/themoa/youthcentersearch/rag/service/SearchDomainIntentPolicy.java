package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import com.themoa.youthcentersearch.rag.dto.SupportIntent;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Set;

/**
 * 검색 분야(SearchDomain)와 실제 지원 목적(SupportIntent)의 관계를 한 곳에서 관리한다.
 * 사용자 제외 의도는 감점이 아니라 Hard Filter로 처리해야 하므로, 검색 서비스와 테스트가 같은 기준을 사용한다.
 */
@Component
public class SearchDomainIntentPolicy {
    private final EnumMap<SearchDomain, Set<SupportIntent>> supportIntentMap = new EnumMap<>(SearchDomain.class);

    public SearchDomainIntentPolicy() {
        supportIntentMap.put(SearchDomain.EMPLOYMENT, EnumSet.of(SupportIntent.EMPLOYMENT_SUPPORT));
        supportIntentMap.put(SearchDomain.HOUSING, EnumSet.of(SupportIntent.HOUSING_COST));
        supportIntentMap.put(SearchDomain.EDUCATION, EnumSet.of(SupportIntent.EDUCATION));
        supportIntentMap.put(SearchDomain.FINANCE, EnumSet.of(
                SupportIntent.ASSET_BUILDING,
                SupportIntent.SAVINGS,
                SupportIntent.MATCHED_SAVINGS,
                SupportIntent.LOAN
        ));
    }

    /**
     * 입력 분야에서 자연스럽게 기대되는 지원 목적을 반환한다.
     * 현금성 지원은 여러 분야에 걸쳐 나타나므로 FINANCE 제외의 자동 제거 근거로 사용하지 않는다.
     */
    public Set<SupportIntent> supportIntentsFor(SearchDomain domain) {
        return supportIntentMap.getOrDefault(domain, Set.of());
    }

    /**
     * 정책의 주된 분야/보조 분야/지원 목적 중 사용자가 명시적으로 제외한 의미가 있는지 판단한다.
     * 예를 들어 EDUCATION 카테고리라도 목적이 취업 교육이면 EMPLOYMENT_SUPPORT 때문에 제외된다.
     */
    public boolean isExcluded(PolicyDomainClassification policyDomain, PolicySearchPlan searchPlan) {
        if (policyDomain == null || searchPlan == null) {
            return false;
        }
        if (searchPlan.excludedDomains().contains(policyDomain.primaryDomain())) {
            return true;
        }
        if (policyDomain.secondaryDomains().stream().anyMatch(searchPlan.excludedDomains()::contains)) {
            return true;
        }
        Set<SupportIntent> excludedSupportIntents = EnumSet.noneOf(SupportIntent.class);
        for (SearchDomain excludedDomain : searchPlan.excludedDomains()) {
            excludedSupportIntents.addAll(supportIntentsFor(excludedDomain));
        }
        excludedSupportIntents.addAll(searchPlan.excludedSupportIntents());
        return policyDomain.supportIntents().stream().anyMatch(excludedSupportIntents::contains);
    }

    public boolean desiredDomainPasses(PolicyDomainClassification policyDomain, PolicySearchPlan searchPlan) {
        if (policyDomain == null || searchPlan == null) {
            return true;
        }
        Set<SearchDomain> desiredDomains = searchPlan.desiredDomains().stream()
                .filter(domain -> domain != SearchDomain.GENERAL)
                .collect(java.util.stream.Collectors.toCollection(() -> EnumSet.noneOf(SearchDomain.class)));
        Set<SupportIntent> desiredSupportIntents = searchPlan.desiredSupportIntents();
        if (desiredDomains.isEmpty() && desiredSupportIntents.isEmpty()) {
            return true;
        }
        if (desiredDomains.contains(policyDomain.primaryDomain())) {
            return true;
        }
        if (policyDomain.secondaryDomains().stream().anyMatch(desiredDomains::contains)) {
            return true;
        }
        return policyDomain.supportIntents().stream().anyMatch(desiredSupportIntents::contains);
    }
}
