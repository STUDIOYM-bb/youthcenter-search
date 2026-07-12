package com.themoa.youthcentersearch.youthcenter.search;

import com.themoa.youthcentersearch.youthcenter.config.NaturalLanguageSearchProperties;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguagePolicyCondition;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class YouthCenterSearchPlanBuilder {
    private final NaturalLanguageSearchProperties properties;
    private final YouthCenterSearchParameterSanitizer sanitizer;

    public YouthCenterSearchPlanBuilder(NaturalLanguageSearchProperties properties,
                                        YouthCenterSearchParameterSanitizer sanitizer) {
        this.properties = properties;
        this.sanitizer = sanitizer;
    }

    public List<YouthCenterSearchPlan> build(NaturalLanguagePolicyCondition condition) {
        List<String> pool = keywordPool(condition);
        List<YouthCenterSearchPlan> plans = new ArrayList<>();

        addKeywordPlan(plans, pool, "泥?뀈", "吏?먭툑");
        addKeywordPlan(plans, pool, "痍⑥뾽", "吏?먭툑");
        addKeywordPlan(plans, pool, "硫댁젒?섎떦", "痍⑥뾽");

        if (properties.isDescriptionFilterEnabled() && plans.size() < properties.getMaxApiRequests()) {
            String description = sanitizer.selectShortDescriptionTerm(pool, properties.getDescriptionFilterMaxLength());
            if (StringUtils.hasText(description)) {
                addPlanIfAbsent(plans, new YouthCenterSearchPlan("plcyExplnCn", description, null, description));
            }
        }

        for (String keyword : pool) {
            if (plans.size() >= properties.getMaxApiRequests()) {
                break;
            }
            addPlanIfAbsent(plans, new YouthCenterSearchPlan("plcyKywdNm", keyword, keyword, null));
        }

        if (plans.isEmpty()) {
            plans.add(new YouthCenterSearchPlan("plcyKywdNm", "泥?뀈", "泥?뀈", null));
        }
        return plans.stream().limit(properties.getMaxApiRequests()).toList();
    }

    private List<String> keywordPool(NaturalLanguagePolicyCondition condition) {
        Set<String> raw = new LinkedHashSet<>();
        if (condition != null) {
            if (condition.keywords() != null) {
                raw.addAll(condition.keywords());
            }
            if (condition.supportTypes() != null) {
                raw.addAll(condition.supportTypes());
            }
            if (StringUtils.hasText(condition.category())) {
                raw.add(condition.category());
            }
            if ("UNEMPLOYED".equals(condition.employmentStatus())) {
                raw.add("痍⑥뾽");
                raw.add("援ъ쭅");
            }
        }
        raw.add("泥?뀈");
        return sanitizer.sanitizeKeywordPool(new ArrayList<>(raw));
    }

    private void addKeywordPlan(List<YouthCenterSearchPlan> plans, List<String> pool, String... keywords) {
        if (plans.size() >= properties.getMaxApiRequests()) {
            return;
        }
        List<String> values = new ArrayList<>();
        for (String keyword : keywords) {
            if (pool.contains(keyword) && values.size() < properties.getMaximumKeywordsPerRequest()) {
                values.add(keyword);
            }
        }
        if (!values.isEmpty()) {
            String value = String.join(",", values);
            addPlanIfAbsent(plans, new YouthCenterSearchPlan("plcyKywdNm", value, value, null));
        }
    }

    private void addPlanIfAbsent(List<YouthCenterSearchPlan> plans, YouthCenterSearchPlan plan) {
        boolean exists = plans.stream().anyMatch(existing -> existing.filterType().equals(plan.filterType())
                && existing.filterValue().equals(plan.filterValue()));
        if (!exists) {
            plans.add(plan);
        }
    }
}
