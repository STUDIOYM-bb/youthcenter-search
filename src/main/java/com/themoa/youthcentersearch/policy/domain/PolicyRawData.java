package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_raw_data")
public class PolicyRawData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(name = "source_policy_id", length = 150)
    private String sourcePolicyId;

    @Column(name = "request_url", nullable = false, length = 1000)
    private String requestUrl;

    @Column(name = "request_parameters", columnDefinition = "json")
    private String requestParameters;

    @Column(name = "response_body", nullable = false, columnDefinition = "longtext")
    private String responseBody;

    @Column(name = "response_format", nullable = false, length = 20)
    private String responseFormat;

    @Column(name = "parse_status", nullable = false, length = 30)
    private String parseStatus;

    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    protected PolicyRawData() {
    }

    public PolicyRawData(String source, String sourcePolicyId, String requestUrl, String requestParameters,
                         String responseBody, String responseFormat, String parseStatus, String errorMessage) {
        this.source = source;
        this.sourcePolicyId = sourcePolicyId;
        this.requestUrl = requestUrl;
        this.requestParameters = requestParameters;
        this.responseBody = responseBody;
        this.responseFormat = responseFormat;
        this.parseStatus = parseStatus;
        this.errorMessage = errorMessage;
        this.collectedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getRequestUrl() {
        return requestUrl;
    }

    public String getResponseBody() {
        return responseBody;
    }

    public String getResponseFormat() {
        return responseFormat;
    }

    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }
}
