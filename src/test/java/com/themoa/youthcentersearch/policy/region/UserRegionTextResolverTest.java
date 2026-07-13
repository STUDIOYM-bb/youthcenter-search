package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserRegionTextResolverTest {
    private final UserRegionTextResolver resolver = resolver();

    @Test
    void resolvesSyncedCountyAliases() {
        assertResolved("칠곡", "경상북도", "칠곡군");
        assertResolved("칠곡군", "경상북도", "칠곡군");
        assertResolved("경북 칠곡", "경상북도", "칠곡군");
        assertResolved("횡성", "강원특별자치도", "횡성군");
        assertResolved("예산", "충청남도", "예산군");
        assertResolved("해남", "전라남도", "해남군");
        assertResolved("합천", "경상남도", "합천군");
    }

    @Test
    void distinguishesAmbiguousShortNames() {
        assertThat(resolver.resolve("광주").status()).isEqualTo(UserRegionResolutionStatus.AMBIGUOUS);
        assertThat(resolver.resolve("광주광역시").province()).isEqualTo("광주광역시");
        var gyeonggiGwangju = resolver.resolve("경기도 광주");
        assertThat(gyeonggiGwangju.status()).isEqualTo(UserRegionResolutionStatus.EXACT);
        assertThat(gyeonggiGwangju.province()).isEqualTo("경기도");
        assertThat(gyeonggiGwangju.city()).isEqualTo("광주시");
    }

    private void assertResolved(String query, String province, String city) {
        var result = resolver.resolve(query);
        assertThat(result.resolved()).isTrue();
        assertThat(result.province()).isEqualTo(province);
        assertThat(result.city()).isEqualTo(city);
    }

    private UserRegionTextResolver resolver() {
        RegionCodeRepository repository = mock(RegionCodeRepository.class);
        List<RegionCode> regions = FakeRegionData.regions();
        when(repository.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repository.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
            when(repository.findByProvince(region.getProvince())).thenReturn(regions.stream()
                    .filter(candidate -> candidate.getProvince().equals(region.getProvince())).toList());
            if (region.getCity() != null) {
                when(repository.findByProvinceAndCity(region.getProvince(), region.getCity())).thenReturn(regions.stream()
                        .filter(candidate -> candidate.getProvince().equals(region.getProvince()) && region.getCity().equals(candidate.getCity()))
                        .toList());
            }
        }
        RegionAliasCatalog aliases = new RegionAliasCatalog();
        RegionNormalizer normalizer = new RegionNormalizer(aliases);
        return new UserRegionTextResolver(new RegionCatalog(repository, aliases, normalizer), aliases, normalizer);
    }
}
