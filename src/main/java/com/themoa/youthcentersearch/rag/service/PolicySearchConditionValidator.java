package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolicySearchConditionValidator {
    private final ExplicitConditionDetector explicitDetector;
    private final PolicyKeywordExtractor keywordExtractor;

    public PolicySearchConditionValidator(ExplicitConditionDetector explicitDetector, PolicyKeywordExtractor keywordExtractor) {
        this.explicitDetector = explicitDetector;
        this.keywordExtractor = keywordExtractor;
    }

    public PolicySearchCondition validate(String query, PolicySearchCondition parsed, Integer resultSize) {
        boolean regionExplicit = explicitDetector.regionExplicit(query);
        boolean ageExplicit = explicitDetector.ageExplicit(query);
        boolean employmentExplicit = explicitDetector.employmentExplicit(query);
        boolean studentExplicit = explicitDetector.studentExplicit(query);
        boolean categoryExplicit = explicitDetector.categoryExplicit(query);
        boolean supportTypeExplicit = explicitDetector.supportTypeExplicit(query);
        var keywords = keywordExtractor.extract(query, parsed == null ? null : parsed.keywords());

        String province = regionExplicit && parsed != null ? parsed.province() : null;
        String city = regionExplicit && parsed != null ? parsed.city() : null;
        String district = regionExplicit && parsed != null ? parsed.district() : null;
        Integer age = ageExplicit && parsed != null ? parsed.age() : null;
        String employment = employmentExplicit && parsed != null ? parsed.employmentStatus() : null;
        Boolean student = studentExplicit && parsed != null ? parsed.studentStatus() : null;
        String category = parsed == null ? null : parsed.category();
        var supportTypes = parsed == null ? java.util.Set.<String>of() : parsed.supportTypes();
        PolicySearchMode mode = mode(regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                StringUtils.hasText(category) || !supportTypes.isEmpty(), !keywords.coreKeywords().isEmpty());
        return new PolicySearchCondition(province, city, district, age, employment, student,
                parsed == null ? null : parsed.careerStage(), category, supportTypes, keywords.coreKeywords(),
                keywords.expandedKeywords(), regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                categoryExplicit, supportTypeExplicit, mode, resultSize);
    }

    private PolicySearchMode mode(boolean regionExplicit,
                                  boolean ageExplicit,
                                  boolean employmentExplicit,
                                  boolean studentExplicit,
                                  boolean categoryOrSupportPresent,
                                  boolean keywordPresent) {
        boolean hasHardCondition = regionExplicit || ageExplicit || employmentExplicit || studentExplicit;
        if (hasHardCondition && keywordPresent) {
            return PolicySearchMode.HYBRID;
        }
        if (hasHardCondition || (categoryOrSupportPresent && !keywordPresent)) {
            return PolicySearchMode.CONDITION;
        }
        return PolicySearchMode.KEYWORD;
    }
}
