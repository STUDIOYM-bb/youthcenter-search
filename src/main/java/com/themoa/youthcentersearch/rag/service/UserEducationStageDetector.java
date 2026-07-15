package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.UserEducationStageCondition;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 사용자 원문에서 명시된 학교 단계를 판정한다.
 * 나이나 취준생 같은 상태 단어는 학교 단계를 보장하지 않으므로 사용하지 않는다.
 */
@Component
public class UserEducationStageDetector {
    private static final Pattern GRADUATE = Pattern.compile("대학원생|대학원\\s*재학생|대학원\\s*휴학생");
    private static final Pattern UNIVERSITY = Pattern.compile("대학생|전문대생|대학교\\s*재학생|대학\\s*재학생|대학\\s*휴학생|대학교\\s*휴학생|대학\\s*재학|대학교\\s*재학|재학\\s*중인\\s*대학");
    private static final Pattern HIGH = Pattern.compile("고교생|고등학생|고\\s*3|고3|특성화고생|마이스터고생|직업계고생|고등학교\\s*재학생");
    private static final Pattern MIDDLE = Pattern.compile("중학생|중학교\\s*재학생");
    private static final Pattern ELEMENTARY = Pattern.compile("초등학생|초등학교\\s*재학생");
    private static final Pattern GRADUATED = Pattern.compile("대학(?:교)?\\s*졸업|졸업\\s*후|졸업한");

    public UserEducationStageCondition detect(String query) {
        if (query == null || query.isBlank()) {
            return UserEducationStageCondition.unknown();
        }
        String normalized = query.replaceAll("\\s+", " ");
        Set<EducationStage> stages = EnumSet.noneOf(EducationStage.class);
        List<String> evidence = new ArrayList<>();
        addIfMatches(stages, evidence, normalized, GRADUATE, EducationStage.GRADUATE_SCHOOL, "대학원생 표현");
        if (!GRADUATED.matcher(normalized).find()) {
            addIfMatches(stages, evidence, normalized, UNIVERSITY, EducationStage.UNIVERSITY, "대학생/대학 재학생 표현");
        }
        addIfMatches(stages, evidence, normalized, HIGH, EducationStage.HIGH_SCHOOL, "고교생/고등학생 표현");
        addIfMatches(stages, evidence, normalized, MIDDLE, EducationStage.MIDDLE_SCHOOL, "중학생 표현");
        addIfMatches(stages, evidence, normalized, ELEMENTARY, EducationStage.ELEMENTARY, "초등학생 표현");
        if (stages.isEmpty()) {
            return UserEducationStageCondition.unknown();
        }
        return new UserEducationStageCondition(stages, true, evidence);
    }

    private void addIfMatches(Set<EducationStage> stages, List<String> evidence, String query,
                              Pattern pattern, EducationStage stage, String reason) {
        if (pattern.matcher(query).find()) {
            stages.add(stage);
            evidence.add(reason);
        }
    }
}
