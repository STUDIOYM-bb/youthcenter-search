package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.region.service.RegionInternalCodeGenerator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "app.region-bootstrap.enabled", havingValue = "true", matchIfMissing = true)
public class RegionBootstrapService implements ApplicationRunner {
    private final RegionCodeRepository regionCodeRepository;
    private final RegionInternalCodeGenerator codeGenerator;

    public RegionBootstrapService(RegionCodeRepository regionCodeRepository, RegionInternalCodeGenerator codeGenerator) {
        this.regionCodeRepository = regionCodeRepository;
        this.codeGenerator = codeGenerator;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        regionCodeRepository.findByRegionCode(codeGenerator.nationwide())
                .orElseGet(() -> regionCodeRepository.save(new RegionCode(null, codeGenerator.nationwide(), "전국", null, "NATIONWIDE")));
    }
}
