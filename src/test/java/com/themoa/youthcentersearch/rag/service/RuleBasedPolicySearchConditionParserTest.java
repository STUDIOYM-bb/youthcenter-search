package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.FakeRegionData;
import com.themoa.youthcentersearch.policy.region.RegionAliasCatalog;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.region.RegionNormalizer;
import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleBasedPolicySearchConditionParserTest {
    private final RuleBasedPolicySearchConditionParser parser = new RuleBasedPolicySearchConditionParser(resolver());

    @Test
    void extractsRegionAgeEmploymentAndKeywords() {
        var condition = parser.parseCondition("수원 사는 27살 무직 청년 지원금", 20);

        assertThat(condition.province()).isEqualTo("경기도");
        assertThat(condition.city()).isEqualTo("수원시");
        assertThat(condition.age()).isEqualTo(27);
        assertThat(condition.employmentStatus()).isEqualTo("UNEMPLOYED");
        assertThat(condition.keywords()).contains("청년", "지원금");
        assertThat(condition.keywords()).doesNotContain("취업", "구직");
    }

    @Test
    void extractsJejuProvince() {
        var condition = parser.parseCondition("제주도 청년 월세 지원", 10);

        assertThat(condition.province()).isEqualTo("제주특별자치도");
        assertThat(condition.city()).isNull();
        assertThat(condition.category()).isEqualTo("주거");
    }

    @Test
    void extractsSyncedCountyFromDynamicCatalog() {
        var condition = parser.parseCondition("칠곡에 살고 있는 30살 청년이 받을 수 있는 취업 관련 정책", 10);

        assertThat(condition.province()).isEqualTo("경상북도");
        assertThat(condition.city()).isEqualTo("칠곡군");
        assertThat(condition.age()).isEqualTo(30);
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
