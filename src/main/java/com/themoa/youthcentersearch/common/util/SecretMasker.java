package com.themoa.youthcentersearch.common.util;

import java.util.regex.Pattern;

public final class SecretMasker {
    private static final Pattern API_KEY_QUERY = Pattern.compile("(?i)(apiKeyNm=)([^&#]*)");

    private SecretMasker() {
    }

    public static String maskUrl(String value) {
        if (value == null) {
            return null;
        }
        return API_KEY_QUERY.matcher(value).replaceAll("$1****");
    }

    public static String maskValue(String value) {
        return value == null || value.isBlank() ? "" : "****";
    }
}
