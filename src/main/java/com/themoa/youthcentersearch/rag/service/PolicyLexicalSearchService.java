package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 메모리 BM25 인덱스를 통해 MySQL 기반 lexical 후보를 반환한다.
 * 과거 title/summary contains 점수는 제거하고, PolicyLexicalIndex가 계산한 필드 가중치 BM25와 제목 매칭만 노출한다.
 */
@Service
public class PolicyLexicalSearchService {
    private final PolicyLexicalIndexBuilder indexBuilder;

    public PolicyLexicalSearchService(PolicyRepository policyRepository, PolicyKeywordNormalizer normalizer,
                                      PolicyLexicalIndexBuilder indexBuilder) {
        this.indexBuilder = indexBuilder;
    }

    public LexicalSearchResult search(PolicySearchCondition condition, int limit) {
        return search(condition, null, limit);
    }

    public LexicalSearchResult search(PolicySearchCondition condition, PolicySearchIntent intent, int limit) {
        List<PolicyLexicalCandidate> candidates = indexBuilder.current().search(condition, intent, limit);
        if (candidates.isEmpty()) {
            return LexicalSearchResult.empty();
        }
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>();
        Map<Integer, Double> titleScores = new LinkedHashMap<>();
        Map<Integer, Set<CandidateSource>> candidateSources = new LinkedHashMap<>();
        Map<CandidateSource, Integer> sourceCounts = new EnumMap<>(CandidateSource.class);
        for (PolicyLexicalCandidate candidate : candidates) {
            lexicalScores.put(candidate.policyId(), candidate.lexicalScore());
            titleScores.put(candidate.policyId(), candidate.titleScore());
            Set<CandidateSource> sources = candidate.sources();
            candidateSources.put(candidate.policyId(), sources);
            for (CandidateSource source : sources) {
                sourceCounts.merge(source, 1, Integer::sum);
            }
        }
        List<Integer> sortedIds = candidates.stream().map(PolicyLexicalCandidate::policyId).toList();
        return new LexicalSearchResult(sortedIds, lexicalScores, titleScores, candidateSources, sourceCounts);
    }

    public record LexicalSearchResult(List<Integer> policyIds,
                                      Map<Integer, Double> lexicalScores,
                                      Map<Integer, Double> titleExactScores,
                                      Map<Integer, Set<CandidateSource>> candidateSources,
                                      Map<CandidateSource, Integer> sourceCounts) {
        public static LexicalSearchResult empty() {
            return new LexicalSearchResult(List.of(), Map.of(), Map.of(), Map.of(), Map.of());
        }

        public LexicalSearchResult {
            policyIds = policyIds == null ? List.of() : List.copyOf(policyIds);
            lexicalScores = lexicalScores == null ? Map.of() : Map.copyOf(lexicalScores);
            titleExactScores = titleExactScores == null ? Map.of() : Map.copyOf(titleExactScores);
            candidateSources = candidateSources == null ? Map.of() : Map.copyOf(candidateSources);
            sourceCounts = sourceCounts == null ? Map.of() : Map.copyOf(sourceCounts);
        }
    }
}
