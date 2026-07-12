package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class CompositePolicySearchConditionParser implements PolicySearchConditionParser {
    private final ObjectProvider<ChatClient.Builder> chatClientBuilderProvider;
    private final RuleBasedPolicySearchConditionParser ruleBasedParser;
    private final String openAiApiKey;

    public CompositePolicySearchConditionParser(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                                RuleBasedPolicySearchConditionParser ruleBasedParser,
                                                @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.ruleBasedParser = ruleBasedParser;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public ParsedPolicySearchCondition parse(String query, Integer resultSize) {
        if (StringUtils.hasText(openAiApiKey)) {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder != null) {
                try {
                    PolicySearchCondition condition = builder.build()
                            .prompt()
                            .system("""
                                    You extract structured Korean youth policy search conditions.
                                    Return only data that appears or is directly implied by the user query.
                                    Do not recommend or invent policies.
                                    Fields: province, city, district, age, employmentStatus, studentStatus,
                                    careerStage, category, supportTypes, keywords, resultSize.
                                    employmentStatus examples: UNEMPLOYED, EMPLOYED.
                                    supportTypes examples: CASH, ALLOWANCE, SUBSIDY, HOUSING, EDUCATION.
                                    """)
                            .user(query)
                            .call()
                            .entity(PolicySearchCondition.class);
                    if (condition != null) {
                        return new ParsedPolicySearchCondition(withResultSize(condition, resultSize), "OPENAI", false, null);
                    }
                } catch (RuntimeException ex) {
                    PolicySearchCondition fallback = ruleBasedParser.parseCondition(query, resultSize);
                    return new ParsedPolicySearchCondition(fallback, "RULE_BASED", true, ex.getMessage());
                }
            }
        }
        return new ParsedPolicySearchCondition(ruleBasedParser.parseCondition(query, resultSize), "RULE_BASED", true,
                "OpenAI ChatModel is not configured.");
    }

    private PolicySearchCondition withResultSize(PolicySearchCondition condition, Integer resultSize) {
        return new PolicySearchCondition(condition.province(), condition.city(), condition.district(), condition.age(),
                condition.employmentStatus(), condition.studentStatus(), condition.careerStage(), condition.category(),
                condition.supportTypes(), condition.keywords(), resultSize);
    }
}
