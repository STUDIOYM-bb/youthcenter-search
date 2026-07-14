package com.themoa.youthcentersearch.policy.repository;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface RegionCodeRepository extends JpaRepository<RegionCode, Integer> {
    Optional<RegionCode> findByRegionCode(String regionCode);

    List<RegionCode> findByProvince(String province);

    List<RegionCode> findByProvinceAndCity(String province, String city);

    long countByRegionLevel(String regionLevel);

    long countByRegionCodeStartingWith(String prefix);

    @Query("select count(r) from RegionCode r where r.regionCode not like 'P:%' and r.regionCode not like 'M:%' and r.regionCode <> 'KR'")
    long countLegacyRegions();

    @Query("""
            select distinct r
            from Policy p
            join p.regions pr
            join pr.region r
            where p.active = true
              and r.regionLevel in ('CITY', 'DISTRICT')
              and r.parent is not null
            order by r.province asc, r.city asc
            """)
    List<RegionCode> findActiveDirectSigunguRegions();
}
