package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionNormalizer;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ExplicitConditionDetector {
    private static final Pattern AGE = Pattern.compile("(만\\s*)?\\d{1,2}\\s*(살|세)");
    private static final Pattern EMPLOYMENT = Pattern.compile("무직|미취업|취준생|취업\\s*준비|구직자|재직자|직장인|회사원|사회초년생");
    private static final Pattern STUDENT = Pattern.compile("대학생(?:이야|입니다|이에요|이고|으로|인\\s*나|인\\s*청년)?|재학생|휴학생");

    private final RegionCatalog regionCatalog;
    private final RegionNormalizer regionNormalizer;

    public ExplicitConditionDetector(RegionCatalog regionCatalog, RegionNormalizer regionNormalizer) {
        this.regionCatalog = regionCatalog;
        this.regionNormalizer = regionNormalizer;
    }

    public boolean ageExplicit(String query) {
        return query != null && AGE.matcher(query).find();
    }

    public boolean employmentExplicit(String query) {
        return query != null && EMPLOYMENT.matcher(query).find();
    }

    public boolean studentExplicit(String query) {
        return query != null && STUDENT.matcher(query).find();
    }

    public boolean regionExplicit(String query) {
        if (query == null || query.isBlank()) {
            return false;
        }
        if (!regionCatalog.findInText(regionNormalizer.normalizeProvince(query)).isEmpty()) {
            return true;
        }
        String compact = query.replaceAll("\\s+", "");
        return regionCatalog.allSpecificRegionsByLongestName().stream().anyMatch(region -> {
            if (region.getCity() == null || region.getCity().isBlank()) {
                return false;
            }
            String city = region.getCity().replaceAll("\\s+", "");
            String shortCity = city.replaceAll("(특별시|광역시|특별자치시|특별자치도|시|군|구)$", "");
            return !shortCity.isBlank() && compact.contains(shortCity);
        });
    }

    public boolean supportTypeExplicit(String query) {
        if (query == null) return false;
        return query.contains("현금만") || query.contains("대출 제외") || query.contains("교육 말고")
                || query.contains("지원금") || query.contains("수당") || query.contains("월세");
    }

    public boolean categoryExplicit(String query) {
        if (query == null) return false;
        return query.contains("주거") || query.contains("월세") || query.contains("교육") || query.contains("금융")
                || query.contains("일자리") || query.contains("취업");
    }
}
