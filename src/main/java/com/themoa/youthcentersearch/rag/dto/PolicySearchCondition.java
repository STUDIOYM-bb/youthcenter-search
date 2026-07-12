package com.themoa.youthcentersearch.rag.dto;

import java.util.Set;

public record PolicySearchCondition(
        String province,
        String city,
        String district,
        Integer age,
        String employmentStatus,
        Boolean studentStatus,
        String careerStage,
        String category,
        Set<String> supportTypes,
        Set<String> keywords,
        Integer resultSize
) {
    public PolicySearchCondition {
        supportTypes = supportTypes == null ? Set.of() : Set.copyOf(supportTypes);
        keywords = keywords == null ? Set.of() : Set.copyOf(keywords);
    }
}
