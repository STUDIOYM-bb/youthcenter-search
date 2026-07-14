package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.RegionExternalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionExternalCodeRepository extends JpaRepository<RegionExternalCode, Long> {
    Optional<RegionExternalCode> findByCodeSystemAndExternalCode(String codeSystem, String externalCode);
    List<RegionExternalCode> findByRegionId(Integer regionId);
    long countByCodeSystem(String codeSystem);

    @Query("select count(e) from RegionExternalCode e where e.codeSystem = :codeSystem and e.region.regionLevel = :regionLevel")
    long countByCodeSystemAndRegionLevel(String codeSystem, String regionLevel);
}
