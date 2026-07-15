package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.rag.dto.EducationStage;
import com.themoa.youthcentersearch.rag.dto.PolicyTargetAudienceClassification;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 정책 Search Projection에서 신청 대상 학교 단계를 판정한다.
 * 검색 후보 전체를 한 번에 받아 projection을 batch 조회하므로 후보 수만큼 repository를 반복 호출하지 않는다.
 */
@Component
public class PolicyTargetAudienceClassifier {
    private static final Pattern HIGH = Pattern.compile("고교생|고등학생|고\\s*3|고3|특성화고\\s*(?:재학생|학생|생)|마이스터고\\s*(?:재학생|학생|생)|직업계고\\s*(?:재학생|학생|생)|고등학교\\s*재학생");
    private static final Pattern UNIVERSITY = Pattern.compile("대학생|전문대생|대학교\\s*(?:재학생|휴학생)|대학\\s*(?:재학생|휴학생)|전문대학\\s*재학생");
    private static final Pattern GRADUATE = Pattern.compile("대학원생|대학원\\s*(?:재학생|휴학생)");
    private static final Pattern MIDDLE = Pattern.compile("중학생|중학교\\s*재학생");
    private static final Pattern ELEMENTARY = Pattern.compile("초등학생|초등학교\\s*재학생");
    private static final Pattern ALL_STUDENTS = Pattern.compile("학생\\s*대상|재학생\\s*대상|학생이면|초·?중·?고|초중고|모든\\s*학생|전체\\s*학생");
    private static final Pattern GENERAL_YOUTH = Pattern.compile("\\d{1,2}\\s*세\\s*[~～-]\\s*\\d{1,2}\\s*세\\s*청년|청년\\s*(?:누구나|구직자|대상)|만\\s*\\d{1,2}\\s*세.*청년");
    private static final Pattern EXCLUDE_UNIVERSITY = Pattern.compile("대학생\\s*제외|대학교\\s*재학생\\s*제외|대학\\s*재학생\\s*제외");

    private final PolicySearchProjectionRepository projectionRepository;

    public PolicyTargetAudienceClassifier(PolicySearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public Map<Integer, PolicyTargetAudienceClassification> classify(Collection<Integer> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, PolicyTargetAudienceClassification> result = new LinkedHashMap<>();
        projectionRepository.findAllById(policyIds).forEach(projection ->
                result.put(projection.getPolicyId(), classify(projection)));
        return result;
    }

    public PolicyTargetAudienceClassification classify(PolicySearchProjection projection) {
        if (projection == null) {
            return PolicyTargetAudienceClassification.unknown();
        }
        String strong = text(projection.getTargetText(), projection.getQualificationText());
        String title = nullToEmpty(projection.getTitleText());
        String weak = text(projection.getDescriptionText(), projection.getSupportText());
        EnumSet<EducationStage> included = EnumSet.noneOf(EducationStage.class);
        EnumSet<EducationStage> excluded = EnumSet.noneOf(EducationStage.class);
        java.util.ArrayList<String> evidence = new java.util.ArrayList<>();

        collectStages(strong, included, evidence, "대상/자격");
        if (included.isEmpty()) {
            collectStages(title, included, evidence, "정책명");
        }
        if (included.isEmpty()) {
            collectBroadStages(strong + " " + title + " " + weak, included, evidence);
        }
        if (EXCLUDE_UNIVERSITY.matcher(strong + " " + title).find()) {
            excluded.add(EducationStage.UNIVERSITY);
            evidence.add("대학생 제외 표현");
        }
        if (included.isEmpty()) {
            return PolicyTargetAudienceClassification.unknown();
        }
        boolean exclusive = included.stream().anyMatch(this::isSpecificStage)
                && !included.contains(EducationStage.GENERAL_YOUTH)
                && !included.contains(EducationStage.ALL_STUDENTS);
        double confidence = StringUtils.hasText(strong) ? 0.9 : 0.65;
        return new PolicyTargetAudienceClassification(included, excluded, exclusive, confidence, evidence);
    }

    private void collectStages(String text, EnumSet<EducationStage> stages, List<String> evidence, String field) {
        add(text, stages, evidence, HIGH, EducationStage.HIGH_SCHOOL, field + "에 고교생 표현");
        add(text, stages, evidence, UNIVERSITY, EducationStage.UNIVERSITY, field + "에 대학생 표현");
        add(text, stages, evidence, GRADUATE, EducationStage.GRADUATE_SCHOOL, field + "에 대학원생 표현");
        add(text, stages, evidence, MIDDLE, EducationStage.MIDDLE_SCHOOL, field + "에 중학생 표현");
        add(text, stages, evidence, ELEMENTARY, EducationStage.ELEMENTARY, field + "에 초등학생 표현");
    }

    private void collectBroadStages(String text, EnumSet<EducationStage> stages, List<String> evidence) {
        if (ALL_STUDENTS.matcher(text).find()) {
            stages.add(EducationStage.ALL_STUDENTS);
            evidence.add("학교 단계가 특정되지 않은 학생 대상");
        }
        if (GENERAL_YOUTH.matcher(text).find()) {
            stages.add(EducationStage.GENERAL_YOUTH);
            evidence.add("특정 학교 단계가 아닌 청년 대상");
        }
    }

    private void add(String text, EnumSet<EducationStage> stages, List<String> evidence,
                     Pattern pattern, EducationStage stage, String reason) {
        if (StringUtils.hasText(text) && pattern.matcher(text).find()) {
            stages.add(stage);
            evidence.add(reason);
        }
    }

    private boolean isSpecificStage(EducationStage stage) {
        return switch (stage) {
            case ELEMENTARY, MIDDLE_SCHOOL, HIGH_SCHOOL, UNIVERSITY, GRADUATE_SCHOOL -> true;
            case ALL_STUDENTS, GENERAL_YOUTH, UNKNOWN -> false;
        };
    }

    private String text(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).map(this::nullToEmpty).toList());
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
