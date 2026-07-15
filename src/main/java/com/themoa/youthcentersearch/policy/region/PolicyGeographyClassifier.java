package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class PolicyGeographyClassifier {
    private static final List<String> STRONG_NATIONWIDE_PATTERNS = List.of(
            "전국", "전국 대상", "전국 거주", "지역 제한 없음", "거주지 제한 없음",
            "전국에서 신청 가능", "국내 거주자", "국내 거주 청년"
    );
    private static final List<String> REGION_RESTRICTION_WORDS = List.of(
            "거주", "주민등록", "주소지", "소재", "관내", "지역 제한", "시민", "군민", "구민", "도민"
    );

    private final RegionCatalog catalog;
    private final StrictPolicyRegionMentionExtractor extractor;
    private final InstitutionRegionResolver institutionResolver;

    public PolicyGeographyClassifier(RegionCatalog catalog,
                                     StrictPolicyRegionMentionExtractor extractor,
                                     InstitutionRegionResolver institutionResolver) {
        this.catalog = catalog;
        this.extractor = extractor;
        this.institutionResolver = institutionResolver;
    }

    public PolicyRegionClassificationResult classify(Map<String, Object> fields) {
        List<PolicyRegionClassificationEvidence> evidence = new ArrayList<>();
        List<PolicyRegionClassificationEvidence> conflicts = new ArrayList<>();
        LinkedHashSet<RegionCode> strongRegions = new LinkedHashSet<>();
        LinkedHashSet<RegionCode> institutionRegions = new LinkedHashSet<>();

        boolean nationwideExpression = addNationwideEvidence(fields, evidence);
        boolean nationalInstitution = false;

        // 신청 대상/추가 자격은 policy_region에 저장 가능한 가장 강한 근거다.
        // 같은 문장에 기업 소재지나 활동 장소가 함께 있어도 role이 다르면 evidence로만 남긴다.
        addEligibilityRegions(strongRegions, evidence, RegionEvidenceSource.PARTICIPANT_TARGET,
                text(fields, "ptcpPrpTrgtCn", "conditionSummary"), 96, true);
        addEligibilityRegions(strongRegions, evidence, RegionEvidenceSource.ADDITIONAL_QUALIFICATION,
                text(fields, "addAplyQlfcCndCn"), 94, true);
        addTitleRegions(strongRegions, evidence, text(fields, "plcyNm", "title"));
        // zipCd는 온통청년 전용 코드 검증 체계가 확정되기 전까지 신청 지역으로 쓰지 않는다.
        addZipEvidence(fields, evidence);

        if (strongRegions.isEmpty()) {
            addEligibilityRegions(strongRegions, evidence, RegionEvidenceSource.SUPPORT_CONTENT,
                    text(fields, "plcySprtCn", "summary"), 62, true);
            addEligibilityRegions(strongRegions, evidence, RegionEvidenceSource.POLICY_DESCRIPTION,
                    text(fields, "plcyExplnCn"), 58, true);
        }

        InstitutionRegionResult supervising = institutionResolver.analyze(text(fields, "sprvsnInstCdNm", "agencyName"));
        InstitutionRegionResult operating = institutionResolver.analyze(text(fields, "operInstCdNm"));
        InstitutionRegionResult registering = institutionResolver.analyze(join(text(fields, "rgtrInstCdNm"),
                text(fields, "rgtrUpInstCdNm"), text(fields, "rgtrHghrkInstCdNm")));
        nationalInstitution = supervising.type() == InstitutionRegionType.NATIONAL_INSTITUTION
                || operating.type() == InstitutionRegionType.NATIONAL_INSTITUTION
                || registering.type() == InstitutionRegionType.NATIONAL_INSTITUTION;
        addInstitutionEvidence(institutionRegions, evidence, RegionEvidenceSource.SUPERVISING_INSTITUTION,
                text(fields, "sprvsnInstCdNm", "agencyName"), supervising);
        addInstitutionEvidence(institutionRegions, evidence, RegionEvidenceSource.OPERATING_INSTITUTION,
                text(fields, "operInstCdNm"), operating);
        addInstitutionEvidence(institutionRegions, evidence, RegionEvidenceSource.REGISTERING_INSTITUTION,
                join(text(fields, "rgtrInstCdNm"), text(fields, "rgtrUpInstCdNm"), text(fields, "rgtrHghrkInstCdNm")), registering);

        LinkedHashSet<RegionCode> selected = new LinkedHashSet<>(strongRegions);
        if (selected.isEmpty()) {
            selected.addAll(institutionRegions);
        } else if (!institutionRegions.isEmpty()) {
            Set<String> selectedProvinces = selected.stream().map(RegionCode::getProvince).collect(Collectors.toSet());
            institutionRegions.stream()
                    .filter(region -> !selectedProvinces.contains(region.getProvince()))
                    .forEach(region -> conflicts.add(new PolicyRegionClassificationEvidence(
                            RegionEvidenceSource.REGISTERING_INSTITUTION, region.displayName(),
                            region.displayName(), 45, "정책 대상 지역과 기관 지역이 충돌할 수 있음")));
        }

        if (selected.isEmpty() && (nationwideExpression || nationalInstitution && hasPolicyContent(fields)
                && !hasSpecificRegionRestriction(fields))) {
            return catalog.nationwide()
                    .map(region -> result(RegionScope.NATIONWIDE, Set.of(region), 0.86, evidence, conflicts, false))
                    .orElseGet(() -> result(RegionScope.UNKNOWN, Set.of(), 0.0, evidence, conflicts, true));
        }

        if (selected.isEmpty()) {
            return result(RegionScope.UNKNOWN, Set.of(), 0.0, evidence, conflicts, true);
        }
        LinkedHashSet<RegionCode> compact = logicalCompact(selected);
        RegionScope scope = scope(compact);
        double confidence = compact.size() == 1 ? 0.88 : 0.78;
        return result(scope, compact, confidence, evidence, conflicts, !conflicts.isEmpty());
    }

    private void addEligibilityRegions(Set<RegionCode> regions, List<PolicyRegionClassificationEvidence> evidence,
                                       RegionEvidenceSource source, String text, int confidence, boolean contextualShortAlias) {
        for (PolicyRegionMention mention : extractor.extractMentions(text, contextualShortAlias)) {
            evidence.add(new PolicyRegionClassificationEvidence(source, text, mention.region().displayName(),
                    Math.max(confidence, mention.confidence()), mention.reason(), mention.role()));
            if (mention.role() == PolicyRegionMentionRole.RESIDENCE_ELIGIBILITY
                    || mention.role() == PolicyRegionMentionRole.SERVICE_ELIGIBILITY) {
                regions.add(mention.region());
            }
        }
    }

    private void addTitleRegions(Set<RegionCode> regions, List<PolicyRegionClassificationEvidence> evidence, String title) {
        Set<RegionCode> found = extractor.extractFromTitle(title);
        found.forEach(region -> evidence.add(new PolicyRegionClassificationEvidence(
                RegionEvidenceSource.POLICY_TITLE, title, region.displayName(), 92,
                "정책 제목의 공식 행정구역명 또는 유일 시·군·구 별칭", PolicyRegionMentionRole.RESIDENCE_ELIGIBILITY)));
        regions.addAll(found);
    }

    private boolean addNationwideEvidence(Map<String, Object> fields, List<PolicyRegionClassificationEvidence> evidence) {
        String targetText = join(text(fields, "ptcpPrpTrgtCn", "conditionSummary"),
                text(fields, "addAplyQlfcCndCn"), text(fields, "plcyExplnCn"), text(fields, "plcySprtCn", "summary"));
        for (String pattern : STRONG_NATIONWIDE_PATTERNS) {
            if (StringUtils.hasText(targetText) && targetText.contains(pattern)) {
                evidence.add(new PolicyRegionClassificationEvidence(RegionEvidenceSource.NATIONWIDE_EXPRESSION,
                        targetText, "전국", 88, "명시적 전국/거주지 제한 없음 표현"));
                return true;
            }
        }
        return false;
    }

    private void addZipEvidence(Map<String, Object> fields, List<PolicyRegionClassificationEvidence> evidence) {
        String zipCd = text(fields, "zipCd");
        if (!StringUtils.hasText(zipCd)) {
            return;
        }
        Set<RegionCode> mapped = catalog.byZipCd(zipCd);
        if (mapped.isEmpty()) {
            evidence.add(new PolicyRegionClassificationEvidence(RegionEvidenceSource.ZIP_CODE,
                    zipCd, "", 0, "UNMAPPED_EXTERNAL_CODE", PolicyRegionMentionRole.REFERENCE_ONLY));
        } else {
            mapped.forEach(region -> evidence.add(new PolicyRegionClassificationEvidence(RegionEvidenceSource.ZIP_CODE,
                    zipCd, region.displayName(), 10, "zipCd는 공식 검증 전까지 신청 지역 생성 근거에서 제외", PolicyRegionMentionRole.REFERENCE_ONLY)));
        }
    }

    private void addInstitutionEvidence(Set<RegionCode> regions, List<PolicyRegionClassificationEvidence> evidence,
                                        RegionEvidenceSource source, String raw, InstitutionRegionResult result) {
        if (result.type() == InstitutionRegionType.NATIONAL_INSTITUTION) {
            evidence.add(new PolicyRegionClassificationEvidence(source, raw, "", 50, "전국/중앙 기관 후보",
                    PolicyRegionMentionRole.INSTITUTION_LOCATION));
        }
        Set<RegionCode> regionsFromInstitution = new LinkedHashSet<>(result.regions());
        regionsFromInstitution.addAll(extractor.extractFromInstitution(raw));
        regionsFromInstitution.forEach(region -> {
            evidence.add(new PolicyRegionClassificationEvidence(source, raw, region.displayName(), 54, "지역 공공기관명"));
            regions.add(region);
        });
    }

    private boolean hasSpecificRegionRestriction(Map<String, Object> fields) {
        String conditionText = join(text(fields, "ptcpPrpTrgtCn", "conditionSummary"),
                text(fields, "addAplyQlfcCndCn"));
        return REGION_RESTRICTION_WORDS.stream().anyMatch(conditionText::contains)
                && !extractor.extract(conditionText, true).isEmpty();
    }

    private boolean hasPolicyContent(Map<String, Object> fields) {
        return StringUtils.hasText(join(text(fields, "plcyNm", "title"), text(fields, "ptcpPrpTrgtCn", "conditionSummary"),
                text(fields, "addAplyQlfcCndCn"), text(fields, "plcyExplnCn"), text(fields, "plcySprtCn", "summary")));
    }

    private LinkedHashSet<RegionCode> logicalCompact(Set<RegionCode> regions) {
        return regions.stream()
                .sorted(Comparator.comparingInt((RegionCode region) -> region.displayName().length()).reversed())
                .collect(Collectors.toMap(this::logicalKey, region -> region, (left, right) -> left,
                        java.util.LinkedHashMap::new))
                .values().stream()
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private String logicalKey(RegionCode region) {
        return region.getProvince() + "|" + (region.getCity() == null ? "" : region.getCity());
    }

    private RegionScope scope(Set<RegionCode> regions) {
        if (regions.isEmpty()) return RegionScope.UNKNOWN;
        if (regions.stream().anyMatch(region -> "KR".equals(region.getRegionCode()))) return RegionScope.NATIONWIDE;
        if (regions.size() > 1) return RegionScope.MULTIPLE;
        String level = regions.iterator().next().getRegionLevel();
        if ("PROVINCE".equals(level)) return RegionScope.PROVINCE;
        if ("CITY".equals(level)) return RegionScope.CITY;
        if ("DISTRICT".equals(level)) return RegionScope.DISTRICT;
        return RegionScope.UNKNOWN;
    }

    private PolicyRegionClassificationResult result(RegionScope scope, Set<RegionCode> regions, double confidence,
                                                    List<PolicyRegionClassificationEvidence> evidence,
                                                    List<PolicyRegionClassificationEvidence> conflicts,
                                                    boolean needsReview) {
        return new PolicyRegionClassificationResult(scope, Set.copyOf(regions), confidence,
                List.copyOf(evidence), List.copyOf(conflicts), needsReview,
                PolicyRegionClassificationResult.VERSION);
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
