package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "policy_search_projection")
public class PolicySearchProjection {
    @Id
    @Column(name = "policy_id")
    private Integer policyId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "policy_id")
    private Policy policy;

    @Column(name = "source_policy_id", nullable = false, length = 100)
    private String sourcePolicyId;

    @Column(name = "normalized_title", nullable = false, length = 500)
    private String normalizedTitle;

    @Column(name = "title_text", nullable = false, columnDefinition = "text")
    private String titleText;

    @Column(name = "keyword_text", columnDefinition = "text")
    private String keywordText;

    @Column(name = "category_text", columnDefinition = "text")
    private String categoryText;

    @Column(name = "description_text", columnDefinition = "longtext")
    private String descriptionText;

    @Column(name = "support_text", columnDefinition = "longtext")
    private String supportText;

    @Column(name = "target_text", columnDefinition = "longtext")
    private String targetText;

    @Column(name = "qualification_text", columnDefinition = "longtext")
    private String qualificationText;

    @Column(name = "application_text", columnDefinition = "longtext")
    private String applicationText;

    @Column(name = "institution_text", columnDefinition = "text")
    private String institutionText;

    @Column(name = "full_search_text", nullable = false, columnDefinition = "longtext")
    private String fullSearchText;

    @Column(name = "projection_version", nullable = false, length = 50)
    private String projectionVersion;

    @Column(name = "missing_snapshot", nullable = false)
    private boolean missingSnapshot;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected PolicySearchProjection() {
    }

    public PolicySearchProjection(Policy policy) {
        this.policy = policy;
        this.policyId = policy.getId();
        this.sourcePolicyId = policy.getSourcePolicyId();
    }

    public void update(String normalizedTitle, String titleText, String keywordText, String categoryText,
                       String descriptionText, String supportText, String targetText, String qualificationText,
                       String applicationText, String institutionText, String fullSearchText,
                       String projectionVersion, boolean missingSnapshot) {
        this.normalizedTitle = normalizedTitle;
        this.titleText = titleText;
        this.keywordText = keywordText;
        this.categoryText = categoryText;
        this.descriptionText = descriptionText;
        this.supportText = supportText;
        this.targetText = targetText;
        this.qualificationText = qualificationText;
        this.applicationText = applicationText;
        this.institutionText = institutionText;
        this.fullSearchText = fullSearchText;
        this.projectionVersion = projectionVersion;
        this.missingSnapshot = missingSnapshot;
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getPolicyId() { return policyId; }
    public String getSourcePolicyId() { return sourcePolicyId; }
    public String getNormalizedTitle() { return normalizedTitle; }
    public String getTitleText() { return titleText; }
    public String getKeywordText() { return keywordText; }
    public String getCategoryText() { return categoryText; }
    public String getDescriptionText() { return descriptionText; }
    public String getSupportText() { return supportText; }
    public String getTargetText() { return targetText; }
    public String getQualificationText() { return qualificationText; }
    public String getApplicationText() { return applicationText; }
    public String getInstitutionText() { return institutionText; }
    public String getFullSearchText() { return fullSearchText; }
    public String getProjectionVersion() { return projectionVersion; }
    public boolean isMissingSnapshot() { return missingSnapshot; }
}
