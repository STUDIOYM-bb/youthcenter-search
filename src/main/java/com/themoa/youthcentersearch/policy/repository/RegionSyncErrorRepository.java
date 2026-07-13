package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.RegionSyncError;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RegionSyncErrorRepository extends JpaRepository<RegionSyncError, Long> {
}
