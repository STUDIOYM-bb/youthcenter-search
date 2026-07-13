package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.Policy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Integer> {
    Optional<Policy> findBySourceTypeAndSourcePolicyId(String sourceType, String sourcePolicyId);

    long countByActiveTrue();

    List<Policy> findByActiveTrue(Pageable pageable);

    @EntityGraph(attributePaths = {"condition", "regions", "regions.region"})
    @Query("select p from Policy p where p.id in :ids")
    List<Policy> findWithRelationsByIdIn(List<Integer> ids);

    @Query("select p.id from Policy p where p.active = true order by p.id asc")
    List<Integer> findActivePolicyIds(Pageable pageable);
}
