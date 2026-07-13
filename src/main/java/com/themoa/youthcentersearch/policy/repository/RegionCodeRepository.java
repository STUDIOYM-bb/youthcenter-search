package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Integer> {
    Optional<RegionCode> findByRegionCode(String regionCode);

    List<RegionCode> findByProvince(String province);

    List<RegionCode> findByProvinceAndCity(String province, String city);

    long countByRegionLevel(String regionLevel);
}
