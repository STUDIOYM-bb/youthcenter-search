package com.themoa.youthcentersearch.policy.region;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

@Component
public class StrictPolicyRegionMentionExtractor {
    private static final List<String> CONTEXT_AFTER_ALIAS = List.of(
            "거주", "주민등록", "주소지", "소재지", "시민", "군민", "구민", "도민",
            "생활권"
    );
    private static final List<String> CONTEXT_BEFORE_ALIAS = List.of(
            "거주", "주민등록", "주소지", "소재지", "관내", "해당지역", "지역"
    );
    private static final List<String> RESIDENCE_ELIGIBILITY_HINTS = List.of(
            "거주", "주민등록", "주소지", "관내", "시민", "군민", "구민", "도민", "신청대상", "지원대상"
    );
    private static final List<String> SERVICE_ELIGIBILITY_HINTS = List.of("서비스지역", "지원지역", "사업지역", "관할");
    private static final List<String> WORKPLACE_HINTS = List.of("소재기업", "면접기업", "회사소재지", "사업장", "근무지", "기업소재");
    private static final List<String> ACTIVITY_HINTS = List.of("활동장소", "교육장소", "방문기관", "행사장소");

    private final RegionCatalog catalog;
    private final RegionNameAliasGenerator aliasGenerator;
    private final RegionNormalizer normalizer;

    public StrictPolicyRegionMentionExtractor(RegionCatalog catalog,
                                              RegionNameAliasGenerator aliasGenerator,
                                              RegionNormalizer normalizer) {
        this.catalog = catalog;
        this.aliasGenerator = aliasGenerator;
        this.normalizer = normalizer;
    }

    public Set<RegionCode> extract(String text, boolean allowContextualShortAlias) {
        return extractMentions(text, allowContextualShortAlias).stream()
                .filter(mention -> mention.role() == PolicyRegionMentionRole.RESIDENCE_ELIGIBILITY
                        || mention.role() == PolicyRegionMentionRole.SERVICE_ELIGIBILITY)
                .map(PolicyRegionMention::region)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    public Set<PolicyRegionMention> extractMentions(String text, boolean allowContextualShortAlias) {
        Set<RegionCode> matches = new LinkedHashSet<>();
        Set<PolicyRegionMention> mentions = new LinkedHashSet<>();
        if (!StringUtils.hasText(text)) {
            return mentions;
        }
        String compact = normalizer.compact(text);
        for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
            if (!isSupported(region)) {
                continue;
            }
            int index = mentionIndex(compact, region, allowContextualShortAlias);
            if (index >= 0) {
                mentions.add(new PolicyRegionMention(region, role(compact, index, region), text, confidence(compact, index, region),
                        "지역 표현 주변 문맥으로 신청 자격/위치 역할을 분리"));
            }
        }
        return mentions;
    }

    public Set<RegionCode> extractFromTitle(String title) {
        Set<RegionCode> matches = extractMentions(title, false).stream()
                .map(PolicyRegionMention::region)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        if (!StringUtils.hasText(title)) {
            return matches;
        }
        String compactTitle = normalizer.compact(title);
        for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
            if (!"CITY".equals(region.getRegionLevel()) && !"DISTRICT".equals(region.getRegionLevel())) {
                continue;
            }
            String shortAlias = aliasGenerator.shortAlias(region.getCity());
            if (!StringUtils.hasText(shortAlias) || shortAlias.length() < 2) {
                continue;
            }
            if (catalog.uniqueSigunguByShortAlias(shortAlias).filter(unique -> sameRegion(unique, region)).isEmpty()) {
                continue;
            }
            String compactAlias = normalizer.compact(shortAlias);
            if (compactTitle.startsWith(compactAlias)
                    || bracketedAlias(title, shortAlias)
                    || independentToken(title, shortAlias)) {
                matches.add(region);
            }
        }
        return matches;
    }

    public Set<RegionCode> extractFromInstitution(String institution) {
        Set<RegionCode> matches = extract(institution, false);
        if (!StringUtils.hasText(institution)) {
            return matches;
        }
        String compactInstitution = normalizer.compact(institution);
        for (RegionCode region : catalog.allSpecificRegionsByLongestName()) {
            if (!"CITY".equals(region.getRegionLevel()) && !"DISTRICT".equals(region.getRegionLevel())) {
                continue;
            }
            String shortAlias = aliasGenerator.shortAlias(region.getCity());
            if (!StringUtils.hasText(shortAlias) || shortAlias.length() < 2) {
                continue;
            }
            if (catalog.uniqueSigunguByShortAlias(shortAlias).filter(unique -> sameRegion(unique, region)).isEmpty()) {
                continue;
            }
            String alias = normalizer.compact(shortAlias);
            if (compactInstitution.startsWith(alias)
                    && compactInstitution.length() > alias.length()
                    && institutionSuffix(compactInstitution.substring(alias.length()))) {
                matches.add(region);
            }
        }
        return matches;
    }

    private boolean officialOrFullPathMention(String compact, RegionCode region) {
        if ("PROVINCE".equals(region.getRegionLevel())) {
            return aliasGenerator.aliasesForSido(region).stream()
                    .map(normalizer::compact)
                    .filter(StringUtils::hasText)
                    .anyMatch(compact::contains);
        }
        String city = region.getCity();
        if (!StringUtils.hasText(city)) {
            return false;
        }
        String official = normalizer.compact(city);
        String full = normalizer.compact(region.getProvince() + " " + city);
        return compact.contains(full) || compact.contains(official);
    }

    private int mentionIndex(String compact, RegionCode region, boolean allowContextualShortAlias) {
        List<String> aliases = new java.util.ArrayList<>();
        if ("PROVINCE".equals(region.getRegionLevel())) {
            aliasGenerator.aliasesForSido(region).stream().map(normalizer::compact).forEach(aliases::add);
        } else if (StringUtils.hasText(region.getCity())) {
            aliases.add(normalizer.compact(region.getProvince() + " " + region.getCity()));
            aliases.add(normalizer.compact(region.getCity()));
            if (allowContextualShortAlias) {
                aliases.add(normalizer.compact(aliasGenerator.shortAlias(region.getCity())));
            }
        }
        return aliases.stream()
                .filter(StringUtils::hasText)
                .mapToInt(compact::indexOf)
                .filter(index -> index >= 0)
                .min()
                .orElse(-1);
    }

    private PolicyRegionMentionRole role(String compact, int index, RegionCode region) {
        String window = window(compact, index, region);
        if (WORKPLACE_HINTS.stream().anyMatch(window::contains)) return PolicyRegionMentionRole.WORKPLACE_LOCATION;
        if (ACTIVITY_HINTS.stream().anyMatch(window::contains)) return PolicyRegionMentionRole.ACTIVITY_LOCATION;
        if (SERVICE_ELIGIBILITY_HINTS.stream().anyMatch(window::contains)) return PolicyRegionMentionRole.SERVICE_ELIGIBILITY;
        if (RESIDENCE_ELIGIBILITY_HINTS.stream().anyMatch(window::contains)) return PolicyRegionMentionRole.RESIDENCE_ELIGIBILITY;
        return PolicyRegionMentionRole.REFERENCE_ONLY;
    }

    private int confidence(String compact, int index, RegionCode region) {
        return switch (role(compact, index, region)) {
            case RESIDENCE_ELIGIBILITY, SERVICE_ELIGIBILITY -> 94;
            case WORKPLACE_LOCATION, ACTIVITY_LOCATION -> 55;
            case INSTITUTION_LOCATION -> 45;
            default -> 35;
        };
    }

    private String window(String compact, int index, RegionCode region) {
        int end = Math.min(compact.length(), index + Math.max(4, normalizer.compact(region.displayName()).length()));
        return compact.substring(Math.max(0, index - 14), Math.min(compact.length(), end + 18));
    }

    private boolean contextualShortMention(String compact, RegionCode region, String originalText, boolean allowContextualShortAlias) {
        if (!allowContextualShortAlias || !"CITY".equals(region.getRegionLevel()) && !"DISTRICT".equals(region.getRegionLevel())) {
            return false;
        }
        String city = region.getCity();
        if (!StringUtils.hasText(city)) {
            return false;
        }
        String shortAlias = aliasGenerator.shortAlias(city);
        if (!StringUtils.hasText(shortAlias) || shortAlias.length() < 2) {
            return false;
        }
        String normalizedAlias = normalizer.compact(shortAlias);
        int index = compact.indexOf(normalizedAlias);
        while (index >= 0) {
            if (hasAdjacentRegionContext(compact, index, normalizedAlias.length())) {
                return true;
            }
            index = compact.indexOf(normalizedAlias, index + normalizedAlias.length());
        }
        return false;
    }

    private boolean hasAdjacentRegionContext(String text, int aliasIndex, int aliasLength) {
        if (!StringUtils.hasText(text) || aliasIndex < 0 || aliasIndex >= text.length()) {
            return false;
        }
        int aliasEnd = Math.min(text.length(), aliasIndex + aliasLength);
        String after = text.substring(aliasEnd, Math.min(text.length(), aliasEnd + 8));
        String before = text.substring(Math.max(0, aliasIndex - 8), aliasIndex);
        return CONTEXT_AFTER_ALIAS.stream().anyMatch(after::startsWith)
                || CONTEXT_BEFORE_ALIAS.stream().anyMatch(before::endsWith);
    }

    private boolean bracketedAlias(String text, String alias) {
        String pattern = "[\\(\\[\\{]\\s*" + Pattern.quote(alias) + "\\s*[\\)\\]\\}]";
        return Pattern.compile(pattern).matcher(text).find();
    }

    private boolean independentToken(String text, String alias) {
        String pattern = "(^|[^가-힣A-Za-z0-9])" + Pattern.quote(alias) + "([^가-힣A-Za-z0-9]|$)";
        return Pattern.compile(pattern).matcher(text).find();
    }

    private boolean institutionSuffix(String suffix) {
        return suffix.startsWith("시청")
                || suffix.startsWith("군청")
                || suffix.startsWith("구청")
                || suffix.startsWith("청년센터")
                || suffix.startsWith("일자리센터")
                || suffix.startsWith("경제진흥원")
                || suffix.startsWith("문화재단")
                || suffix.startsWith("복지재단")
                || suffix.startsWith("상공회의소");
    }

    private boolean sameRegion(RegionCode left, RegionCode right) {
        if (left == null || right == null) {
            return false;
        }
        if (left.getId() != null && right.getId() != null) {
            return left.getId().equals(right.getId());
        }
        if (StringUtils.hasText(left.getRegionCode()) && StringUtils.hasText(right.getRegionCode())) {
            return left.getRegionCode().equals(right.getRegionCode());
        }
        return left.displayName().equals(right.displayName());
    }

    private boolean isSupported(RegionCode region) {
        return "PROVINCE".equals(region.getRegionLevel())
                || "CITY".equals(region.getRegionLevel())
                || "DISTRICT".equals(region.getRegionLevel());
    }
}
