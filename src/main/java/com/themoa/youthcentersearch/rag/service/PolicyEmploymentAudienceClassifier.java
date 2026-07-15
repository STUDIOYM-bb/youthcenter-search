package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.rag.dto.PolicyEmploymentAudience;
import com.themoa.youthcentersearch.rag.dto.UserEmploymentStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 정책 신청 대상이 재직자/미취업자 전용인지 판정한다.
 * 신청 대상/자격 필드를 우선하고, 정책 설명의 취업 주제어만으로 자격 상태를 단정하지 않는다.
 */
@Component
public class PolicyEmploymentAudienceClassifier {
    private static final Pattern UNEMPLOYED_ONLY = Pattern.compile("미취업자만|미취업\\s*청년|구직자\\s*대상|취업하지\\s*않은\\s*자|재직\\s*중이지\\s*않은\\s*자|고용보험\\s*미가입\\s*미취업자");
    private static final Pattern EMPLOYED_ONLY = Pattern.compile("재직자\\s*대상|중소기업\\s*재직\\s*청년|근로자\\s*대상|재직\\s*중인\\s*자|직장인\\s*대상");
    private static final Pattern BOTH_OR_NONE = Pattern.compile("청년\\s*누구나|\\d{1,2}\\s*세\\s*[~～-]\\s*\\d{1,2}\\s*세\\s*청년|취업\\s*여부\\s*무관|재직자와\\s*미취업자\\s*모두|일반\\s*청년\\s*대상");

    private final PolicySearchProjectionRepository projectionRepository;

    public PolicyEmploymentAudienceClassifier(PolicySearchProjectionRepository projectionRepository) {
        this.projectionRepository = projectionRepository;
    }

    public Map<Integer, PolicyEmploymentAudience> classify(Collection<Integer> policyIds) {
        if (policyIds == null || policyIds.isEmpty()) {
            return Map.of();
        }
        Map<Integer, PolicyEmploymentAudience> result = new LinkedHashMap<>();
        projectionRepository.findAllById(policyIds).forEach(projection ->
                result.put(projection.getPolicyId(), classify(projection)));
        return result;
    }

    public PolicyEmploymentAudience classify(PolicySearchProjection projection) {
        if (projection == null) {
            return PolicyEmploymentAudience.unknown();
        }
        String strong = text(projection.getTargetText(), projection.getQualificationText(), projection.getApplicationText());
        String support = text(projection.getTitleText(), projection.getDescriptionText(), projection.getSupportText());
        EnumSet<UserEmploymentStatus> statuses = EnumSet.noneOf(UserEmploymentStatus.class);
        java.util.ArrayList<String> evidence = new java.util.ArrayList<>();
        if (BOTH_OR_NONE.matcher(strong).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            statuses.add(UserEmploymentStatus.UNEMPLOYED);
            evidence.add("취업 여부 무관 또는 일반 청년 대상");
            return new PolicyEmploymentAudience(statuses, false, 0.8, evidence);
        }
        if (UNEMPLOYED_ONLY.matcher(strong).find()) {
            statuses.add(UserEmploymentStatus.UNEMPLOYED);
            evidence.add("대상/자격에 미취업자 전용 표현");
        }
        if (EMPLOYED_ONLY.matcher(strong).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            evidence.add("대상/자격에 재직자 전용 표현");
        }
        if (statuses.isEmpty() && EMPLOYED_ONLY.matcher(support).find()) {
            statuses.add(UserEmploymentStatus.EMPLOYED);
            evidence.add("정책명/설명에 재직자 대상 표현");
        }
        if (statuses.isEmpty()) {
            return PolicyEmploymentAudience.unknown();
        }
        boolean exclusive = statuses.size() == 1;
        return new PolicyEmploymentAudience(statuses, exclusive, StringUtils.hasText(strong) ? 0.9 : 0.65, evidence);
    }

    private String text(String... values) {
        return String.join(" ", java.util.Arrays.stream(values).map(value -> value == null ? "" : value).toList());
    }
}
