package com.themoa.youthcentersearch.youthcenter.mapper;

import com.themoa.youthcentersearch.youthcenter.dto.parsed.YouthPolicyItem;
import com.themoa.youthcentersearch.youthcenter.dto.external.YouthCenterPolicyRaw;
import com.themoa.youthcentersearch.youthcenter.dto.response.YouthPolicyView;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Component
public class YouthPolicyMapper {
    private static final List<String> NUMBER_FIELDS = List.of("plcyNo", "policyNumber", "policyNo", "bizId");
    private static final List<String> NAME_FIELDS = List.of("plcyNm", "policyName", "title", "name");
    private static final List<String> DESCRIPTION_FIELDS = List.of("plcyExplnCn", "policyDescription", "description", "content");
    private static final List<String> KEYWORD_FIELDS = List.of("plcyKywdNm", "keywordNames", "keywords", "keyword");

    public YouthPolicyItem fromJson(JsonNode node) {
        Map<String, Object> fields = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            for (Map.Entry<String, JsonNode> entry : node.properties()) {
                fields.put(entry.getKey(), toPlainValue(entry.getValue()));
            }
        }
        return new YouthPolicyItem(first(fields, NUMBER_FIELDS), first(fields, NAME_FIELDS),
                first(fields, DESCRIPTION_FIELDS), first(fields, KEYWORD_FIELDS), fields);
    }

    public YouthPolicyItem fromMap(Map<String, Object> fields) {
        Map<String, Object> safe = fields == null ? new LinkedHashMap<>() : new LinkedHashMap<>(fields);
        return new YouthPolicyItem(first(safe, NUMBER_FIELDS), first(safe, NAME_FIELDS),
                first(safe, DESCRIPTION_FIELDS), first(safe, KEYWORD_FIELDS), safe);
    }

    public YouthPolicyView toView(YouthCenterPolicyRaw raw) {
        return toView(raw == null ? Map.of() : raw.toFieldMap());
    }

    public YouthPolicyView toView(YouthPolicyItem item) {
        return toView(item == null ? Map.of() : item.fields());
    }

    public YouthPolicyView toView(Map<String, Object> sourceFields) {
        Map<String, Object> fields = normalizeFields(sourceFields);
        return new YouthPolicyView(
                text(fields, "plcyNo"),
                text(fields, "plcyNm"),
                text(fields, "plcyExplnCn"),
                split(text(fields, "plcyKywdNm")),
                text(fields, "lclsfNm"),
                text(fields, "mclsfNm"),
                text(fields, "plcySprtCn"),
                text(fields, "sprvsnInstCdNm"),
                text(fields, "operInstCdNm"),
                integer(text(fields, "sprtTrgtMinAge")),
                integer(text(fields, "sprtTrgtMaxAge")),
                "Y".equalsIgnoreCase(nullToEmpty(text(fields, "sprtTrgtAgeLmtYn"))),
                applicationPeriod(fields),
                text(fields, "plcyAplyMthdCn"),
                text(fields, "aplyUrlAddr"),
                urls(text(fields, "refUrlAddr1"), text(fields, "refUrlAddr2")),
                incomeCondition(fields),
                text(fields, "addAplyQlfcCndCn"),
                text(fields, "ptcpPrpTrgtCn"),
                split(text(fields, "zipCd")),
                fields
        );
    }

    private static String first(Map<String, Object> fields, List<String> names) {
        for (String name : names) {
            Object value = fields.get(name);
            String normalized = normalizeString(value);
            if (normalized != null) {
                return normalized;
            }
        }
        return null;
    }

    private static Object toPlainValue(JsonNode value) {
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.numberValue();
        }
        if (value.isBoolean()) {
            return value.booleanValue();
        }
        if (value.isTextual()) {
            return value.asText();
        }
        return value.toString();
    }

    private static Map<String, Object> normalizeFields(Map<String, Object> fields) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        if (fields == null) {
            return normalized;
        }
        fields.forEach((key, value) -> normalized.put(key, value instanceof String ? normalizeString(value) : value));
        return normalized;
    }

    private static String text(Map<String, Object> fields, String key) {
        return normalizeString(fields.get(key));
    }

    private static String normalizeString(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() ? null : text;
    }

    private static List<String> split(String value) {
        if (!StringUtils.hasText(value)) {
            return List.of();
        }
        List<String> values = new ArrayList<>();
        for (String part : value.split(",")) {
            String normalized = normalizeString(part);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static Integer integer(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static List<String> urls(String... urls) {
        List<String> values = new ArrayList<>();
        for (String url : urls) {
            String normalized = normalizeString(url);
            if (normalized != null) {
                values.add(normalized);
            }
        }
        return values;
    }

    private static String applicationPeriod(Map<String, Object> fields) {
        String aplyYmd = text(fields, "aplyYmd");
        if (aplyYmd != null) {
            return aplyYmd;
        }
        String start = text(fields, "bizPrdBgngYmd");
        String end = text(fields, "bizPrdEndYmd");
        if (start == null && end == null) {
            return text(fields, "bizPrdEtcCn");
        }
        return nullToEmpty(start) + " ~ " + nullToEmpty(end);
    }

    private static String incomeCondition(Map<String, Object> fields) {
        List<String> values = new ArrayList<>();
        values.add(text(fields, "earnCndSeCd"));
        values.add(text(fields, "earnMinAmt"));
        values.add(text(fields, "earnMaxAmt"));
        values.add(text(fields, "earnEtcCn"));
        return values.stream()
                .filter(Objects::nonNull)
                .reduce((left, right) -> left + " / " + right)
                .orElse(null);
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
