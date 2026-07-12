package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.PolicyCollectionRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PolicyCollectionRunRepository extends JpaRepository<PolicyCollectionRun, Long> {
    Optional<PolicyCollectionRun> findTopByOrderByStartedAtDesc();
}
