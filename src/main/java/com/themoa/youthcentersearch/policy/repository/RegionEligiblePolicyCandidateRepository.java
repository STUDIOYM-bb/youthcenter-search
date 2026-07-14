package com.themoa.youthcentersearch.policy.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RegionEligiblePolicyCandidateRepository extends Repository<com.themoa.youthcentersearch.policy.domain.Policy, Integer> {

    @Query(value = """
            select p.id as policy_id, pr.region_id as region_id, cnt.region_count as region_count
            from policy p
            join policy_region pr on pr.policy_id = p.id
            join (
                select policy_id, count(*) as region_count
                from policy_region
                group by policy_id
            ) cnt on cnt.policy_id = p.id
            where p.is_active = true
              and pr.region_id in (:eligibleRegionIds)
            order by p.id asc
            """, nativeQuery = true)
    List<Object[]> findEligibleRegionRows(@Param("eligibleRegionIds") List<Integer> eligibleRegionIds);

    @Query(value = """
            select distinct p.id as policy_id,
                   case
                       when :userLevel = 'SIGUNGU'
                            and pr.region_id = :userRegionId
                            and cnt.region_count > 1 then 'MULTIPLE_REGION_MATCH'
                       when :userLevel = 'SIGUNGU'
                            and pr.region_id = :userRegionId then 'EXACT_SIGUNGU'
                       when :userLevel = 'SIGUNGU'
                            and pr.region_id = :parentSidoId
                            and cnt.region_count > 1 then 'MULTIPLE_REGION_MATCH'
                       when :userLevel = 'SIGUNGU'
                            and pr.region_id = :parentSidoId then 'PARENT_SIDO'
                       when :userLevel = 'SIDO'
                            and pr.region_id = :userRegionId
                            and cnt.region_count > 1 then 'MULTIPLE_REGION_MATCH'
                       when :userLevel = 'SIDO'
                            and pr.region_id = :userRegionId then 'EXACT_SIDO'
                       when pr.region_id = :nationwideRegionId then 'NATIONWIDE'
                       else 'MULTIPLE_REGION_MATCH'
                   end as compatibility
            from policy p
            join policy_region pr on pr.policy_id = p.id
            join (
                select policy_id, count(*) as region_count
                from policy_region
                group by policy_id
            ) cnt on cnt.policy_id = p.id
            where p.is_active = true
              and (
                  (:userLevel = 'SIGUNGU' and pr.region_id in (:userRegionId, :parentSidoId, :nationwideRegionId))
                  or (:userLevel = 'SIDO' and pr.region_id in (:userRegionId, :nationwideRegionId))
                  or (:userLevel = 'NATIONWIDE' and pr.region_id = :nationwideRegionId)
              )
            order by p.id asc
            """, nativeQuery = true)
    List<Object[]> findEligibleRows(@Param("userRegionId") Integer userRegionId,
                                    @Param("parentSidoId") Integer parentSidoId,
                                    @Param("nationwideRegionId") Integer nationwideRegionId,
                                    @Param("userLevel") String userLevel);
}
