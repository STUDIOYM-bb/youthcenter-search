package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCondition;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.stream.Collectors;

@Component
public class PolicyDocumentBuilder {
    private final PolicyDocumentIdGenerator idGenerator;
    private final PolicyDocumentMetadataBuilder metadataBuilder;

    public PolicyDocumentBuilder(PolicyDocumentIdGenerator idGenerator, PolicyDocumentMetadataBuilder metadataBuilder) {
        this.idGenerator = idGenerator;
        this.metadataBuilder = metadataBuilder;
    }

    public BuiltPolicyDocument build(Policy policy) {
        String text = documentText(policy);
        String hash = sha256(text);
        Document document = Document.builder()
                .id(idGenerator.documentId(policy))
                .text(text)
                .metadata(metadataBuilder.metadata(policy, hash))
                .build();
        return new BuiltPolicyDocument(document, hash);
    }

    public String documentText(Policy policy) {
        StringBuilder builder = new StringBuilder();
        append(builder, "정책명", policy.getTitle());
        append(builder, "분야", policy.getCategory() == null ? null : policy.getCategory().name());
        append(builder, "지역", policy.getRegions().stream()
                .map(region -> region.getRegion().displayName())
                .distinct()
                .collect(Collectors.joining(", ")));
        PolicyCondition condition = policy.getCondition();
        if (condition != null) {
            append(builder, "지원 대상", condition.getConditionSummary());
            append(builder, "취업 조건", condition.getEmploymentStatus());
            append(builder, "나이 조건", age(condition));
        }
        append(builder, "지원 내용", policy.getSummary());
        append(builder, "신청 기간", period(policy));
        append(builder, "주관 기관", policy.getAgencyName());
        append(builder, "공식 URL", policy.getOfficialUrl());
        return builder.toString().trim();
    }

    private void append(StringBuilder builder, String label, String value) {
        if (StringUtils.hasText(value)) {
            builder.append('[').append(label).append("] ").append(value.trim()).append('\n');
        }
    }

    private String age(PolicyCondition condition) {
        if (condition.getMinAge() == null && condition.getMaxAge() == null) {
            return null;
        }
        return (condition.getMinAge() == null ? "" : "만 " + condition.getMinAge() + "세") + "~"
                + (condition.getMaxAge() == null ? "" : "만 " + condition.getMaxAge() + "세");
    }

    private String period(Policy policy) {
        if (policy.isAlwaysOpen()) {
            return "상시";
        }
        if (policy.getStartDate() == null && policy.getDueDate() == null) {
            return null;
        }
        return (policy.getStartDate() == null ? "" : policy.getStartDate().toString()) + "~"
                + (policy.getDueDate() == null ? "" : policy.getDueDate().toString());
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public record BuiltPolicyDocument(Document document, String contentHash) {
    }
}
