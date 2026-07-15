package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchMode;
import com.themoa.youthcentersearch.policy.region.UserRegionResolution;
import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PolicySearchConditionValidator {
    private final ExplicitConditionDetector explicitDetector;
    private final PolicyKeywordExtractor keywordExtractor;
    private final UserRegionTextResolver userRegionTextResolver;
    private final UserEmploymentStatusDetector employmentStatusDetector;

    public PolicySearchConditionValidator(ExplicitConditionDetector explicitDetector,
                                          PolicyKeywordExtractor keywordExtractor,
                                          UserRegionTextResolver userRegionTextResolver,
                                          UserEmploymentStatusDetector employmentStatusDetector) {
        this.explicitDetector = explicitDetector;
        this.keywordExtractor = keywordExtractor;
        this.userRegionTextResolver = userRegionTextResolver;
        this.employmentStatusDetector = employmentStatusDetector;
    }

    public PolicySearchCondition validate(String query, PolicySearchCondition parsed, Integer resultSize) {
        UserRegionResolution resolvedRegion = resolveRegion(query, parsed);
        boolean regionExplicit = resolvedRegion.resolved();
        boolean ageExplicit = explicitDetector.ageExplicit(query);
        var detectedEmployment = employmentStatusDetector.detect(query);
        boolean employmentExplicit = detectedEmployment.explicit();
        boolean studentExplicit = explicitDetector.studentExplicit(query);
        boolean categoryExplicit = explicitDetector.categoryExplicit(query);
        boolean supportTypeExplicit = explicitDetector.supportTypeExplicit(query);
        var keywords = keywordExtractor.extract(query, parsed == null ? null : parsed.keywords());

        String province = regionExplicit ? resolvedRegion.province() : null;
        String city = regionExplicit ? resolvedRegion.city() : null;
        String district = regionExplicit ? resolvedRegion.district() : null;
        Integer age = ageExplicit && parsed != null ? parsed.age() : null;
        String employment = detectedEmployment.explicit()
                ? detectedEmployment.status().name()
                : employmentFromParsedWithEvidence(query, parsed);
        Boolean student = studentExplicit && parsed != null ? parsed.studentStatus() : null;
        String category = parsed == null ? null : parsed.category();
        var supportTypes = parsed == null ? java.util.Set.<String>of() : parsed.supportTypes();
        PolicySearchMode mode = mode(regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                StringUtils.hasText(category) || !supportTypes.isEmpty(), !keywords.coreKeywords().isEmpty());
        return new PolicySearchCondition(province, city, district, age, employment, student,
                parsed == null ? null : parsed.careerStage(), category, supportTypes, keywords.coreKeywords(),
                keywords.expandedKeywords(), resolvedRegion.regionName(), resolvedRegion.status().name(),
                resolvedRegion.regionLevel() == null ? null : resolvedRegion.regionLevel().name(),
                java.util.Set.copyOf(resolvedRegion.candidates()),
                regionExplicit, ageExplicit, employmentExplicit, studentExplicit,
                categoryExplicit, supportTypeExplicit, mode, resultSize);
    }

    private String employmentFromParsedWithEvidence(String query, PolicySearchCondition parsed) {
        if (parsed == null || !StringUtils.hasText(parsed.employmentStatus())) {
            return null;
        }
        var detected = employmentStatusDetector.detect(query);
        if (detected.explicit()) {
            return detected.status().name();
        }
        return explicitDetector.employmentExplicit(query) ? parsed.employmentStatus() : null;
    }

    private UserRegionResolution resolveRegion(String query, PolicySearchCondition parsed) {
        UserRegionResolution queryRegion = userRegionTextResolver.resolve(query);
        if (queryRegion.resolved() || queryRegion.status().name().equals("AMBIGUOUS")) {
            return queryRegion;
        }
        if (parsed != null && StringUtils.hasText(parsed.rawRegionText())) {
            UserRegionResolution raw = userRegionTextResolver.resolve(parsed.rawRegionText());
            if (raw.resolved() || raw.status().name().equals("AMBIGUOUS")) {
                return raw;
            }
        }
        if (parsed != null && StringUtils.hasText(parsed.province())) {
            String combined = parsed.province() + " " + (parsed.city() == null ? "" : parsed.city());
            UserRegionResolution openAiRegion = userRegionTextResolver.resolve(combined);
            if (openAiRegion.resolved() || openAiRegion.status().name().equals("AMBIGUOUS")) {
                return openAiRegion;
            }
        }
        return UserRegionResolution.notFound();
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
