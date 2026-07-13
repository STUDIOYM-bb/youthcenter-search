package com.themoa.youthcentersearch.region.service;

import org.springframework.stereotype.Component;

@Component
public class MunicipalityHierarchyResolver {
    private final RegionMunicipalityNormalizer normalizer;

    public MunicipalityHierarchyResolver(RegionMunicipalityNormalizer normalizer) {
        this.normalizer = normalizer;
    }

    public NormalizedMunicipality normalize(String provinceName, String childCode, String childName, String fullAddress) {
        return normalizer.normalize(provinceName, childCode, childName, fullAddress);
    }
}
