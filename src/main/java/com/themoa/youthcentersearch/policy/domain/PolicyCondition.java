package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_condition")
public class PolicyCondition {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false, unique = true)
    private Policy policy;

    @Column(name = "min_age")
    private Integer minAge;

    @Column(name = "max_age")
    private Integer maxAge;

    @Column(name = "employment_status", length = 50)
    private String employmentStatus;

    @Column(name = "student_status")
    private Boolean studentStatus;

    @Column(name = "income_condition", length = 200)
    private String incomeCondition;

    @Column(name = "condition_summary", length = 500)
    private String conditionSummary;

    @Column(name = "need_check", nullable = false)
    private boolean needCheck;

    protected PolicyCondition() {
    }

    public PolicyCondition(Integer minAge, Integer maxAge, String employmentStatus, Boolean studentStatus,
                           String incomeCondition, String conditionSummary, boolean needCheck) {
        update(minAge, maxAge, employmentStatus, studentStatus, incomeCondition, conditionSummary, needCheck);
    }

    void attach(Policy policy) {
        this.policy = policy;
    }

    public void update(Integer minAge, Integer maxAge, String employmentStatus, Boolean studentStatus,
                       String incomeCondition, String conditionSummary, boolean needCheck) {
        this.minAge = minAge;
        this.maxAge = maxAge;
        this.employmentStatus = employmentStatus;
        this.studentStatus = studentStatus;
        this.incomeCondition = incomeCondition;
        this.conditionSummary = conditionSummary;
        this.needCheck = needCheck;
    }

    public Integer getMinAge() {
        return minAge;
    }

    public Integer getMaxAge() {
        return maxAge;
    }

    public String getEmploymentStatus() {
        return employmentStatus;
    }

    public Boolean getStudentStatus() {
        return studentStatus;
    }

    public String getConditionSummary() {
        return conditionSummary;
    }

    public boolean isNeedCheck() {
        return needCheck;
    }
}
