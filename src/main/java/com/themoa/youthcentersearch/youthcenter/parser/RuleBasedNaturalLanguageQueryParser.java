package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.policy.region.UserRegionTextResolver;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageParseResult;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguagePolicyCondition;
import com.themoa.youthcentersearch.youthcenter.dto.response.ParserMode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedNaturalLanguageQueryParser implements NaturalLanguageQueryParser {
    private static final Pattern AGE_PATTERN = Pattern.compile("(\\d{1,2})\\s*(살|세)");
    private final UserRegionTextResolver userRegionTextResolver;

    public RuleBasedNaturalLanguageQueryParser(UserRegionTextResolver userRegionTextResolver) {
        this.userRegionTextResolver = userRegionTextResolver;
    }

    @Override
    public NaturalLanguageParseResult parse(String query) {
        String text = query == null ? "" : query.trim();
        Integer age = age(text);
        String region = region(text);
        String employment = employment(text);
        String student = student(text);
        String category = category(text);
        Set<String> keywords = new LinkedHashSet<>();
        List<String> supportTypes = new ArrayList<>();

        addIfContains(text, keywords, "면접수당", "면접수당", "면접", "취업");
        addIfContains(text, keywords, "면접비", "면접수당", "면접", "취업");
        addIfContains(text, keywords, "월세", "월세", "주거");
        addIfContains(text, keywords, "생활비", "생활비", "지원금");
        addIfContains(text, keywords, "교육비", "교육", "교육비");
        addIfContains(text, keywords, "지원금", "청년", "지원금");
        addIfContains(text, keywords, "수당", "청년", "수당");
        if ("UNEMPLOYED".equals(employment)) {
            keywords.add("취업");
            keywords.add("구직");
        }
        if (keywords.isEmpty()) {
            keywords.add("청년");
        }
        if (text.contains("지원금") || text.contains("수당") || text.contains("보조금")) {
            supportTypes.addAll(List.of("현금지원", "수당", "보조금"));
        }

        NaturalLanguagePolicyCondition condition = new NaturalLanguagePolicyCondition(region, age, employment, student,
                category, supportTypes, new ArrayList<>(keywords), null, null);
        return new NaturalLanguageParseResult(condition, ParserMode.RULE_BASED, false, null);
    }

    private Integer age(String text) {
        Matcher matcher = AGE_PATTERN.matcher(text);
        return matcher.find() ? Integer.parseInt(matcher.group(1)) : null;
    }

    private String region(String text) {
        var resolved = userRegionTextResolver.resolve(text);
        return resolved.resolved() ? resolved.regionName() : null;
    }

    private String employment(String text) {
        if (text.contains("무직") || text.contains("미취업") || text.contains("취준생") || text.contains("구직자")) {
            return "UNEMPLOYED";
        }
        if (text.contains("재직") || text.contains("직장인")) {
            return "EMPLOYED";
        }
        return null;
    }

    private String student(String text) {
        if (text.contains("대학생")) return "COLLEGE_STUDENT";
        if (text.contains("휴학생")) return "LEAVE_OF_ABSENCE";
        if (text.contains("졸업생")) return "GRADUATE";
        return null;
    }

    private String category(String text) {
        if (text.contains("월세") || text.contains("주거")) return "주거";
        if (text.contains("교육")) return "교육";
        if (text.contains("취업") || text.contains("면접") || text.contains("구직")) return "취업";
        if (text.contains("창업")) return "창업";
        return null;
    }

    private void addIfContains(String text, Set<String> keywords, String trigger, String... values) {
        if (text.contains(trigger)) {
            keywords.addAll(List.of(values));
        }
    }
}
