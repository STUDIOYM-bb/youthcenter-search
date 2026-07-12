package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "policy")
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "source_policy_id", nullable = false, unique = true, length = 100)
    private String sourcePolicyId;

    @Column(name = "source_type", nullable = false, length = 50)
    private String sourceType;

    @Column(name = "agency_name", nullable = false, length = 100)
    private String agencyName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PolicyCategory category;

    @Column(length = 500)
    private String summary;

    @Column(name = "official_url", length = 500)
    private String officialUrl;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_always_open", nullable = false)
    private boolean alwaysOpen;

    @Column(name = "is_active", nullable = false)
    private boolean active;

    @Column(nullable = false, length = 50)
    private String status;

    @OneToOne(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private PolicyCondition condition;

    @OneToMany(mappedBy = "policy", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<PolicyRegion> regions = new LinkedHashSet<>();

    protected Policy() {
    }

    public Policy(String sourcePolicyId) {
        this.sourcePolicyId = sourcePolicyId;
        this.sourceType = PolicySource.YOUTH_CENTER.name();
    }

    public void updateBasic(String title, String agencyName, PolicyCategory category, String summary,
                            String officialUrl, LocalDate startDate, LocalDate dueDate,
                            boolean alwaysOpen, boolean active, String status) {
        this.title = title;
        this.agencyName = agencyName;
        this.category = category;
        this.summary = summary;
        this.officialUrl = officialUrl;
        this.startDate = startDate;
        this.dueDate = dueDate;
        this.alwaysOpen = alwaysOpen;
        this.active = active;
        this.status = status;
    }

    public void updateCondition(PolicyCondition condition) {
        this.condition = condition;
        condition.attach(this);
    }

    public Integer getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getSourcePolicyId() {
        return sourcePolicyId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getAgencyName() {
        return agencyName;
    }

    public PolicyCategory getCategory() {
        return category;
    }

    public String getSummary() {
        return summary;
    }

    public String getOfficialUrl() {
        return officialUrl;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isAlwaysOpen() {
        return alwaysOpen;
    }

    public boolean isActive() {
        return active;
    }

    public String getStatus() {
        return status;
    }

    public PolicyCondition getCondition() {
        return condition;
    }

    public Set<PolicyRegion> getRegions() {
        return regions;
    }
}
