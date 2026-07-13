package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.domain.RegionExternalCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PolicyRegionResolverTest {
    private final RegionCodeRepository repository = repository();
    private final RegionExternalCodeRepository externalCodeRepository = externalCodeRepository();
    private final RegionAliasCatalog aliases = new RegionAliasCatalog();
    private final RegionNormalizer normalizer = new RegionNormalizer(aliases);
    private final RegionCatalog catalog = new RegionCatalog(repository, externalCodeRepository, aliases, normalizer);
    private final PolicyRegionResolver resolver = new PolicyRegionResolver(catalog, new InstitutionRegionResolver(catalog));

    @Test
    void noRegionEvidenceIsUnknown() {
        var result = resolver.resolve(Map.of("plcyNm", "청년 자립 지원"));

        assertThat(result.scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(result.regionCodes()).isEmpty();
    }

    @Test
    void explicitNationwideOnlyWhenTargetContextSaysSo() {
        assertThat(resolver.resolve(Map.of("ptcpPrpTrgtCn", "전국 거주 청년")).scope()).isEqualTo(RegionScope.NATIONWIDE);
        assertThat(resolver.resolve(Map.of("addAplyQlfcCndCn", "지역 제한 없음")).scope()).isEqualTo(RegionScope.NATIONWIDE);
    }

    @Test
    void extractsRegionFromPolicyTitle() {
        assertThat(resolver.resolve(Map.of("plcyNm", "서울시 청년수당")).regionNames()).contains("서울특별시");
        assertThat(resolver.resolve(Map.of("plcyNm", "경북 청년애꿈 수당")).regionNames()).contains("경상북도");
        assertThat(resolver.resolve(Map.of("plcyNm", "서산시 청년면접수당")).regionNames()).contains("충청남도 서산시");
        assertThat(resolver.resolve(Map.of("plcyNm", "미추홀구 청년 면접수당")).regionNames()).contains("인천광역시 미추홀구");
        assertThat(resolver.resolve(Map.of("plcyNm", "남동구 청년월세")).regionNames()).contains("인천광역시 남동구");
        assertThat(resolver.resolve(Map.of("plcyNm", "부평구 청년도전")).regionNames()).contains("인천광역시 부평구");
        assertThat(resolver.resolve(Map.of("plcyNm", "평택시 청년 월세 지원")).regionNames()).contains("경기도 평택시");
        assertThat(resolver.resolve(Map.of("plcyNm", "창원시 청년 지원")).regionNames()).contains("경상남도 창원시");
        assertThat(resolver.resolve(Map.of("plcyNm", "경기도 청년 면접수당")).regionNames()).contains("경기도");
        assertThat(resolver.resolve(Map.of("plcyNm", "제주도 청년 지원")).regionNames()).contains("제주특별자치도");
    }

    @Test
    void titleRegionWinsOverLowerPriorityContent() {
        var result = resolver.resolve(Map.of(
                "plcyNm", "미추홀구 청년 면접수당",
                "plcySprtCn", "경기도 사례를 참고한 청년 지원 사업"
        ));

        assertThat(result.regionNames()).containsExactly("인천광역시 미추홀구");
    }

    @Test
    void extractsInstitutionRegionButIgnoresCentralInstitution() {
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "수원시청")).regionNames()).contains("경기도 수원시");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "경기도일자리재단")).regionNames()).contains("경기도");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "서울특별시 미래청년기획관")).regionNames()).contains("서울특별시");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "제주특별자치도청")).regionNames()).contains("제주특별자치도");
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "고용노동부")).scope()).isEqualTo(RegionScope.UNKNOWN);
        assertThat(resolver.resolve(Map.of("sprvsnInstCdNm", "보건복지부")).scope()).isEqualTo(RegionScope.UNKNOWN);
    }

    @Test
    void parsesZipCd() {
        assertThat(resolver.resolve(Map.of("zipCd", "41111,41113,41115,41117")).regionNames())
                .containsExactly("경기도 수원시");
        assertThat(resolver.resolve(Map.of("zipCd", "not-a-code")).scope()).isEqualTo(RegionScope.UNKNOWN);
    }

    private RegionCodeRepository repository() {
        RegionCodeRepository repo = mock(RegionCodeRepository.class);
        var regions = FakeRegionData.regions();
        when(repo.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repo.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
            when(repo.findByProvince(region.getProvince())).thenReturn(regions.stream().filter(r -> r.getProvince().equals(region.getProvince())).toList());
            when(repo.findByProvinceAndCity(region.getProvince(), region.getCity())).thenReturn(regions.stream()
                    .filter(r -> r.getProvince().equals(region.getProvince()) && java.util.Objects.equals(r.getCity(), region.getCity())).toList());
        }
        return repo;
    }

    private RegionExternalCodeRepository externalCodeRepository() {
        RegionExternalCodeRepository repo = mock(RegionExternalCodeRepository.class);
        RegionCode suwon = FakeRegionData.regions().stream()
                .filter(region -> "경기도".equals(region.getProvince()) && "수원시".equals(region.getCity()))
                .findFirst()
                .orElseThrow();
        for (String zipCd : List.of("41111", "41113", "41115", "41117")) {
            when(repo.findByCodeSystemAndExternalCode("YOUTH_CENTER_ZIP", zipCd))
                    .thenReturn(Optional.of(new RegionExternalCode(suwon, "YOUTH_CENTER_ZIP", zipCd)));
        }
        return repo;
    }
}
