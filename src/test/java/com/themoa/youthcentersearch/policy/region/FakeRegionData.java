package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;

import java.util.List;

public final class FakeRegionData {
    private FakeRegionData() {
    }

    public static List<RegionCode> regions() {
        return List.of(
                new RegionCode(null, "KR", "전국", null, "NATIONWIDE"),
                new RegionCode(null, "11", "서울특별시", null, "PROVINCE"),
                new RegionCode(null, "26", "부산광역시", null, "PROVINCE"),
                new RegionCode(null, "28", "인천광역시", null, "PROVINCE"),
                new RegionCode(null, "28177", "인천광역시", "미추홀구", "DISTRICT"),
                new RegionCode(null, "28200", "인천광역시", "남동구", "DISTRICT"),
                new RegionCode(null, "28237", "인천광역시", "부평구", "DISTRICT"),
                new RegionCode(null, "29", "광주광역시", null, "PROVINCE"),
                new RegionCode(null, "41", "경기도", null, "PROVINCE"),
                new RegionCode(null, "41110", "경기도", "수원시", "CITY"),
                new RegionCode(null, "41111", "경기도", "수원시 장안구", "DISTRICT"),
                new RegionCode(null, "41113", "경기도", "수원시 권선구", "DISTRICT"),
                new RegionCode(null, "41115", "경기도", "수원시 팔달구", "DISTRICT"),
                new RegionCode(null, "41117", "경기도", "수원시 영통구", "DISTRICT"),
                new RegionCode(null, "41130", "경기도", "성남시", "CITY"),
                new RegionCode(null, "41220", "경기도", "평택시", "CITY"),
                new RegionCode(null, "41410", "경기도", "군포시", "CITY"),
                new RegionCode(null, "41460", "경기도", "용인시", "CITY"),
                new RegionCode(null, "41610", "경기도", "광주시", "CITY"),
                new RegionCode(null, "42", "강원특별자치도", null, "PROVINCE"),
                new RegionCode(null, "42730", "강원특별자치도", "횡성군", "CITY"),
                new RegionCode(null, "44", "충청남도", null, "PROVINCE"),
                new RegionCode(null, "44210", "충청남도", "서산시", "CITY"),
                new RegionCode(null, "44810", "충청남도", "예산군", "CITY"),
                new RegionCode(null, "46", "전라남도", null, "PROVINCE"),
                new RegionCode(null, "46820", "전라남도", "해남군", "CITY"),
                new RegionCode(null, "47", "경상북도", null, "PROVINCE"),
                new RegionCode(null, "47850", "경상북도", "칠곡군", "CITY"),
                new RegionCode(null, "48", "경상남도", null, "PROVINCE"),
                new RegionCode(null, "48120", "경상남도", "창원시", "CITY"),
                new RegionCode(null, "48890", "경상남도", "합천군", "CITY"),
                new RegionCode(null, "50", "제주특별자치도", null, "PROVINCE")
        );
    }
}
