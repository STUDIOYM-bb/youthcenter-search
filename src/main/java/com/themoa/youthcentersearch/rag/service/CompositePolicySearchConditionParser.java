package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicyQuerySemantics;
import com.themoa.youthcentersearch.rag.dto.SearchDomain;
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
    private final PolicyIntentPolarityDetector polarityDetector;
    private final String openAiApiKey;

    public CompositePolicySearchConditionParser(ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
                                                RuleBasedPolicySearchConditionParser ruleBasedParser,
                                                PolicySearchConditionValidator conditionValidator,
                                                PolicyIntentPolarityDetector polarityDetector,
                                                @Value("${spring.ai.openai.api-key:}") String openAiApiKey) {
        this.chatClientBuilderProvider = chatClientBuilderProvider;
        this.ruleBasedParser = ruleBasedParser;
        this.conditionValidator = conditionValidator;
        this.polarityDetector = polarityDetector;
        this.openAiApiKey = openAiApiKey;
    }

    @Override
    public ParsedPolicySearchCondition parse(String query, Integer resultSize) {
        if (StringUtils.hasText(openAiApiKey)) {
            ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
            if (builder != null) {
                try {
                    OpenAiPolicySearchAnalysis analysis = builder.build()
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
                                    Include rawRegionText when the user wrote a region phrase such as a province alias,
                                    official municipality name, or province-and-municipality phrase.
                                    Also separate positive search intent from negative/exclusion intent.
                                    A mentioned word is not necessarily a desired topic.
                                    Detect negation, exclusion, and lack-of-interest expressions.
                                    These exclude EMPLOYMENT: "취업 생각은 없어", "취업에는 관심 없어", "취업 정책은 필요 없어",
                                    "취업 말고", "취업 정책 제외", "일자리 지원은 빼줘", "구직 관련 정책은 원하지 않아".
                                    These may not mean employmentStatus. Current employment status and whether the user wants
                                    employment policies are separate.
                                    Positive EMPLOYMENT examples: "취업 지원을 받고 싶어", "취업 정책을 찾아줘",
                                    "구직 중이라 지원이 필요해", "면접비 지원을 찾고 있어", "취업 준비 중이야".
                                    UNEMPLOYED plus positive EMPLOYMENT examples: "미취업이라 취업 지원 정책을 찾고 있어",
                                    "직장이 없어서 구직 지원이 필요해", "취준생인데 면접비 지원을 찾고 있어".
                                    EMPLOYED status examples: "직장에 다니고 있다", "회사에서 일하고 있다",
                                    "현재 근무 중이다", "재직 중이다", "직장인", "회사원", "근로자".
                                    UNEMPLOYED status examples: "직장을 구하고 있다", "직장을 찾고 있다",
                                    "취업 준비 중이다", "현재 무직이다", "직장이 없다".
                                    "직장에 다니고 있다" means employmentStatus=EMPLOYED but not desiredDomains=EMPLOYMENT.
                                    "직장에 다니며 이직 지원을 찾고 있다" means employmentStatus=EMPLOYED and desiredDomains may include EMPLOYMENT.
                                    Return normalizedGoal with only positive search purpose. Do not include excluded topic words
                                    in normalizedGoal.
                                    Fields: rawRegionText, province, city, district, age, employmentStatus, studentStatus,
                                    careerStage, category, supportTypes, keywords, resultSize,
                                    normalizedGoal, desiredDomains, excludedDomains, positiveKeywords, excludedKeywords,
                                    explicitExclusion.
                                    employmentStatus examples: UNEMPLOYED, EMPLOYED.
                                    supportTypes examples: CASH, ALLOWANCE, SUBSIDY, HOUSING, EDUCATION.
                                    desiredDomains/excludedDomains examples: EMPLOYMENT, HOUSING, EDUCATION, WELFARE,
                                    FINANCE, STARTUP, CULTURE, HEALTH, CARE, GENERAL.
                                    """)
                            .user(query)
                            .call()
                            .entity(OpenAiPolicySearchAnalysis.class);
                    if (analysis != null) {
                        PolicySearchCondition condition = analysis.toCondition(resultSize);
                        PolicyQuerySemantics openAiSemantics = analysis.toSemantics();
                        PolicyQuerySemantics semantics = polarityDetector.merge(query, openAiSemantics);
                        return new ParsedPolicySearchCondition(conditionValidator.validate(query, condition, resultSize),
                                semantics, "OPENAI", false, null);
                    }
                } catch (RuntimeException ex) {
                    PolicySearchCondition fallback = ruleBasedParser.parseCondition(query, resultSize);
                    PolicyQuerySemantics semantics = polarityDetector.merge(query, null);
                    return new ParsedPolicySearchCondition(conditionValidator.validate(query, fallback, resultSize),
                            semantics, "RULE_BASED", true, ex.getMessage());
                }
            }
        }
        return new ParsedPolicySearchCondition(conditionValidator.validate(query, ruleBasedParser.parseCondition(query, resultSize), resultSize),
                polarityDetector.merge(query, null), "RULE_BASED", true,
                "OpenAI ChatModel is not configured.");
    }

    record OpenAiPolicySearchAnalysis(
            String rawRegionText,
            String province,
            String city,
            String district,
            Integer age,
            String employmentStatus,
            Boolean studentStatus,
            String careerStage,
            String category,
            java.util.Set<String> supportTypes,
            java.util.Set<String> keywords,
            String normalizedGoal,
            java.util.Set<String> desiredDomains,
            java.util.Set<String> excludedDomains,
            java.util.Set<String> positiveKeywords,
            java.util.Set<String> excludedKeywords,
            Boolean explicitExclusion
    ) {
        PolicySearchCondition toCondition(Integer resultSize) {
            return new PolicySearchCondition(province, city, district, age, employmentStatus, studentStatus,
                    careerStage, category, supportTypes, keywords, resultSize);
        }

        PolicyQuerySemantics toSemantics() {
            return new PolicyQuerySemantics(normalizedGoal,
                    domains(desiredDomains), domains(excludedDomains), positiveKeywords, excludedKeywords,
                    Boolean.TRUE.equals(explicitExclusion));
        }

        private java.util.Set<SearchDomain> domains(java.util.Set<String> raw) {
            if (raw == null) return java.util.Set.of();
            return raw.stream().map(SearchDomain::fromRaw)
                    .collect(java.util.stream.Collectors.toCollection(java.util.LinkedHashSet::new));
        }
    }
}
