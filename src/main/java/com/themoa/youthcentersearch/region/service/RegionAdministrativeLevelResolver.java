package com.themoa.youthcentersearch.region.service;

import org.springframework.stereotype.Component;

@Component
public class RegionAdministrativeLevelResolver {
    public boolean isProvinceUnitOnly(String provinceName) {
        return provinceName != null && provinceName.endsWith("특별자치시");
    }

    public boolean isProvinceLike(String provinceName) {
        return provinceName != null && provinceName.endsWith("도");
    }

    public boolean isMetropolitanLike(String provinceName) {
        return provinceName != null && !isProvinceLike(provinceName) && !isProvinceUnitOnly(provinceName);
    }

    public boolean isTownVillageOrDong(String name) {
        return name != null && (name.endsWith("읍") || name.endsWith("면") || name.endsWith("동") || name.endsWith("리"));
    }

    public boolean isMunicipalityName(String name) {
        return name != null && (name.endsWith("시") || name.endsWith("군") || name.endsWith("구"));
    }
}
