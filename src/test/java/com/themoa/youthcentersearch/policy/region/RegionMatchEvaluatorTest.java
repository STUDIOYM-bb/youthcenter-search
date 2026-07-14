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
    void sigunguSearchAllowsExactParentSidoNationwideAndMatchingMultipleOnly() {
        ResolvedUserRegion user = evaluator.resolveUserRegion("경기도", "수원시", null);

        assertMatch(policy(region("41110")), user, RegionCompatibility.EXACT_SIGUNGU, true, 100);
        assertMatch(policy(region("41")), user, RegionCompatibility.PARENT_SIDO, true, 100);
        assertMatch(policy(region("KR")), user, RegionCompatibility.NATIONWIDE, true, 100);
        assertMatch(policy(region("41110"), region("41130")), user, RegionCompatibility.MULTIPLE_REGION_MATCH, true, 100);
        assertMatch(policy(region("41"), region("11")), user, RegionCompatibility.MULTIPLE_REGION_MATCH, true, 100);

        assertMatch(policy(region("41130")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("11")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("28177")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(), user, RegionCompatibility.UNKNOWN, false, 0);
    }

    @Test
    void sidoSearchAllowsSidoNationwideAndMatchingMultipleButNotChildOnlyPolicies() {
        ResolvedUserRegion user = evaluator.resolveUserRegion("경기도", null, null);

        assertMatch(policy(region("41")), user, RegionCompatibility.EXACT_SIDO, true, 100);
        assertMatch(policy(region("KR")), user, RegionCompatibility.NATIONWIDE, true, 100);
        assertMatch(policy(region("41"), region("11")), user, RegionCompatibility.MULTIPLE_REGION_MATCH, true, 100);

        assertMatch(policy(region("41110")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("41130")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("11")), user, RegionCompatibility.NOT_MATCHED, false, 0);
    }

    @Test
    void metropolitanAutonomousDistrictSearchIncludesParentMetropolitanSidoAndNationwide() {
        ResolvedUserRegion user = evaluator.resolveUserRegion("인천광역시", "부평구", null);

        assertMatch(policy(region("28237")), user, RegionCompatibility.EXACT_SIGUNGU, true, 100);
        assertMatch(policy(region("28")), user, RegionCompatibility.PARENT_SIDO, true, 100);
        assertMatch(policy(region("KR")), user, RegionCompatibility.NATIONWIDE, true, 100);

        assertMatch(policy(region("28200")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("26")), user, RegionCompatibility.NOT_MATCHED, false, 0);
    }

    @Test
    void explicitNationwideSearchAllowsOnlyNationwidePolicies() {
        ResolvedUserRegion user = evaluator.resolveUserRegion("전국", null, null, SearchRegionLevel.NATIONWIDE.name());

        assertMatch(policy(region("KR")), user, RegionCompatibility.NATIONWIDE, true, 100);
        assertMatch(policy(region("41")), user, RegionCompatibility.NOT_MATCHED, false, 0);
        assertMatch(policy(region("41110")), user, RegionCompatibility.NOT_MATCHED, false, 0);
    }

    @Test
    void noRegionSearchDoesNotApplyRegionEligibility() {
        ResolvedUserRegion user = evaluator.resolveUserRegion(null, null, null);

        RegionMatchResult result = evaluator.evaluate(policy(region("41110")), user);

        assertThat(result.compatibility()).isEqualTo(RegionCompatibility.UNKNOWN);
        assertThat(result.eligible()).isTrue();
        assertThat(result.score()).isZero();
    }

    private void assertMatch(Policy policy, ResolvedUserRegion user, RegionCompatibility compatibility,
                             boolean eligible, int score) {
        RegionMatchResult result = evaluator.evaluate(policy, user);

        assertThat(result.compatibility()).isEqualTo(compatibility);
        assertThat(result.eligible()).isEqualTo(eligible);
        assertThat(result.score()).isEqualTo(score);
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
