package com.themoa.youthcentersearch.region.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RegionMunicipalityNormalizerTest {
    private final RegionMunicipalityNormalizer normalizer =
            new RegionMunicipalityNormalizer(new RegionAdministrativeLevelResolver());

    @Test
    void keepsProvinceMunicipalities() {
        assertSupported("경상북도", "47850", "칠곡군", "칠곡군");
        assertSupported("경기도", "41110", "수원시", "수원시");
        assertSupported("충청남도", "44810", "예산군", "예산군");
    }

    @Test
    void keepsMetropolitanAutonomousDistricts() {
        assertSupported("서울특별시", "11680", "강남구", "강남구");
        assertSupported("인천광역시", "28237", "부평구", "부평구");
        assertSupported("부산광역시", "26710", "기장군", "기장군");
    }

    @Test
    void mergesGeneralCityDistrictsToUpperCity() {
        assertSupported("경기도", "41117", "수원시 영통구", "수원시");
        assertSupported("충청북도", "43113", "청주시 흥덕구", "청주시");
    }

    @Test
    void mergesEmdTextToMunicipalityWhenPresentAndIgnoresStandaloneEmd() {
        assertSupported("경상북도", "47850250", "칠곡군 왜관읍", "칠곡군");
        var standalone = normalizer.normalize("경상북도", "47850250", "왜관읍", "경상북도 칠곡군 왜관읍");
        assertThat(standalone.supported()).isFalse();
        assertThat(standalone.ignoredReason()).isEqualTo("EMD_NOT_SUPPORTED");
    }

    private void assertSupported(String province, String code, String childName, String expectedMunicipality) {
        var result = normalizer.normalize(province, code, childName, province + " " + childName);
        assertThat(result.supported()).isTrue();
        assertThat(result.provinceName()).isEqualTo(province);
        assertThat(result.municipalityName()).isEqualTo(expectedMunicipality);
        assertThat(result.sourceCode()).isEqualTo(code);
    }
}
