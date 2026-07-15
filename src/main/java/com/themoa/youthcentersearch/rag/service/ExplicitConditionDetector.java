package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class ExplicitConditionDetector {
    private static final Pattern AGE = Pattern.compile("(만\\s*)?\\d{1,2}\\s*(살|세)");
    private static final Pattern STUDENT = Pattern.compile("대학생(?:이야|입니다|이에요|이고|으로|인\\s*나|인\\s*청년)?|재학생|휴학생");
    private static final Pattern EMPLOYMENT_CATEGORY_REQUEST = Pattern.compile(
            "(취업|구직|일자리|면접|채용).{0,12}(지원|정책|찾|추천|알려|필요|원해)"
                    + "|(지원|정책|찾|추천|알려|필요|원해).{0,12}(취업|구직|일자리|면접|채용)"
    );

    private final UserRegionTextResolver userRegionTextResolver;
    private final UserEmploymentStatusDetector employmentStatusDetector;

    public ExplicitConditionDetector(UserRegionTextResolver userRegionTextResolver,
                                     UserEmploymentStatusDetector employmentStatusDetector) {
        this.userRegionTextResolver = userRegionTextResolver;
        this.employmentStatusDetector = employmentStatusDetector;
    }

    public boolean ageExplicit(String query) {
        return query != null && AGE.matcher(query).find();
    }

    public boolean employmentExplicit(String query) {
        return employmentStatusDetector.detect(query).explicit();
    }

    public boolean studentExplicit(String query) {
        return query != null && STUDENT.matcher(query).find();
    }

    public boolean regionExplicit(String query) {
        return query != null && userRegionTextResolver.resolve(query).resolved();
    }

    public boolean supportTypeExplicit(String query) {
        if (query == null) return false;
        return query.contains("현금만") || query.contains("대출 제외") || query.contains("교육 말고")
                || query.contains("지원금") || query.contains("수당") || query.contains("월세");
    }

    public boolean categoryExplicit(String query) {
        if (query == null) return false;
        return query.contains("주거") || query.contains("월세") || query.contains("교육") || query.contains("금융")
                || EMPLOYMENT_CATEGORY_REQUEST.matcher(query).find();
    }
}
