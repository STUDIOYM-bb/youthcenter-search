package com.themoa.youthcentersearch.youthcenter.parser;

import com.themoa.youthcentersearch.common.exception.YouthCenterApiException;
import com.themoa.youthcentersearch.youthcenter.config.NaturalLanguageSearchProperties;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguageParseResult;
import com.themoa.youthcentersearch.youthcenter.dto.response.NaturalLanguagePolicyCondition;
import com.themoa.youthcentersearch.youthcenter.dto.response.ParserMode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class OpenAiNaturalLanguageQueryParser implements NaturalLanguageQueryParser {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final ObjectMapper objectMapper;
    private final NaturalLanguageSearchProperties properties;
    private final String apiKey;

    public OpenAiNaturalLanguageQueryParser(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                            ObjectMapper objectMapper,
                                            NaturalLanguageSearchProperties properties,
                                            @Value("${spring.ai.openai.api-key:}") String apiKey) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.apiKey = apiKey;
    }

    @Override
    public NaturalLanguageParseResult parse(String query) {
        if (!properties.isOpenaiEnabled() || !StringUtils.hasText(apiKey)) {
            throw new YouthCenterApiException("OpenAI parser is disabled or API key is empty.");
        }
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        if (builder == null) {
            throw new YouthCenterApiException("Spring AI ChatClient is not available.");
        }
        String prompt = """
                Extract only structured conditions from this Korean youth policy query.
                Do not recommend policies. Do not invent policy data.
                Return strict JSON with only these fields:
                region, age, employmentStatus, studentStatus, category, supportTypes, keywords.
                Do not generate API parameters, primarySearchKeyword, descriptionSearchText, or long search strings.
                employmentStatus examples: UNEMPLOYED, EMPLOYED.
                Query:
                """ + query;
        try {
            String content = builder.build()
                    .prompt()
                    .user(prompt)
                    .call()
                    .content();
            NaturalLanguagePolicyCondition raw = objectMapper.readValue(stripJsonFence(content), NaturalLanguagePolicyCondition.class);
            NaturalLanguagePolicyCondition condition = new NaturalLanguagePolicyCondition(raw.region(), raw.age(),
                    raw.employmentStatus(), raw.studentStatus(), raw.category(), raw.supportTypes(), raw.keywords(),
                    null, null);
            return new NaturalLanguageParseResult(condition, ParserMode.OPENAI, false, null);
        } catch (Exception ex) {
            throw new YouthCenterApiException("OpenAI ?먯뿰??議곌굔 異붿텧???ㅽ뙣?덉뒿?덈떎.", ex);
        }
    }

    private String stripJsonFence(String content) {
        if (content == null) {
            return "{}";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("^```json", "").replaceFirst("^```", "");
            int end = trimmed.lastIndexOf("```");
            if (end >= 0) {
                trimmed = trimmed.substring(0, end);
            }
        }
        return trimmed.trim();
    }
}
