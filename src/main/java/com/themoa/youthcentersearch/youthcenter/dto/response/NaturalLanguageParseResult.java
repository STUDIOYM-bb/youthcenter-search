package com.themoa.youthcentersearch.youthcenter.dto.response;

public record NaturalLanguageParseResult(
        NaturalLanguagePolicyCondition condition,
        ParserMode parserMode,
        boolean fallback,
        String fallbackReason
) {
}
