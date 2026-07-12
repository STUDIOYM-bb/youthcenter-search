package com.themoa.youthcentersearch.youthcenter.parser;

import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ResponseTypeDetector {
    public ResponseType detect(String contentType, String body) {
        String trimmed = body == null ? "" : body.trim();
        if (trimmed.isEmpty()) {
            return ResponseType.EMPTY;
        }
        String lower = trimmed.toLowerCase(Locale.ROOT);
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            return ResponseType.JSON;
        }
        if (lower.startsWith("<!doctype html") || lower.startsWith("<html")) {
            return ResponseType.HTML;
        }
        if (trimmed.startsWith("<")) {
            return ResponseType.XML;
        }
        return ResponseType.UNKNOWN;
    }

    public String mismatchWarning(String contentType, ResponseType responseType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        String lower = contentType.toLowerCase(Locale.ROOT);
        boolean contentLooksJson = lower.contains("json");
        boolean contentLooksXml = lower.contains("xml");
        boolean contentLooksHtml = lower.contains("html");
        if (contentLooksJson && responseType != ResponseType.JSON) {
            return "Content-Type? JSON?댁?留?蹂몃Ц? " + responseType + " ?뺥깭?낅땲??";
        }
        if (contentLooksXml && responseType != ResponseType.XML) {
            return "Content-Type? XML?댁?留?蹂몃Ц? " + responseType + " ?뺥깭?낅땲??";
        }
        if (contentLooksHtml && responseType != ResponseType.HTML) {
            return "Content-Type? text/html?댁?留?蹂몃Ц? " + responseType + " ?뺥깭?낅땲??";
        }
        return null;
    }
}
