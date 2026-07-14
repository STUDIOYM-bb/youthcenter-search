package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.PolicySourceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.List;

public interface PolicySourceSnapshotRepository extends JpaRepository<PolicySourceSnapshot, Long> {
    Optional<PolicySourceSnapshot> findByPolicyId(Integer policyId);

    Optional<PolicySourceSnapshot> findBySourceAndSourcePolicyId(String source, String sourcePolicyId);

    long countByPolicyActiveTrue();

    List<PolicySourceSnapshot> findByPolicyIdIn(List<Integer> policyIds);
}
