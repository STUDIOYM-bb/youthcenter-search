package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.RegionSyncRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RegionSyncRunRepository extends JpaRepository<RegionSyncRun, Long> {
    Optional<RegionSyncRun> findTopByOrderByStartedAtDesc();
}
