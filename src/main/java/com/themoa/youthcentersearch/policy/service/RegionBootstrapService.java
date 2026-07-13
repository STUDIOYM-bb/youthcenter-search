package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
        seed(List.of(
                new RegionSeed("KR", "전국", null, "NATIONWIDE"),
                new RegionSeed("41", "경기도", null, "PROVINCE"),
                new RegionSeed("41110", "경기도", "수원시", "CITY"),
                new RegionSeed("41111", "경기도", "수원시 장안구", "DISTRICT"),
                new RegionSeed("41113", "경기도", "수원시 권선구", "DISTRICT"),
                new RegionSeed("41115", "경기도", "수원시 팔달구", "DISTRICT"),
                new RegionSeed("41117", "경기도", "수원시 영통구", "DISTRICT"),
                new RegionSeed("41130", "경기도", "성남시", "CITY"),
                new RegionSeed("41150", "경기도", "의정부시", "CITY"),
                new RegionSeed("41170", "경기도", "안양시", "CITY"),
                new RegionSeed("41190", "경기도", "부천시", "CITY"),
                new RegionSeed("41210", "경기도", "광명시", "CITY"),
                new RegionSeed("41220", "경기도", "평택시", "CITY"),
                new RegionSeed("41250", "경기도", "동두천시", "CITY"),
                new RegionSeed("41270", "경기도", "안산시", "CITY"),
                new RegionSeed("41280", "경기도", "고양시", "CITY"),
                new RegionSeed("41290", "경기도", "과천시", "CITY"),
                new RegionSeed("41310", "경기도", "구리시", "CITY"),
                new RegionSeed("41360", "경기도", "남양주시", "CITY"),
                new RegionSeed("41370", "경기도", "오산시", "CITY"),
                new RegionSeed("41390", "경기도", "시흥시", "CITY"),
                new RegionSeed("41410", "경기도", "군포시", "CITY"),
                new RegionSeed("41430", "경기도", "의왕시", "CITY"),
                new RegionSeed("41450", "경기도", "하남시", "CITY"),
                new RegionSeed("41460", "경기도", "용인시", "CITY"),
                new RegionSeed("41480", "경기도", "파주시", "CITY"),
                new RegionSeed("41500", "경기도", "이천시", "CITY"),
                new RegionSeed("41550", "경기도", "안성시", "CITY"),
                new RegionSeed("41570", "경기도", "김포시", "CITY"),
                new RegionSeed("41590", "경기도", "화성시", "CITY"),
                new RegionSeed("41610", "경기도", "광주시", "CITY"),
                new RegionSeed("41630", "경기도", "양주시", "CITY"),
                new RegionSeed("41650", "경기도", "포천시", "CITY"),
                new RegionSeed("41670", "경기도", "여주시", "CITY"),
                new RegionSeed("41800", "경기도", "연천군", "CITY"),
                new RegionSeed("41820", "경기도", "가평군", "CITY"),
                new RegionSeed("41830", "경기도", "양평군", "CITY"),
                new RegionSeed("11", "서울특별시", null, "PROVINCE"),
                new RegionSeed("26", "부산광역시", null, "PROVINCE"),
                new RegionSeed("28", "인천광역시", null, "PROVINCE"),
                new RegionSeed("28177", "인천광역시", "미추홀구", "DISTRICT"),
                new RegionSeed("28200", "인천광역시", "남동구", "DISTRICT"),
                new RegionSeed("28237", "인천광역시", "부평구", "DISTRICT"),
                new RegionSeed("29", "광주광역시", null, "PROVINCE"),
                new RegionSeed("27", "대구광역시", null, "PROVINCE"),
                new RegionSeed("30", "대전광역시", null, "PROVINCE"),
                new RegionSeed("31", "울산광역시", null, "PROVINCE"),
                new RegionSeed("36", "세종특별자치시", null, "PROVINCE"),
                new RegionSeed("42", "강원특별자치도", null, "PROVINCE"),
                new RegionSeed("43", "충청북도", null, "PROVINCE"),
                new RegionSeed("44", "충청남도", null, "PROVINCE"),
                new RegionSeed("44210", "충청남도", "서산시", "CITY"),
                new RegionSeed("45", "전북특별자치도", null, "PROVINCE"),
                new RegionSeed("46", "전라남도", null, "PROVINCE"),
                new RegionSeed("47", "경상북도", null, "PROVINCE"),
                new RegionSeed("48", "경상남도", null, "PROVINCE"),
                new RegionSeed("48120", "경상남도", "창원시", "CITY"),
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
