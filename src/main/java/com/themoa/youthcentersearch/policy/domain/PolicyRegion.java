package com.themoa.youthcentersearch.policy.domain;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "policy_region", uniqueConstraints = @UniqueConstraint(name = "uk_policy_region", columnNames = {"policy_id", "region_id"}))
public class PolicyRegion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private Policy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "region_id", nullable = false)
    private RegionCode region;

    protected PolicyRegion() {
    }

    public PolicyRegion(Policy policy, RegionCode region) {
        this.policy = policy;
        this.region = region;
    }

    public RegionCode getRegion() {
        return region;
    }
}
