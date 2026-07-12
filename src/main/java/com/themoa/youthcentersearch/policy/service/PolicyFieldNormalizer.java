package com.themoa.youthcentersearch.policy.service;

import com.themoa.youthcentersearch.policy.domain.PolicyCategory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;

@Component
public class PolicyFieldNormalizer {
    public String text(Map<String, Object> fields, String... names) {
        for (String name : names) {
            Object value = fields.get(name);
            if (value != null && StringUtils.hasText(String.valueOf(value).trim())) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }

    public Integer integer(Map<String, Object> fields, String... names) {
        String value = text(fields, names);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9-]", "");
        if (!StringUtils.hasText(digits)) {
            return null;
        }
        try {
            return Integer.parseInt(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public LocalDate date(Map<String, Object> fields, String... names) {
        String value = text(fields, names);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() != 8) {
            return null;
        }
        try {
            return LocalDate.parse(digits, DateTimeFormatter.BASIC_ISO_DATE);
        } catch (RuntimeException ex) {
            return null;
        }
    }

    public PolicyCategory category(Map<String, Object> fields) {
        String combined = String.join(" ",
                nullToEmpty(text(fields, "lclsfNm", "mclsfNm", "plcyMajorCd", "plcyKywdNm")),
                nullToEmpty(text(fields, "plcyNm")),
                nullToEmpty(text(fields, "plcyExplnCn")));
        if (containsAny(combined, "일자리", "취업", "구직", "면접")) return PolicyCategory.일자리;
        if (containsAny(combined, "주거", "월세", "전세", "임대")) return PolicyCategory.주거;
        if (containsAny(combined, "교육", "훈련", "강의")) return PolicyCategory.교육;
        if (containsAny(combined, "금융", "자산", "저축", "대출", "지원금", "수당")) return PolicyCategory.금융;
        if (containsAny(combined, "창업", "벤처")) return PolicyCategory.창업;
        if (containsAny(combined, "문화", "예술")) return PolicyCategory.문화;
        if (containsAny(combined, "건강", "의료")) return PolicyCategory.건강;
        if (containsAny(combined, "돌봄", "보육")) return PolicyCategory.돌봄;
        if (containsAny(combined, "복지")) return PolicyCategory.복지;
        if (containsAny(combined, "생활")) return PolicyCategory.생활지원;
        return PolicyCategory.기타;
    }

    public String truncate(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        int sentenceEnd = Math.max(value.lastIndexOf('.', max), Math.max(value.lastIndexOf('。', max), value.lastIndexOf('\n', max)));
        if (sentenceEnd > max / 2) {
            return value.substring(0, sentenceEnd).trim();
        }
        return value.substring(0, max).trim();
    }

    public String firstUrl(Map<String, Object> fields) {
        return truncate(text(fields, "aplyUrlAddr", "refUrlAddr1", "refUrlAddr2"), 500);
    }

    private boolean containsAny(String value, String... terms) {
        for (String term : terms) {
            if (value.contains(term)) {
                return true;
            }
        }
        return false;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
