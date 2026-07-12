package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageParseResult;

public interface NaturalLanguageQueryParser {
    NaturalLanguageParseResult parse(String query);
}
