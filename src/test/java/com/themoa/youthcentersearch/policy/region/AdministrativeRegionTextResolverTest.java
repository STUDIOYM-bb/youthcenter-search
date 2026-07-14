package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AdministrativeRegionTextResolverTest {
    private final UserRegionTextResolver resolver = resolver();

    @Test
    void resolvesSidoOfficialNamesAndGeneratedAliasesAsSido() {
        assertSido("서울특별시", "서울특별시");
        assertSido("서울시", "서울특별시");
        assertSido("서울", "서울특별시");
        assertSido("부산광역시", "부산광역시");
        assertSido("부산시", "부산광역시");
        assertSido("부산", "부산광역시");
        assertSido("세종특별자치시", "세종특별자치시");
        assertSido("세종시", "세종특별자치시");
        assertSido("경기도", "경기도");
        assertSido("경기", "경기도");
        assertSido("제주특별자치도", "제주특별자치도");
        assertSido("제주도", "제주특별자치도");
        assertSido("제주", "제주특별자치도");
    }

    @Test
    void doesNotResolveSidoAliasAsChildDistrict() {
        var result = resolver.resolve("서울에 사는 청년");

        assertThat(result.resolved()).isTrue();
        assertThat(result.regionLevel()).isEqualTo(SearchRegionLevel.SIDO);
        assertThat(result.province()).isEqualTo("서울특별시");
        assertThat(result.city()).isNull();
    }

    @Test
    void resolvesNationwideExpressionAsNationwideScope() {
        var result = resolver.resolve("전국 청년 지원 정책");

        assertThat(result.resolved()).isTrue();
        assertThat(result.regionLevel()).isEqualTo(SearchRegionLevel.NATIONWIDE);
        assertThat(result.province()).isEqualTo("전국");
        assertThat(result.city()).isNull();
    }

    @Test
    void resolvesSigunguByOfficialPathAndGeneratedAliases() {
        assertSigungu("서울특별시 강남구", "서울특별시", "강남구");
        assertSigungu("서울 강남", "서울특별시", "강남구");
        assertSigungu("부산 해운대", "부산광역시", "해운대구");
        assertSigungu("부산 기장", "부산광역시", "기장군");
        assertSigungu("경기도 수원시", "경기도", "수원시");
        assertSigungu("경기 수원", "경기도", "수원시");
        assertSigungu("전남 해남", "전라남도", "해남군");
        assertSigungu("제주 제주시", "제주특별자치도", "제주시");
    }

    @Test
    void normalizesGeneralCityDistrictToParentCity() {
        var result = resolver.resolve("영통구에 사는 청년");

        assertThat(result.resolved()).isTrue();
        assertThat(result.regionLevel()).isEqualTo(SearchRegionLevel.SIGUNGU);
        assertThat(result.province()).isEqualTo("경기도");
        assertThat(result.city()).isEqualTo("수원시");
    }

    @Test
    void keepsMetropolitanAutonomousDistrictAsSigungu() {
        var result = resolver.resolve("강남구 청년");

        assertThat(result.resolved()).isTrue();
        assertThat(result.regionLevel()).isEqualTo(SearchRegionLevel.SIGUNGU);
        assertThat(result.province()).isEqualTo("서울특별시");
        assertThat(result.city()).isEqualTo("강남구");
    }

    @Test
    void officialNameWinsOverGeneratedAliasAndAmbiguousAliasIsKept() {
        assertThat(resolver.resolve("광주").status()).isEqualTo(UserRegionResolutionStatus.AMBIGUOUS);
        assertSido("광주광역시", "광주광역시");
        assertSigungu("광주시", "경기도", "광주시");
        assertSigungu("경기도 광주", "경기도", "광주시");
    }

    @Test
    void oneLetterAliasesAreNotGenerated() {
        var result = resolver.resolve("남");

        assertThat(result.status()).isEqualTo(UserRegionResolutionStatus.NOT_FOUND);
    }

    @Test
    void everyFixtureSidoOfficialNameResolvesToSido() {
        regions().stream()
                .filter(region -> "PROVINCE".equals(region.getRegionLevel()))
                .forEach(region -> assertSido(region.getProvince(), region.getProvince()));
    }

    @Test
    void everyFixtureSigunguOfficialPathResolvesToSigungu() {
        regions().stream()
                .filter(region -> "CITY".equals(region.getRegionLevel()))
                .forEach(region -> assertSigungu(region.displayName(), region.getProvince(), region.getCity()));
    }

    private void assertSido(String query, String province) {
        var result = resolver.resolve(query);
        assertThat(result.resolved()).as(query + " -> " + result).isTrue();
        assertThat(result.regionLevel()).as(query).isEqualTo(SearchRegionLevel.SIDO);
        assertThat(result.province()).as(query).isEqualTo(province);
        assertThat(result.city()).as(query).isNull();
    }

    private void assertSigungu(String query, String province, String city) {
        var result = resolver.resolve(query);
        assertThat(result.resolved()).as(query + " -> " + result).isTrue();
        assertThat(result.regionLevel()).as(query).isEqualTo(SearchRegionLevel.SIGUNGU);
        assertThat(result.province()).as(query).isEqualTo(province);
        assertThat(result.city()).as(query).isEqualTo(city);
        assertThat(result.district()).as(query).isNull();
    }

    private UserRegionTextResolver resolver() {
        RegionCodeRepository repository = mock(RegionCodeRepository.class);
        List<RegionCode> regions = regions();
        when(repository.findAll()).thenReturn(regions);
        when(repository.findByRegionCode("KR")).thenReturn(java.util.Optional.of(new RegionCode(null, "KR", "전국", null, "NATIONWIDE")));
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        return new UserRegionTextResolver(new RegionCatalog(repository, aliases, normalizer),
                new RegionNameAliasGenerator(), normalizer);
    }

    private List<RegionCode> regions() {
        RegionCode seoul = sido("P:서울특별시", "서울특별시");
        RegionCode busan = sido("P:부산광역시", "부산광역시");
        RegionCode sejong = sido("P:세종특별자치시", "세종특별자치시");
        RegionCode gyeonggi = sido("P:경기도", "경기도");
        RegionCode jeonnam = sido("P:전라남도", "전라남도");
        RegionCode jeju = sido("P:제주특별자치도", "제주특별자치도");
        RegionCode gwangju = sido("P:광주광역시", "광주광역시");
        return List.of(
                seoul,
                sigungu(seoul, "M:서울특별시:중구", "중구"),
                sigungu(seoul, "M:서울특별시:강남구", "강남구"),
                busan,
                sigungu(busan, "M:부산광역시:해운대구", "해운대구"),
                sigungu(busan, "M:부산광역시:기장군", "기장군"),
                sigungu(busan, "M:부산광역시:남구", "남구"),
                sejong,
                gyeonggi,
                sigungu(gyeonggi, "M:경기도:수원시", "수원시"),
                sigungu(gyeonggi, "M:경기도:광주시", "광주시"),
                new RegionCode(gyeonggi, "D:경기도:수원시 영통구", "경기도", "수원시 영통구", "DISTRICT"),
                jeonnam,
                sigungu(jeonnam, "M:전라남도:해남군", "해남군"),
                jeju,
                sigungu(jeju, "M:제주특별자치도:제주시", "제주시"),
                gwangju,
                sigungu(gwangju, "M:광주광역시:남구", "남구")
        );
    }

    private RegionCode sido(String code, String province) {
        return new RegionCode(null, code, province, null, "PROVINCE");
    }

    private RegionCode sigungu(RegionCode sido, String code, String city) {
        return new RegionCode(sido, code, sido.getProvince(), city, "CITY");
    }
}
