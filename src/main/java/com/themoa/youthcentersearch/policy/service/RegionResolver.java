package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class RegionResolver {
    private static final Set<String> NON_REGION_WORDS = Set.of(
            "보건복지부", "고용노동부", "중소벤처기업부", "중앙행정기관", "공공기관", "한국장학재단"
    );

    private final RegionCodeRepository regionCodeRepository;
    private final PolicyFieldNormalizer normalizer;

    public RegionResolver(RegionCodeRepository regionCodeRepository, PolicyFieldNormalizer normalizer) {
        this.regionCodeRepository = regionCodeRepository;
        this.normalizer = normalizer;
    }

    public Set<RegionCode> resolve(Map<String, Object> fields) {
        Set<RegionCode> result = new LinkedHashSet<>();
        String zipCd = normalizer.text(fields, "zipCd");
        if (StringUtils.hasText(zipCd)) {
            for (String token : zipCd.split(",")) {
                String code = token.trim();
                regionCodeRepository.findByRegionCode(code).ifPresent(result::add);
            }
        }
        String text = String.join(" ",
                nullToEmpty(normalizer.text(fields, "plcyNm")),
                nullToEmpty(normalizer.text(fields, "plcyExplnCn")),
                nullToEmpty(normalizer.text(fields, "plcySprtCn")),
                nullToEmpty(normalizer.text(fields, "ptcpPrpTrgtCn")),
                nullToEmpty(normalizer.text(fields, "sprvsnInstCdNm", "operInstCdNm")));
        if (NON_REGION_WORDS.stream().noneMatch(text::contains)) {
            addByName(result, text, "전국", null);
            addByName(result, text, "경기도", null);
            addByName(result, text, "수원", "수원시");
            addByName(result, text, "성남", "성남시");
            addByName(result, text, "용인", "용인시");
            addByName(result, text, "서울", null);
            addByName(result, text, "부평", "부평구");
            addByName(result, text, "제주", null);
        }
        if (result.isEmpty()) {
            regionCodeRepository.findByRegionCode("KR").ifPresent(result::add);
        }
        return result;
    }

    private void addByName(Set<RegionCode> target, String text, String provinceOrKeyword, String city) {
        if (!text.contains(provinceOrKeyword)) {
            return;
        }
        List<RegionCode> matches;
        if (city != null) {
            matches = regionCodeRepository.findAll().stream()
                    .filter(region -> city.equals(region.getCity()))
                    .toList();
        } else {
            matches = regionCodeRepository.findAll().stream()
                    .filter(region -> region.getProvince().contains(provinceOrKeyword) || provinceOrKeyword.equals(region.getProvince()))
                    .toList();
        }
        target.addAll(matches);
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
