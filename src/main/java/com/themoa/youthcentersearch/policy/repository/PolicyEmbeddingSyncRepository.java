package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.PolicyEmbeddingSync;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicyEmbeddingSyncRepository extends JpaRepository<PolicyEmbeddingSync, Long> {
    Optional<PolicyEmbeddingSync> findByPolicyId(Integer policyId);

    long countBySyncStatus(String syncStatus);

    @Query("select s.policy.id from PolicyEmbeddingSync s where s.syncStatus = :status order by s.requestedAt asc")
    List<Integer> findPolicyIdsByStatus(String status, Pageable pageable);
}
