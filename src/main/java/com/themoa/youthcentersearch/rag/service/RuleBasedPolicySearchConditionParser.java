package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedPolicySearchConditionParser {
    private static final Pattern AGE = Pattern.compile("(\\d{1,2})\\s*(살|세)");
    private final UserRegionTextResolver userRegionTextResolver;
    private final UserEmploymentStatusDetector employmentStatusDetector;
    private final PolicyIntentPolarityDetector polarityDetector = new PolicyIntentPolarityDetector();

    public RuleBasedPolicySearchConditionParser(UserRegionTextResolver userRegionTextResolver,
                                                UserEmploymentStatusDetector employmentStatusDetector) {
        this.userRegionTextResolver = userRegionTextResolver;
        this.employmentStatusDetector = employmentStatusDetector;
    }

    public PolicySearchCondition parseCondition(String query, Integer resultSize) {
        String text = query == null ? "" : query;
        Set<String> keywords = new LinkedHashSet<>();
        Set<String> supportTypes = new LinkedHashSet<>();
        String province = null;
        String city = null;
        String rawRegionText = null;
        String regionResolutionStatus = null;
        var region = userRegionTextResolver.resolve(text);
        if (region.resolved()) {
            province = region.province();
            city = region.city();
            rawRegionText = region.regionName();
            regionResolutionStatus = region.status().name();
        } else if (region.status().name().equals("AMBIGUOUS")) {
            regionResolutionStatus = region.status().name();
        }
        Integer age = null;
        Matcher matcher = AGE.matcher(text);
        if (matcher.find()) {
            age = Integer.parseInt(matcher.group(1));
        }
        var detectedEmployment = employmentStatusDetector.detect(text);
        String employment = detectedEmployment.explicit() ? detectedEmployment.status().name() : null;
        Boolean student = null;
        if (containsAny(text, "대학생", "재학생", "휴학생")) {
            student = true;
        }
        String category = null;
        var polarity = polarityDetector.detect(text);
        if (containsAny(text, "월세", "주거")) category = "주거";
        if (polarity.desiredDomains().contains(com.themoa.youthcentersearch.rag.dto.SearchDomain.EMPLOYMENT)) category = "일자리";
        if (containsAny(text, "교육", "훈련")) category = "교육";
        if (containsAny(text, "자산", "저축", "계좌", "통장", "지원금", "수당")) category = "금융";
        add(text, keywords, "청년", "청년");
        add(text, keywords, "지원금", "지원금", "청년");
        add(text, keywords, "수당", "수당", "청년");
        add(text, keywords, "보조금", "보조금", "청년");
        if (polarity.positiveTerms().contains("면접")) {
            keywords.add("면접");
        }
        add(text, keywords, "월세", "월세", "주거");
        add(text, keywords, "생활비", "생활비");
        add(text, keywords, "자산", "자산형성", "저축");
        add(text, keywords, "계좌", "계좌", "저축", "자산형성");
        add(text, keywords, "통장", "통장", "저축", "자산형성");
        add(text, keywords, "저축", "저축", "계좌", "통장", "자산형성");
        if (containsAny(text, "지원금", "수당", "보조금")) {
            supportTypes.add("CASH");
            supportTypes.add("ALLOWANCE");
            supportTypes.add("SUBSIDY");
        }
        if (keywords.isEmpty()) {
            keywords.add("청년");
        }
        return new PolicySearchCondition(province, city, null, age, employment, student, null, category,
                supportTypes, keywords, Set.of(), rawRegionText, regionResolutionStatus,
                region.regionLevel() == null ? null : region.regionLevel().name(),
                Set.copyOf(region.candidates()),
                false, false, false, false, false, false, null, resultSize);
    }

    private void add(String text, Set<String> target, String trigger, String... values) {
        if (text.contains(trigger)) {
            target.addAll(Set.of(values));
        }
    }

    private boolean containsAny(String text, String... terms) {
        for (String term : terms) {
            if (text.contains(term)) {
                return true;
            }
        }
        return false;
    }
}
