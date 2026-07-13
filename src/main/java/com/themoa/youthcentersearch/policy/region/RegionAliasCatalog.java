package com.themoa.youthcentersearch.policy.region;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class RegionAliasCatalog {
    private final Map<String, String> provinceAliases = new LinkedHashMap<>();

    public RegionAliasCatalog() {
        provinceAliases.put("서울시", "서울특별시");
        provinceAliases.put("서울", "서울특별시");
        provinceAliases.put("부산", "부산광역시");
        provinceAliases.put("대구", "대구광역시");
        provinceAliases.put("인천", "인천광역시");
        provinceAliases.put("광주", "광주광역시");
        provinceAliases.put("대전", "대전광역시");
        provinceAliases.put("울산", "울산광역시");
        provinceAliases.put("세종", "세종특별자치시");
        provinceAliases.put("경기", "경기도");
        provinceAliases.put("강원", "강원특별자치도");
        provinceAliases.put("충북", "충청북도");
        provinceAliases.put("충남", "충청남도");
        provinceAliases.put("전북", "전북특별자치도");
        provinceAliases.put("전남", "전라남도");
        provinceAliases.put("경북", "경상북도");
        provinceAliases.put("경남", "경상남도");
        provinceAliases.put("제주도", "제주특별자치도");
        provinceAliases.put("제주", "제주특별자치도");
    }

    public String province(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return provinceAliases.getOrDefault(trimmed, trimmed);
    }

    public Map<String, String> provinceAliases() {
        return provinceAliases;
    }
}
