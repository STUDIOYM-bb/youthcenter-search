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
        Set<String> expandedKeywords,
        String rawRegionText,
        String regionResolutionStatus,
        boolean regionExplicit,
        boolean ageExplicit,
        boolean employmentExplicit,
        boolean studentExplicit,
        boolean categoryExplicit,
        boolean supportTypeExplicit,
        PolicySearchMode searchMode,
        Integer resultSize
) {
    public PolicySearchCondition(String province,
                                 String city,
                                 String district,
                                 Integer age,
                                 String employmentStatus,
                                 Boolean studentStatus,
                                 String careerStage,
                                 String category,
                                 Set<String> supportTypes,
                                 Set<String> keywords,
                                 Integer resultSize) {
        this(province, city, district, age, employmentStatus, studentStatus, careerStage, category,
                supportTypes, keywords, Set.of(), null, null, false, false, false, false, false, false,
                PolicySearchMode.KEYWORD, resultSize);
    }

    public PolicySearchCondition {
        province = blankToNull(province);
        city = blankToNull(city);
        district = blankToNull(district);
        employmentStatus = blankToNull(employmentStatus);
        careerStage = blankToNull(careerStage);
        category = blankToNull(category);
        rawRegionText = blankToNull(rawRegionText);
        regionResolutionStatus = blankToNull(regionResolutionStatus);
        if (age != null && age <= 0) {
            age = null;
        }
        if (Boolean.FALSE.equals(studentStatus)) {
            studentStatus = null;
        }
        supportTypes = supportTypes == null ? Set.of() : Set.copyOf(supportTypes);
        keywords = keywords == null ? Set.of() : Set.copyOf(keywords);
        expandedKeywords = expandedKeywords == null ? Set.of() : Set.copyOf(expandedKeywords);
        searchMode = searchMode == null ? PolicySearchMode.KEYWORD : searchMode;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
