package com.themoa.youthcentersearch.youthcenter.search;

import com.themoa.youthcentersearch.youthcenter.config.NaturalLanguageSearchProperties;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguagePolicyCondition;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class YouthCenterSearchPlanBuilderTest {
    @Test
    void naturalLanguageApiPlansUseShortKeywordFilterByDefault() {
        NaturalLanguageSearchProperties properties = new NaturalLanguageSearchProperties();
        YouthCenterSearchPlanBuilder builder = new YouthCenterSearchPlanBuilder(properties, new YouthCenterSearchParameterSanitizer());
        NaturalLanguagePolicyCondition condition = new NaturalLanguagePolicyCondition("수원시", 27, "UNEMPLOYED",
                null, null, List.of("현금지원", "수당"), List.of("청년", "지원금", "미취업 청년 현금지원 수당 보조금 청년 지원금"),
                null, "미취업 청년 현금지원 수당 보조금 청년 지원금");

        List<YouthCenterSearchPlan> plans = builder.build(condition);

        assertThat(plans).isNotEmpty();
        assertThat(plans).allMatch(plan -> "plcyKywdNm".equals(plan.filterType()));
        assertThat(plans).allMatch(plan -> plan.filterValue().split(",").length <= 2);
        assertThat(plans).noneMatch(plan -> plan.filterValue().contains("미취업 청년 현금지원"));
    }
}
