package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.region.RegionEligiblePolicyCandidate;
import com.themoa.youthcentersearch.policy.region.ResolvedUserRegion;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.service.RegionEligiblePolicyCandidateService;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import com.themoa.youthcentersearch.rag.dto.CandidateSource;
import com.themoa.youthcentersearch.rag.dto.PolicySearchCondition;
import com.themoa.youthcentersearch.rag.dto.PolicySearchIntent;
import com.themoa.youthcentersearch.rag.dto.PolicySearchPlan;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 자연어 검색 후보를 수집하고 policyId 기준으로 병합한다.
 *
 * <p>Qdrant 벡터 후보, BM25 lexical 후보, 지역 적격 pool 후보를 같은 순서로 모은다.
 * 이 클래스는 후보 생성만 담당하며 지역/나이/취업/교육 단계 hard filter나 최종 랭킹은 수행하지 않는다.</p>
 */
@Service
public class PolicySearchCandidateRetriever {
    private final PolicyRepository policyRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties properties;
    private final PolicyLexicalSearchService lexicalSearchService;
    private final RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService;

    public PolicySearchCandidateRetriever(PolicyRepository policyRepository,
                                          ObjectProvider<VectorStore> vectorStoreProvider,
                                          RagProperties properties,
                                          PolicyLexicalSearchService lexicalSearchService,
                                          RegionEligiblePolicyCandidateService regionEligiblePolicyCandidateService) {
        this.policyRepository = policyRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.properties = properties;
        this.lexicalSearchService = lexicalSearchService;
        this.regionEligiblePolicyCandidateService = regionEligiblePolicyCandidateService;
    }

    /**
     * SearchPlan과 사용자 지역을 기준으로 후보 정책과 원천 evidence를 수집한다.
     *
     * <p>명시적 제외 의도가 있는 경우 원문 벡터 검색 대신 정규화된 긍정 목적을 사용한다.
     * 부정어가 들어간 원문 embedding이 제외 분야 정책을 다시 끌어오는 것을 막기 위한 기존 규칙이다.</p>
     */
    public PolicyCandidateCollection retrieve(PolicySearchPlan plan,
                                              PolicySearchIntent intent,
                                              ResolvedUserRegion userRegion,
                                              int page,
                                              int size,
                                              int resultSize) {
        PolicySearchCondition condition = plan.condition();
        SearchQueryType queryType = plan.queryType();
        boolean regionPoolApplied = condition.regionExplicit() && regionEligiblePolicyCandidateService != null;
        List<RegionEligiblePolicyCandidate> regionEligibleCandidates = regionPoolApplied
                ? regionEligiblePolicyCandidateService.findEligibleCandidates(userRegion)
                : List.of();
        Set<Integer> regionEligibleIds = regionEligibleCandidates.stream()
                .map(RegionEligiblePolicyCandidate::policyId)
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
        RegionPoolCounts regionPoolCounts = RegionPoolCounts.from(regionEligibleCandidates);

        VectorStore vectorStore = vectorStoreProvider.getIfAvailable();
        boolean mysqlFallbackUsed = false;
        boolean retried = false;
        String fallbackReason = null;
        Map<CandidateSource, List<Document>> vectorCandidatesBySource = new EnumMap<>(CandidateSource.class);

        if (properties.isEnabled() && vectorStore != null) {
            String firstQuery = plan.explicitExclusion() ? plan.normalizedGoal() : plan.originalQuery();
            CandidateSource firstSource = plan.explicitExclusion()
                    ? CandidateSource.VECTOR_NORMALIZED_QUERY
                    : CandidateSource.VECTOR_ORIGINAL_QUERY;
            vectorCandidatesBySource.put(firstSource,
                    vectorSearch(vectorStore, firstQuery, properties.getSearch().getTopK(), true));

            String secondQuery = switch (queryType) {
                case POLICY_NAME -> intent.originalQuery();
                case BROAD_DISCOVERY -> categoryQuery(condition, intent);
                case TOPIC_SEARCH, ELIGIBILITY_SEARCH -> intent.semanticQuery();
            };
            if (!secondQuery.equals(plan.originalQuery())) {
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_QUERY,
                        vectorSearch(vectorStore, secondQuery, properties.getSearch().getTopK(), true));
            }
            int vectorCandidateCount = vectorCandidatesBySource.values().stream().mapToInt(List::size).sum();
            if (vectorCandidateCount < resultSize) {
                retried = true;
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_QUERY,
                        vectorSearch(vectorStore, secondQuery, properties.getSearch().getRetryTopK(), true));
            }
            if (vectorCandidatesBySource.values().stream().allMatch(List::isEmpty)) {
                retried = true;
                fallbackReason = "VECTOR_THRESHOLD_EMPTY";
                vectorCandidatesBySource.put(CandidateSource.VECTOR_INTENT_QUERY,
                        vectorSearch(vectorStore, secondQuery, properties.getSearch().getRetryTopK(), false));
            }
        } else {
            fallbackReason = "VECTOR_SEARCH_DISABLED";
        }

        Map<Integer, Double> semanticScores = semanticScores(vectorCandidatesBySource);
        Map<Integer, Set<CandidateSource>> candidateSources = candidateSources(vectorCandidatesBySource);
        Map<Integer, List<CandidateSourceEvidence>> sourceEvidence = sourceEvidence(vectorCandidatesBySource);

        var lexical = lexicalSearchService.search(condition, intent, properties.getSearch().getRetryTopK());
        Map<Integer, Double> lexicalScores = new LinkedHashMap<>(lexical.lexicalScores());
        Map<Integer, Double> titleExactScores = new LinkedHashMap<>(lexical.titleExactScores());
        for (int i = 0; i < lexical.policyIds().size(); i++) {
            Integer policyId = lexical.policyIds().get(i);
            int rank = i + 1;
            double lexicalScore = lexicalScores.getOrDefault(policyId, 0.0);
            double titleScore = titleExactScores.getOrDefault(policyId, 0.0);
            Set<CandidateSource> merged = candidateSources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class));
            Set<CandidateSource> lexicalSources = lexical.candidateSources().getOrDefault(policyId, Set.of());
            merged.addAll(lexicalSources);
            merged.add(CandidateSource.LEXICAL_INDEX);
            addEvidence(sourceEvidence, policyId, CandidateSource.LEXICAL_INDEX, rank, lexicalScore, lexicalScore, "BM25");
            for (CandidateSource source : lexicalSources) {
                addEvidence(sourceEvidence, policyId, source, rank, lexicalScore, lexicalScore, "BM25_FIELD");
            }
            if (titleScore >= 1.0) {
                merged.add(CandidateSource.EXACT_TITLE);
                addEvidence(sourceEvidence, policyId, CandidateSource.EXACT_TITLE, rank, titleScore, titleScore, "TITLE");
            } else if (titleScore >= 0.75) {
                merged.add(CandidateSource.TITLE_PHRASE);
                addEvidence(sourceEvidence, policyId, CandidateSource.TITLE_PHRASE, rank, titleScore, titleScore, "TITLE");
            }
        }

        LinkedHashSet<Integer> mergedIds = new LinkedHashSet<>(semanticScores.keySet());
        int beforeLexicalMerge = mergedIds.size();
        mergedIds.addAll(lexical.policyIds());
        if (queryType == SearchQueryType.BROAD_DISCOVERY || queryType == SearchQueryType.ELIGIBILITY_SEARCH) {
            List<Integer> broadIds = regionPoolApplied
                    ? regionEligibleIds.stream().toList()
                    : policyRepository.findActivePolicyIds(PageRequest.of(0,
                    Math.max(properties.getSearch().getRetryTopK(), (page + 2) * size * 5)));
            broadIds.forEach(id -> {
                candidateSources.computeIfAbsent(id, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.BROAD_ELIGIBLE_POOL);
                addEvidence(sourceEvidence, id, CandidateSource.BROAD_ELIGIBLE_POOL, null, null, null, "REGION_POOL");
            });
            mergedIds.addAll(broadIds);
        }
        if (regionPoolApplied) {
            mergedIds.removeIf(id -> !regionEligibleIds.contains(id));
        }

        int rawCandidateCount = vectorCandidatesBySource.values().stream().mapToInt(List::size).sum()
                + lexical.policyIds().size()
                + (int) candidateSources.entrySet().stream()
                .filter(entry -> entry.getValue().contains(CandidateSource.BROAD_ELIGIBLE_POOL))
                .count();
        int duplicateCandidateCount = Math.max(0, rawCandidateCount - mergedIds.size());
        List<Policy> policies = mergedIds.isEmpty()
                ? List.of()
                : policyRepository.findWithRelationsByIdIn(new ArrayList<>(mergedIds));
        if (policies.isEmpty() && properties.getSearch().isMysqlFallbackEnabled()) {
            mysqlFallbackUsed = true;
            fallbackReason = fallbackReason == null ? "NO_CANDIDATES" : fallbackReason;
            List<Integer> fallbackIds = regionPoolApplied
                    ? regionEligibleIds.stream().limit(properties.getSearch().getRetryTopK()).toList()
                    : policyRepository.findActivePolicyIds(PageRequest.of(0, properties.getSearch().getRetryTopK()));
            policies = policyRepository.findWithRelationsByIdIn(fallbackIds);
            fallbackIds.forEach(id -> {
                candidateSources.computeIfAbsent(id, ignored -> EnumSet.noneOf(CandidateSource.class))
                        .add(CandidateSource.MYSQL_FALLBACK);
                addEvidence(sourceEvidence, id, CandidateSource.MYSQL_FALLBACK, null, null, null, "MYSQL_FALLBACK");
            });
        }

        int fallbackAddedCount = mysqlFallbackUsed ? Math.max(0, policies.size() - beforeLexicalMerge) : 0;
        CandidateCollectionMetrics metrics = new CandidateCollectionMetrics(
                vectorCandidatesBySource.values().stream().mapToInt(List::size).sum(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_ORIGINAL_QUERY, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_INTENT_QUERY, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_EXPANDED_QUERY, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_CATEGORY_QUERY, List.of()).size(),
                vectorCandidatesBySource.getOrDefault(CandidateSource.VECTOR_NORMALIZED_QUERY, List.of()).size(),
                lexical.policyIds().size(),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_TITLE, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_KEYWORD, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_SUMMARY, 0),
                lexical.sourceCounts().getOrDefault(CandidateSource.MYSQL_CATEGORY, 0),
                policies.size(),
                duplicateCandidateCount,
                fallbackAddedCount,
                regionPoolCounts.total(),
                regionPoolCounts.exactSigungu(),
                regionPoolCounts.parentSido(),
                regionPoolCounts.nationwide(),
                regionPoolCounts.multiple()
        );
        Map<Integer, CandidateEvidence> evidenceByPolicyId = new LinkedHashMap<>();
        candidateSources.keySet().forEach(policyId -> evidenceByPolicyId.put(policyId,
                new CandidateEvidence(policyId,
                        sourceEvidence.getOrDefault(policyId, List.of()),
                        semanticScores.getOrDefault(policyId, 0.0),
                        lexicalScores.getOrDefault(policyId, 0.0),
                        titleExactScores.getOrDefault(policyId, 0.0))));
        return new PolicyCandidateCollection(policies, evidenceByPolicyId, semanticScores, lexicalScores,
                titleExactScores, candidateSources, metrics, retried, mysqlFallbackUsed, fallbackReason);
    }

    private List<Document> vectorSearch(VectorStore vectorStore, String query, int topK, boolean applyThreshold) {
        SearchRequest.Builder builder = SearchRequest.builder()
                .query(query)
                .topK(topK);
        if (applyThreshold) {
            builder.similarityThreshold(properties.getSearch().getMinimumSimilarity());
        }
        return vectorStore.similaritySearch(builder.build());
    }

    private Map<Integer, Double> semanticScores(Map<CandidateSource, List<Document>> documentsBySource) {
        Map<Integer, Double> scores = new LinkedHashMap<>();
        for (var entry : documentsBySource.entrySet()) {
            double weight = switch (entry.getKey()) {
                case VECTOR_ORIGINAL_QUERY -> 1.0;
                case VECTOR_NORMALIZED_QUERY -> 0.95;
                case VECTOR_INTENT_QUERY -> 0.85;
                default -> 0.6;
            };
            List<Document> documents = entry.getValue();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                Integer policyId = policyId(document);
                if (policyId != null) {
                    double vector = document.getScore() == null ? 0.0 : document.getScore();
                    double rrf = weight / (60.0 + i + 1);
                    scores.merge(policyId, Math.max(vector * 0.35, rrf * 20.0), Math::max);
                }
            }
        }
        return scores;
    }

    private Map<Integer, Set<CandidateSource>> candidateSources(Map<CandidateSource, List<Document>> documentsBySource) {
        Map<Integer, Set<CandidateSource>> sources = new LinkedHashMap<>();
        for (var entry : documentsBySource.entrySet()) {
            for (Document document : entry.getValue()) {
                Integer policyId = policyId(document);
                if (policyId != null) {
                    sources.computeIfAbsent(policyId, ignored -> EnumSet.noneOf(CandidateSource.class)).add(entry.getKey());
                }
            }
        }
        return sources;
    }

    private Map<Integer, List<CandidateSourceEvidence>> sourceEvidence(Map<CandidateSource, List<Document>> documentsBySource) {
        Map<Integer, List<CandidateSourceEvidence>> evidence = new LinkedHashMap<>();
        for (var entry : documentsBySource.entrySet()) {
            List<Document> documents = entry.getValue();
            for (int i = 0; i < documents.size(); i++) {
                Document document = documents.get(i);
                Integer policyId = policyId(document);
                if (policyId != null) {
                    double score = document.getScore() == null ? 0.0 : document.getScore();
                    addEvidence(evidence, policyId, entry.getKey(), i + 1, score, score, entry.getKey().name());
                }
            }
        }
        return evidence;
    }

    private void addEvidence(Map<Integer, List<CandidateSourceEvidence>> evidence,
                             Integer policyId,
                             CandidateSource source,
                             Integer rank,
                             Double rawScore,
                             Double normalizedScore,
                             String queryVariant) {
        evidence.computeIfAbsent(policyId, ignored -> new ArrayList<>())
                .add(new CandidateSourceEvidence(source, rank, rawScore, normalizedScore, queryVariant));
    }

    private Integer policyId(Document document) {
        Object value = document.getMetadata().get("policyId");
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String text && text.matches("\\d+")) {
            return Integer.parseInt(text);
        }
        return null;
    }

    private String categoryQuery(PolicySearchCondition condition, PolicySearchIntent intent) {
        if (StringUtils.hasText(condition.category())) {
            return "청년 " + condition.category() + " 지원 정책";
        }
        return intent.intentTerms().isEmpty() ? intent.semanticQuery() : String.join(" ", intent.intentTerms()) + " 정책";
    }

    private record RegionPoolCounts(int total, int exactSigungu, int parentSido, int nationwide, int multiple) {
        static RegionPoolCounts from(List<RegionEligiblePolicyCandidate> rows) {
            int exactSigungu = 0;
            int parentSido = 0;
            int nationwide = 0;
            int multiple = 0;
            for (RegionEligiblePolicyCandidate row : rows) {
                switch (row.compatibility()) {
                    case EXACT_SIGUNGU -> exactSigungu++;
                    case PARENT_SIDO, EXACT_SIDO -> parentSido++;
                    case NATIONWIDE -> nationwide++;
                    case MULTIPLE_REGION_MATCH -> multiple++;
                    default -> {
                    }
                }
            }
            return new RegionPoolCounts(rows.size(), exactSigungu, parentSido, nationwide, multiple);
        }
    }
}
