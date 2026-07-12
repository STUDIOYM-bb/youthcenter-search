package com.themoa.youthcentersearch.youthcenter.dto.response;

import java.util.List;

public record NaturalLanguagePolicyCondition(
        String region,
        Integer age,
        String employmentStatus,
        String studentStatus,
        String category,
        List<String> supportTypes,
        List<String> keywords,
        String primarySearchKeyword,
        String descriptionSearchText
) {
    public NaturalLanguagePolicyCondition {
        supportTypes = supportTypes == null ? List.of() : List.copyOf(supportTypes);
        keywords = keywords == null ? List.of() : List.copyOf(keywords);
    }
}
