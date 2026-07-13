package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import com.themoa.youthcentersearch.policy.domain.PolicyRegion;
import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RegionMatchEvaluatorTest {
    private final RegionCodeRepository repository = repository();
    private final RegionAliasCatalog aliases = new RegionAliasCatalog();
    private final RegionNormalizer normalizer = new RegionNormalizer(aliases);
    private final RegionCatalog catalog = new RegionCatalog(repository, aliases, normalizer);
    private final RegionMatchEvaluator evaluator = new RegionMatchEvaluator(catalog, normalizer);

    @Test
    void evaluatesSuwonUserAgainstPolicyRegions() {
        ResolvedUserRegion user = evaluator.resolveUserRegion("경기도", "수원시", null);

        assertThat(evaluator.evaluate(policy(region("41110")), user).status()).isEqualTo(RegionMatchStatus.EXACT_CITY);
        assertThat(evaluator.evaluate(policy(region("41117")), user).status()).isEqualTo(RegionMatchStatus.EXACT_CITY);
        assertThat(evaluator.evaluate(policy(region("41")), user).status()).isEqualTo(RegionMatchStatus.PROVINCE_MATCH);
        assertThat(evaluator.evaluate(policy(region("KR")), user).status()).isEqualTo(RegionMatchStatus.NATIONWIDE);
        assertThat(evaluator.evaluate(policy(region("11")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("41130")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("41220")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("41410")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("41460")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("28177")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(region("28237")), user).status()).isEqualTo(RegionMatchStatus.NOT_MATCHED);
        assertThat(evaluator.evaluate(policy(), user).status()).isEqualTo(RegionMatchStatus.UNKNOWN);
    }

    private Policy policy(RegionCode... regions) {
        Policy policy = new Policy("P" + Math.random());
        policy.updateBasic("정책", "기관", PolicyCategory.일자리, "요약", null, null, null, true, true, "OPEN");
        for (RegionCode region : regions) {
            policy.getRegions().add(new PolicyRegion(policy, region));
        }
        return policy;
    }

    private RegionCode region(String code) {
        return FakeRegionData.regions().stream().filter(region -> code.equals(region.getRegionCode())).findFirst().orElseThrow();
    }

    private RegionCodeRepository repository() {
        RegionCodeRepository repo = mock(RegionCodeRepository.class);
        var regions = FakeRegionData.regions();
        when(repo.findAll()).thenReturn(regions);
        for (RegionCode region : regions) {
            when(repo.findByRegionCode(region.getRegionCode())).thenReturn(Optional.of(region));
        }
        return repo;
    }
}
