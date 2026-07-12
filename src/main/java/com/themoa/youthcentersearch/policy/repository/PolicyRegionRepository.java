package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PolicyRegionRepository extends JpaRepository<PolicyRegion, Integer> {
    List<PolicyRegion> findByPolicy(Policy policy);
}
