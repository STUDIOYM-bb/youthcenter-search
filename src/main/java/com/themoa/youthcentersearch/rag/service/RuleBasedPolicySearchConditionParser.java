package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import org.springframework.stereotype.Component;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedPolicySearchConditionParser {
    private static final Pattern AGE = Pattern.compile("(\\d{1,2})\\s*(살|세)");

    public PolicySearchCondition parseCondition(String query, Integer resultSize) {
        String text = query == null ? "" : query;
        Set<String> keywords = new LinkedHashSet<>();
        Set<String> supportTypes = new LinkedHashSet<>();
        String province = null;
        String city = null;
        if (text.contains("수원")) {
            province = "경기도";
            city = "수원시";
        } else if (text.contains("제주도") || text.contains("제주")) {
            province = "제주특별자치도";
        } else if (text.contains("경기도") || text.contains("경기")) {
            province = "경기도";
        } else if (text.contains("서울")) {
            province = "서울특별시";
        }
        Integer age = null;
        Matcher matcher = AGE.matcher(text);
        if (matcher.find()) {
            age = Integer.parseInt(matcher.group(1));
        }
        String employment = null;
        if (containsAny(text, "무직", "미취업", "취준생", "구직자")) {
            employment = "UNEMPLOYED";
            keywords.add("취업");
            keywords.add("구직");
        } else if (containsAny(text, "재직", "직장인")) {
            employment = "EMPLOYED";
        }
        Boolean student = null;
        if (containsAny(text, "대학생", "재학생", "휴학생")) {
            student = true;
        }
        String category = null;
        if (containsAny(text, "월세", "주거")) category = "주거";
        if (containsAny(text, "면접", "취업", "구직")) category = "일자리";
        if (containsAny(text, "교육", "훈련")) category = "교육";
        if (containsAny(text, "자산", "저축", "지원금", "수당")) category = "금융";
        add(text, keywords, "청년", "청년");
        add(text, keywords, "지원금", "지원금", "청년");
        add(text, keywords, "수당", "수당", "청년");
        add(text, keywords, "보조금", "보조금", "청년");
        add(text, keywords, "면접", "면접", "취업");
        add(text, keywords, "월세", "월세", "주거");
        add(text, keywords, "생활비", "생활비");
        add(text, keywords, "자산", "자산형성", "저축");
        if (containsAny(text, "지원금", "수당", "보조금")) {
            supportTypes.add("CASH");
            supportTypes.add("ALLOWANCE");
            supportTypes.add("SUBSIDY");
        }
        if (keywords.isEmpty()) {
            keywords.add("청년");
        }
        return new PolicySearchCondition(province, city, null, age, employment, student, null, category, supportTypes, keywords, resultSize);
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
