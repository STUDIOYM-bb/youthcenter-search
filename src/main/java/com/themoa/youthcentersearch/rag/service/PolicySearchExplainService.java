package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicyDomainClassification;
import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 정책별 검색 Explain과 Trace 응답을 조립하는 서비스다.
 *
 * <p>랭킹과 결과 DTO 조립 이후 호출된다. 입력은 SearchPlan, 정책, CandidateEvidence,
 * Eligibility 평가, Ranking 평가, Diagnostics이며 출력은 기존 API와 호환되는 Map 구조다.</p>
 *
 * <p>DB 또는 Qdrant/MySQL 검색을 다시 실행하지 않는다. Explain에서 검색을 재실행하면 목록과 Explain의
 * 후보 증거가 달라지고 정책 수만큼 추가 조회가 생길 수 있으므로, 검색 과정에서 만든 evidence만 사용한다.</p>
 */
@Service
public class PolicySearchExplainService {
    private final PolicyDomainClassifier domainClassifier;

    public PolicySearchExplainService(PolicyDomainClassifier domainClassifier) {
        this.domainClassifier = domainClassifier;
    }

    public Map<String, Object> explain(PolicySearchPlan plan,
                                       Policy policy,
                                       CandidateEvidence evidence,
                                       PolicyEligibilityEvaluation eligibility,
                                       PolicyRankingEvaluation ranking,
                                       PolicySearchDiagnostics diagnostics,
                                       boolean included) {
        PolicyDomainClassification domain = domainClassifier.classify(policy);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policyId", policy.getId());
        body.put("policyName", policy.getTitle());
        body.put("candidateSources", evidence.sourceEvidence().stream().map(item -> item.source().name()).distinct().sorted().toList());
        body.put("sourceEvidence", sourceEvidence(evidence));
        body.put("vectorScores", vectorScores(evidence));
        body.put("lexicalScore", ranking == null ? 0.0 : Math.round(ranking.lexicalScore() * 1000.0) / 10.0);
        body.put("titleScore", ranking == null ? 0.0 : Math.round(ranking.titleScore() * 1000.0) / 10.0);
        body.put("categoryScore", null);
        body.put("region", Map.of("status", eligibility.regionMatch().compatibility().name(), "eligible", eligibility.regionMatch().eligible(),
                "score", eligibility.regionMatch().score(), "reason", eligibility.regionMatch().reason()));
        body.put("age", Map.of("status", eligibility.ageMatch().status().name(), "reason", eligibility.ageMatch().reason()));
        body.put("employment", Map.of("status", eligibility.employmentMatch().status().name(), "reason", eligibility.employmentMatch().reason()));
        body.put("student", Map.of("status", eligibility.studentMatch().status().name(), "reason", eligibility.studentMatch().reason()));
        body.put("targetStage", Map.of("status", eligibility.educationStageMatch().status().name(),
                "reason", eligibility.educationStageMatch().reason()));
        body.put("confirmationReasons", eligibility.confirmationReasons());
        body.put("employmentAudience", Map.of("status", eligibility.employmentAudienceMatch().status().name(),
                "reason", eligibility.employmentAudienceMatch().reason()));
        body.put("recommendationTier", ranking == null ? eligibility.preliminaryTier().name() : ranking.recommendationTier().name());
        body.put("recommendationTierReason", eligibility.preliminaryTier().name());
        body.put("topicRelevance", Map.of("status", ranking == null ? "UNKNOWN_OR_FILTERED" : "MATCH",
                "score", ranking == null ? 0.0 : Math.round(ranking.topicScore() * 1000.0) / 10.0));
        body.put("domain", Map.of("primary", domain.primaryDomain().name(),
                "secondary", domain.secondaryDomains().stream().map(Enum::name).sorted().toList(),
                "supportIntents", domain.supportIntents().stream().map(Enum::name).sorted().toList(),
                "domainScore", ranking == null ? null : ranking.domainScore(),
                "supportIntentScore", ranking == null ? null : ranking.supportIntentScore()));
        body.put("finalDisposition", included ? "INCLUDED" : eligibility.excludedReason());
        body.put("finalScore", ranking == null ? null : ranking.finalScore());
        body.put("finalRank", ranking == null ? null : ranking.finalRank());
        body.put("trace", trace(policy, evidence, domain, eligibility, ranking));
        body.put("diagnostics", diagnostics);
        body.put("queryType", plan.queryType().name());
        return body;
    }

    private List<Map<String, Object>> sourceEvidence(CandidateEvidence evidence) {
        return evidence.sourceEvidence().stream()
                .map(item -> {
                    Map<String, Object> row = new LinkedHashMap<>();
                    row.put("source", item.source().name());
                    row.put("sourceRank", item.sourceRank());
                    row.put("rawScore", item.rawScore());
                    row.put("normalizedScore", item.normalizedScore());
                    row.put("queryVariant", item.queryVariant());
                    return row;
                })
                .toList();
    }

    private Map<String, Object> vectorScores(CandidateEvidence evidence) {
        Map<String, Object> scores = new LinkedHashMap<>();
        scores.put("originalQuery", sourceScore(evidence, CandidateSource.VECTOR_ORIGINAL_QUERY));
        scores.put("normalizedQuery", sourceScore(evidence, CandidateSource.VECTOR_NORMALIZED_QUERY));
        scores.put("intentQuery", sourceScore(evidence, CandidateSource.VECTOR_INTENT_QUERY));
        scores.put("expandedQuery", sourceScore(evidence, CandidateSource.VECTOR_EXPANDED_QUERY));
        scores.put("categoryQuery", sourceScore(evidence, CandidateSource.VECTOR_CATEGORY_QUERY));
        return scores;
    }

    private Map<String, Object> sourceScore(CandidateEvidence evidence, CandidateSource source) {
        CandidateSourceEvidence found = evidence.sourceEvidence().stream()
                .filter(item -> item.source() == source)
                .findFirst()
                .orElse(null);
        Map<String, Object> score = new LinkedHashMap<>();
        score.put("used", found != null);
        score.put("rank", found == null ? null : found.sourceRank());
        score.put("score", found == null ? null : found.rawScore());
        score.put("normalizedScore", found == null ? null : found.normalizedScore());
        score.put("queryVariant", found == null ? null : found.queryVariant());
        return score;
    }

    private Map<String, Object> trace(Policy policy,
                                      CandidateEvidence evidence,
                                      PolicyDomainClassification domain,
                                      PolicyEligibilityEvaluation eligibility,
                                      PolicyRankingEvaluation ranking) {
        Map<String, Object> trace = new LinkedHashMap<>();
        trace.put("policyId", policy.getId());
        trace.put("candidateSources", evidence.sourceEvidence().stream().map(item -> item.source().name()).distinct().sorted().toList());
        trace.put("sourceEvidence", sourceEvidence(evidence));
        trace.put("domain", domain.primaryDomain().name());
        trace.put("regionPassed", eligibility.regionMatch().eligible());
        trace.put("agePassed", eligibility.ageMatch().score() > 0);
        trace.put("employmentPassed", eligibility.employmentMatch().score() > 0);
        trace.put("studentPassed", eligibility.studentMatch().score() > 0);
        trace.put("finalScore", ranking == null ? null : ranking.finalScore());
        trace.put("finalRank", ranking == null ? null : ranking.finalRank());
        return trace;
    }
}
