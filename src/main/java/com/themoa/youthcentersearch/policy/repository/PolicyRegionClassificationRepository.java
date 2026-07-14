package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.PolicyRegionClassification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PolicyRegionClassificationRepository extends JpaRepository<PolicyRegionClassification, Integer> {
    long countByRegionScope(String regionScope);
    long countByClassifierVersion(String classifierVersion);
}
