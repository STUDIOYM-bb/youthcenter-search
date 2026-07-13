package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.region-bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class RegionBootstrapService implements ApplicationRunner {
    private final RegionCodeRepository regionCodeRepository;

    public RegionBootstrapService(RegionCodeRepository regionCodeRepository) {
        this.regionCodeRepository = regionCodeRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        regionCodeRepository.findByRegionCode("KR")
                .orElseGet(() -> regionCodeRepository.save(new RegionCode(null, "KR", "전국", null, "NATIONWIDE")));
    }
}
