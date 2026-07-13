package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.domain.RegionExternalCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegionProvincePersistenceService {
    public static final String CODE_SYSTEM_SGIS = "SGIS";

    private final RegionCodeRepository regionCodeRepository;
    private final RegionExternalCodeRepository externalCodeRepository;
    private final RegionInternalCodeGenerator codeGenerator;

    public RegionProvincePersistenceService(RegionCodeRepository regionCodeRepository,
                                            RegionExternalCodeRepository externalCodeRepository,
                                            RegionInternalCodeGenerator codeGenerator) {
        this.regionCodeRepository = regionCodeRepository;
        this.externalCodeRepository = externalCodeRepository;
        this.codeGenerator = codeGenerator;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistedRegion upsertProvince(String sgisCode, String provinceName) {
        String internalCode = codeGenerator.province(provinceName);
        UpsertCounter counter = new UpsertCounter();
        RegionCode region = regionCodeRepository.findByRegionCode(internalCode)
                .map(existing -> {
                    if (existing.update(null, provinceName, null, "PROVINCE")) counter.updated++;
                    else counter.unchanged++;
                    return existing;
                })
                .orElseGet(() -> {
                    counter.inserted++;
                    return regionCodeRepository.save(new RegionCode(null, internalCode, provinceName, null, "PROVINCE"));
                });
        upsertExternal(region, CODE_SYSTEM_SGIS, sgisCode);
        return new PersistedRegion(region, counter.inserted, counter.updated, counter.unchanged);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public PersistedRegion upsertMunicipality(RegionCode parent, NormalizedMunicipality municipality) {
        String internalCode = codeGenerator.municipality(municipality.provinceName(), municipality.municipalityName());
        UpsertCounter counter = new UpsertCounter();
        RegionCode region = regionCodeRepository.findByRegionCode(internalCode)
                .map(existing -> {
                    if (existing.update(parent, municipality.provinceName(), municipality.municipalityName(), "CITY")) counter.updated++;
                    else counter.unchanged++;
                    return existing;
                })
                .orElseGet(() -> {
                    counter.inserted++;
                    return regionCodeRepository.save(new RegionCode(parent, internalCode, municipality.provinceName(), municipality.municipalityName(), "CITY"));
                });
        upsertExternal(region, CODE_SYSTEM_SGIS, municipality.sourceCode());
        return new PersistedRegion(region, counter.inserted, counter.updated, counter.unchanged);
    }

    private void upsertExternal(RegionCode region, String codeSystem, String externalCode) {
        externalCodeRepository.findByCodeSystemAndExternalCode(codeSystem, externalCode)
                .ifPresentOrElse(existing -> existing.touch(region),
                        () -> externalCodeRepository.save(new RegionExternalCode(region, codeSystem, externalCode)));
    }

    private static class UpsertCounter {
        int inserted;
        int updated;
        int unchanged;
    }

    public record PersistedRegion(RegionCode region, int inserted, int updated, int unchanged) {
    }
}
