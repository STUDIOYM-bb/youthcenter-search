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
    private final PolicySearchConditionValidator conditionValidator;
    private final String openAiApiKey;

    public CompositePolicySearchConditionParser(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                                RuleBasedPolicySearchConditionParser ruleBasedParser,
                                                PolicySearchConditionValidator conditionValidator,
                                                @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.ruleBasedParser = ruleBasedParser;
        this.conditionValidator = conditionValidator;
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
                                    Return only conditions the user explicitly states about themselves or their search.
                                    Do not infer age, region, employmentStatus, or studentStatus from policy topic words.
                                    "청년 면접 수당" means keywords only: age=null, employmentStatus=null, province=null.
                                    "취업 지원 정책" or "면접 수당" does not mean the user is unemployed.
                                    "청년 정책" does not mean age=19.
                                    Set missing conditions to null, never to 0, false, or an empty string.
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
                        return new ParsedPolicySearchCondition(conditionValidator.validate(query, condition, resultSize), "OPENAI", false, null);
                    }
                } catch (RuntimeException ex) {
                    PolicySearchCondition fallback = ruleBasedParser.parseCondition(query, resultSize);
                    return new ParsedPolicySearchCondition(conditionValidator.validate(query, fallback, resultSize), "RULE_BASED", true, ex.getMessage());
                }
            }
        }
        return new ParsedPolicySearchCondition(conditionValidator.validate(query, ruleBasedParser.parseCondition(query, resultSize), resultSize), "RULE_BASED", true,
                "OpenAI ChatModel is not configured.");
    }
}
