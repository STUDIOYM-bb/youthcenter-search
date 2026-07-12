package com.themoa.youthcentersearch.youthcenter.client;

import java.util.List;
import java.util.Map;

public record ExternalApiResponse(
        int statusCode,
        String reasonPhrase,
        String contentType,
        Map<String, List<String>> headers,
        String body,
        String maskedRequestUrl,
        String redirectLocation,
        List<String> redirectHistory,
        String finalMaskedUrl,
        List<String> warnings,
        long elapsedTimeMs
) {
    public boolean redirected() {
        return redirectLocation != null || (redirectHistory != null && !redirectHistory.isEmpty());
    }

    public int bodyLength() {
        return body == null ? 0 : body.length();
    }
}
