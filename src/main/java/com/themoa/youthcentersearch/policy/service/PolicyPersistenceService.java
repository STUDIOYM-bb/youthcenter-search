package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import com.themoa.youthcentersearch.policy.domain.PolicySource;
import com.themoa.youthcentersearch.policy.domain.PolicyRawData;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.youthcenter.dto.parsed.YouthPolicyItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.util.Map;

@Service
public class PolicyPersistenceService {
    private final PolicyRepository policyRepository;
    private final PolicyFieldNormalizer normalizer;
    private final PolicySourceSnapshotService snapshotService;
    private final PolicyApplicabilityClassificationService applicabilityClassificationService;

    public PolicyPersistenceService(PolicyRepository policyRepository,
                                    PolicyFieldNormalizer normalizer,
                                    PolicySourceSnapshotService snapshotService,
                                    PolicyApplicabilityClassificationService applicabilityClassificationService) {
        this.policyRepository = policyRepository;
        this.normalizer = normalizer;
        this.snapshotService = snapshotService;
        this.applicabilityClassificationService = applicabilityClassificationService;
    }

    @Transactional
    public PolicyUpsertResult upsert(YouthPolicyItem item) {
        return upsert(item, null);
    }

    @Transactional
    public PolicyUpsertResult upsert(YouthPolicyItem item, PolicyRawData rawData) {
        Map<String, Object> fields = item.fields();
        String sourcePolicyId = firstText(item.policyNumber(), normalizer.text(fields, "plcyNo"));
        if (!StringUtils.hasText(sourcePolicyId)) {
            throw new IllegalArgumentException("온통청년 정책 번호(plcyNo)가 없습니다.");
        }

        Policy policy = policyRepository.findBySourceTypeAndSourcePolicyId(PolicySource.YOUTH_CENTER.name(), sourcePolicyId)
                .orElseGet(() -> new Policy(sourcePolicyId));
        boolean inserted = policy.getId() == null;

        String title = normalizer.truncate(firstText(item.policyName(), normalizer.text(fields, "plcyNm")), 200);
        String agency = normalizer.truncate(firstText(
                normalizer.text(fields, "sprvsnInstCdNm"),
                normalizer.text(fields, "operInstCdNm"),
                normalizer.text(fields, "rgtrInstCdNm"),
                normalizer.text(fields, "rgtrUpInstCdNm"),
                normalizer.text(fields, "rgtrHghrkInstCdNm"),
                "온통청년"), 100);
        String summary = normalizer.truncate(firstText(
                normalizer.text(fields, "plcyExplnCn"),
                normalizer.text(fields, "plcySprtCn"),
                item.policyDescription()), 500);
        LocalDate startDate = normalizer.date(fields, "bizPrdBgngYmd");
        LocalDate dueDate = normalizer.date(fields, "bizPrdEndYmd");
        boolean alwaysOpen = dueDate == null || String.valueOf(fields.getOrDefault("aplyYmd", "")).contains("상시");
        String status = dueDate != null && dueDate.isBefore(LocalDate.now()) ? "CLOSED" : "OPEN";

        policy.updateBasic(
                StringUtils.hasText(title) ? title : sourcePolicyId,
                agency,
                normalizer.category(fields),
                summary,
                normalizer.firstUrl(fields),
                startDate,
                dueDate,
                alwaysOpen,
                true,
                status
        );

        PolicyCondition condition = policy.getCondition();
        if (condition == null) {
            condition = new PolicyCondition(null, null, null, null, null, null, true);
            policy.updateCondition(condition);
        }
        condition.update(
                normalizer.integer(fields, "sprtTrgtMinAge"),
                normalizer.integer(fields, "sprtTrgtMaxAge"),
                employmentStatus(fields),
                studentStatus(fields),
                normalizer.truncate(incomeCondition(fields), 200),
                normalizer.truncate(conditionSummary(fields), 500),
                true
        );

        Policy saved = policyRepository.save(policy);
        snapshotService.upsert(saved.getId(), sourcePolicyId, rawData, fields);
        applicabilityClassificationService.classifyFromFields(saved, fields, true);
        return new PolicyUpsertResult(saved.getId(), inserted);
    }

    private String employmentStatus(Map<String, Object> fields) {
        String text = conditionSummary(fields);
        if (containsAny(text, "미취업", "구직", "취업준비", "무직")) return "UNEMPLOYED";
        if (containsAny(text, "재직", "근로자", "직장인")) return "EMPLOYED";
        return null;
    }

    private Boolean studentStatus(Map<String, Object> fields) {
        String text = conditionSummary(fields);
        if (containsAny(text, "대학생", "재학생", "휴학생")) return true;
        return null;
    }

    private String incomeCondition(Map<String, Object> fields) {
        return String.join(" ",
                nullToEmpty(normalizer.text(fields, "earnCndSeCd")),
                nullToEmpty(normalizer.text(fields, "earnMinAmt")),
                nullToEmpty(normalizer.text(fields, "earnMaxAmt")),
                nullToEmpty(normalizer.text(fields, "earnEtcCn"))).trim();
    }

    private String conditionSummary(Map<String, Object> fields) {
        return String.join(" ",
                nullToEmpty(normalizer.text(fields, "ptcpPrpTrgtCn")),
                nullToEmpty(normalizer.text(fields, "addAplyQlfcCndCn")),
                nullToEmpty(normalizer.text(fields, "jobCd")),
                nullToEmpty(normalizer.text(fields, "schoolCd"))).trim();
    }

    private boolean containsAny(String value, String... terms) {
        if (value == null) {
            return false;
        }
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value.trim();
            }
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
