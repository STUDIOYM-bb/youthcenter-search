package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
public class RegionBootstrapService implements ApplicationRunner {
    private final RegionCodeRepository regionCodeRepository;

    public RegionBootstrapService(RegionCodeRepository regionCodeRepository) {
        this.regionCodeRepository = regionCodeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        seed(List.of(
                new RegionSeed("KR", "전국", null, "NATIONWIDE"),
                new RegionSeed("41", "경기도", null, "PROVINCE"),
                new RegionSeed("41110", "경기도", "수원시", "CITY"),
                new RegionSeed("41130", "경기도", "성남시", "CITY"),
                new RegionSeed("41460", "경기도", "용인시", "CITY"),
                new RegionSeed("11", "서울특별시", null, "PROVINCE"),
                new RegionSeed("26", "부산광역시", null, "PROVINCE"),
                new RegionSeed("28", "인천광역시", null, "PROVINCE"),
                new RegionSeed("28237", "인천광역시", "부평구", "DISTRICT"),
                new RegionSeed("29", "광주광역시", null, "PROVINCE"),
                new RegionSeed("27", "대구광역시", null, "PROVINCE"),
                new RegionSeed("50", "제주특별자치도", null, "PROVINCE")
        ));
    }

    private void seed(List<RegionSeed> seeds) {
        for (RegionSeed seed : seeds) {
            regionCodeRepository.findByRegionCode(seed.code())
                    .orElseGet(() -> regionCodeRepository.save(new RegionCode(null, seed.code(), seed.province(), seed.city(), seed.level())));
        }
    }

    private record RegionSeed(String code, String province, String city, String level) {
    }
}
