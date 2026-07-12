package com.themoa.youthcentersearch.youthcenter.service;

import com.themoa.youthcentersearch.youthcenter.dto.request.NaturalLanguageSearchRequest;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageParseResult;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageSearchResponse;
import com.themoa.youthcentersearch.youthcenter.parser.CompositeNaturalLanguageQueryParser;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NaturalLanguagePolicySearchService {
    private final CompositeNaturalLanguageQueryParser queryParser;

    public NaturalLanguagePolicySearchService(CompositeNaturalLanguageQueryParser queryParser) {
        this.queryParser = queryParser;
    }

    public NaturalLanguageSearchResponse search(NaturalLanguageSearchRequest request) {
        NaturalLanguageParseResult parseResult = queryParser.parse(request.query());
        return new NaturalLanguageSearchResponse(
                request.query(),
                parseResult.parserMode(),
                parseResult.fallback(),
                parseResult.fallbackReason(),
                parseResult.condition(),
                List.of(),
                0,
                0,
                0,
                0,
                0,
                0,
                0,
                false,
                List.of("정식 서비스에서는 /api/policies/search RAG 검색 API를 사용합니다."),
                List.of()
        );
    }
}
