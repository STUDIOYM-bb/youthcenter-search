package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageParseResult;
import com.themoa.youthcentersearch.youthcenter.dto.response.ParserMode;
import org.springframework.stereotype.Component;

@Component
public class CompositeNaturalLanguageQueryParser implements NaturalLanguageQueryParser {
    private final OpenAiNaturalLanguageQueryParser openAiParser;
    private final RuleBasedNaturalLanguageQueryParser ruleBasedParser;

    public CompositeNaturalLanguageQueryParser(OpenAiNaturalLanguageQueryParser openAiParser,
                                               RuleBasedNaturalLanguageQueryParser ruleBasedParser) {
        this.openAiParser = openAiParser;
        this.ruleBasedParser = ruleBasedParser;
    }

    @Override
    public NaturalLanguageParseResult parse(String query) {
        try {
            return openAiParser.parse(query);
        } catch (RuntimeException ex) {
            NaturalLanguageParseResult fallback = ruleBasedParser.parse(query);
            return new NaturalLanguageParseResult(fallback.condition(), ParserMode.RULE_BASED, true, ex.getMessage());
        }
    }
}
