package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PolicyRegionResolver {
    private static final List<String> NATIONWIDE_PATTERNS = List.of(
            "전국 청년", "전국 거주자", "전국 단위", "대한민국 청년", "국내 거주 청년",
            "전국 거주 청년",
            "지역 제한 없음", "거주지 제한 없음", "전국에서 신청 가능"
    );
    private static final List<String> NEGATIVE_REGION_PATTERNS = List.of("거주자가 아니어도", "거주하지 않아도");

    private final RegionCatalog catalog;
    private final InstitutionRegionResolver institutionResolver;

    public PolicyRegionResolver(RegionCatalog catalog, InstitutionRegionResolver institutionResolver) {
        this.catalog = catalog;
        this.institutionResolver = institutionResolver;
    }

    public PolicyRegionResolution resolve(Map<String, Object> fields) {
        List<RegionEvidence> evidence = new ArrayList<>();

        Set<RegionCode> title = match(RegionEvidenceSource.POLICY_TITLE, text(fields, "plcyNm", "title"), 100, evidence);
        if (!title.isEmpty()) {
            return resolution(title, evidence, false);
        }

        Set<RegionCode> participantTarget = match(RegionEvidenceSource.PARTICIPANT_TARGET, text(fields, "ptcpPrpTrgtCn", "conditionSummary"), 95, evidence);
        if (!participantTarget.isEmpty()) {
            return resolution(participantTarget, evidence, false);
        }

        Set<RegionCode> qualification = match(RegionEvidenceSource.ADDITIONAL_QUALIFICATION, text(fields, "addAplyQlfcCndCn"), 92, evidence);
        if (!qualification.isEmpty()) {
            return resolution(qualification, evidence, false);
        }

        Set<RegionCode> supportContent = match(RegionEvidenceSource.SUPPORT_CONTENT, text(fields, "plcySprtCn", "summary"), 75, evidence);
        if (!supportContent.isEmpty()) {
            return resolution(supportContent, evidence, false);
        }

        Set<RegionCode> description = match(RegionEvidenceSource.POLICY_DESCRIPTION, text(fields, "plcyExplnCn"), 70, evidence);
        if (!description.isEmpty()) {
            return resolution(description, evidence, false);
        }

        Set<RegionCode> zipRegions = catalog.byZipCd(text(fields, "zipCd"));
        zipRegions.forEach(region -> evidence.add(new RegionEvidence(RegionEvidenceSource.ZIP_CODE,
                text(fields, "zipCd"), region.displayName(), 98)));
        if (!zipRegions.isEmpty()) {
            return resolution(zipRegions, evidence, false);
        }

        Set<RegionCode> institutions = new LinkedHashSet<>();
        institutions.addAll(institutionMatch(RegionEvidenceSource.SUPERVISING_INSTITUTION, text(fields, "sprvsnInstCdNm", "agencyName"), evidence));
        institutions.addAll(institutionMatch(RegionEvidenceSource.OPERATING_INSTITUTION, text(fields, "operInstCdNm"), evidence));
        institutions.addAll(institutionMatch(RegionEvidenceSource.REGISTERING_INSTITUTION,
                join(text(fields, "rgtrInstCdNm"), text(fields, "rgtrUpInstCdNm"), text(fields, "rgtrHghrkInstCdNm")), evidence));
        if (!institutions.isEmpty()) {
            return resolution(institutions, evidence, false);
        }

        String nationwideSource = nationwideSource(fields);
        if (StringUtils.hasText(nationwideSource)) {
            catalog.nationwide().ifPresent(region -> evidence.add(new RegionEvidence(
                    RegionEvidenceSource.NATIONWIDE_EXPRESSION, nationwideSource, region.displayName(), 80)));
            return catalog.nationwide()
                    .map(region -> resolution(Set.of(region), evidence, true))
                    .orElseGet(() -> PolicyRegionResolution.unknown(evidence));
        }

        return PolicyRegionResolution.unknown(evidence);
    }

    private Set<RegionCode> match(RegionEvidenceSource source, String value, int confidence, List<RegionEvidence> evidence) {
        if (!StringUtils.hasText(value) || hasNegativeRegionContext(value)) {
            return Set.of();
        }
        Set<RegionCode> regions = catalog.findInText(value);
        regions.forEach(region -> evidence.add(new RegionEvidence(source, value, region.displayName(), confidence)));
        return regions;
    }

    private Set<RegionCode> institutionMatch(RegionEvidenceSource source, String value, List<RegionEvidence> evidence) {
        Set<RegionCode> regions = institutionResolver.resolve(value);
        regions.forEach(region -> evidence.add(new RegionEvidence(source, value, region.displayName(), 55)));
        return regions;
    }

    private boolean hasNegativeRegionContext(String value) {
        return NEGATIVE_REGION_PATTERNS.stream().anyMatch(value::contains);
    }

    private String nationwideSource(Map<String, Object> fields) {
        String targetContext = join(text(fields, "ptcpPrpTrgtCn", "conditionSummary"),
                text(fields, "addAplyQlfcCndCn"), text(fields, "plcyExplnCn"));
        for (String pattern : NATIONWIDE_PATTERNS) {
            if (targetContext.contains(pattern)) {
                return targetContext;
            }
        }
        return null;
    }

    private PolicyRegionResolution resolution(Set<RegionCode> regions, List<RegionEvidence> evidence, boolean nationwide) {
        Set<RegionCode> filtered = regions.stream().collect(Collectors.toCollection(LinkedHashSet::new));
        RegionScope scope = scope(filtered);
        return new PolicyRegionResolution(scope,
                filtered.stream().map(RegionCode::getId).filter(java.util.Objects::nonNull).collect(Collectors.toCollection(LinkedHashSet::new)),
                filtered.stream().map(RegionCode::getRegionCode).collect(Collectors.toCollection(LinkedHashSet::new)),
                filtered.stream().map(RegionCode::displayName).collect(Collectors.toCollection(LinkedHashSet::new)),
                List.copyOf(evidence),
                nationwide,
                scope == RegionScope.UNKNOWN);
    }

    private RegionScope scope(Set<RegionCode> regions) {
        if (regions.isEmpty()) {
            return RegionScope.UNKNOWN;
        }
        if (regions.stream().anyMatch(region -> "KR".equals(region.getRegionCode()))) {
            return RegionScope.NATIONWIDE;
        }
        if (regions.size() > 1) {
            return RegionScope.MULTIPLE;
        }
        String level = regions.iterator().next().getRegionLevel();
        return switch (level) {
            case "PROVINCE" -> RegionScope.PROVINCE;
            case "DISTRICT" -> RegionScope.DISTRICT;
            case "CITY" -> RegionScope.CITY;
            default -> RegionScope.UNKNOWN;
        };
    }

    private String text(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            Object value = fields.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private String join(String... values) {
        return java.util.Arrays.stream(values).filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }
}
