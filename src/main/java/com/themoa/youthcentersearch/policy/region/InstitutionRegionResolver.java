package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;

@Component
public class InstitutionRegionResolver {
    private static final Set<String> CENTRAL_INSTITUTIONS = Set.of(
            "고용노동부", "보건복지부", "중소벤처기업부", "국토교통부", "교육부", "금융위원회",
            "한국장학재단", "서민금융진흥원", "한국고용정보원", "중앙행정기관", "공공기관"
    );

    private final RegionCatalog catalog;

    public InstitutionRegionResolver(RegionCatalog catalog) {
        this.catalog = catalog;
    }

    public Set<RegionCode> resolve(String institution) {
        return analyze(institution).regions();
    }

    public InstitutionRegionResult analyze(String institution) {
        Set<RegionCode> result = new LinkedHashSet<>();
        if (institution == null || institution.isBlank()) {
            return new InstitutionRegionResult(InstitutionRegionType.UNKNOWN_INSTITUTION, result);
        }
        if (isCentralInstitution(institution)) {
            return new InstitutionRegionResult(InstitutionRegionType.NATIONAL_INSTITUTION, result);
        }
        result.addAll(catalog.findInText(institution));
        if (!result.isEmpty()) {
            return new InstitutionRegionResult(InstitutionRegionType.LOCAL_INSTITUTION, result);
        }
        return new InstitutionRegionResult(InstitutionRegionType.UNKNOWN_INSTITUTION, result);
    }

    public boolean isCentralInstitution(String institution) {
        if (institution == null) {
            return false;
        }
        return CENTRAL_INSTITUTIONS.stream().anyMatch(institution::contains);
    }

    public Set<String> centralInstitutions() {
        return CENTRAL_INSTITUTIONS;
    }
}
