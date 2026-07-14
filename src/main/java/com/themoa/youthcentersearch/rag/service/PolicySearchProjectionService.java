package com.themoa.youthcentersearch.rag.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicySearchProjection;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class PolicySearchProjectionService {
    public static final String VERSION = "policy-search-v2";
    private static final Logger log = LoggerFactory.getLogger(PolicySearchProjectionService.class);

    private final PolicyRepository policyRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyKeywordNormalizer normalizer;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @PersistenceContext
    private EntityManager entityManager;

    public PolicySearchProjectionService(PolicyRepository policyRepository,
                                         PolicySourceSnapshotRepository snapshotRepository,
                                         PolicySearchProjectionRepository projectionRepository,
                                         PolicyKeywordNormalizer normalizer,
                                         ObjectMapper objectMapper,
                                         TransactionTemplate transactionTemplate) {
        this.policyRepository = policyRepository;
        this.snapshotRepository = snapshotRepository;
        this.projectionRepository = projectionRepository;
        this.normalizer = normalizer;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
    }

    public ProjectionRebuildResult rebuildAll() {
        long total = policyRepository.countByActiveTrue();
        long processed = 0;
        long missingSnapshot = 0;
        int page = 0;
        int batchSize = 200;
        while (true) {
            var ids = policyRepository.findActivePolicyIds(PageRequest.of(page++, batchSize));
            if (ids.isEmpty()) break;
            ProjectionBatchResult result = transactionTemplate.execute(status -> rebuildBatch(ids));
            processed += result.processed();
            missingSnapshot += result.missingSnapshot();
        }
        return new ProjectionRebuildResult(total, processed, missingSnapshot);
    }

    @Transactional
    public void rebuildOne(Policy policy) {
        ProjectionSource source = source(policy);
        upsert(policy, source.fields(), source.missingSnapshot());
    }

    public ProjectionBatchResult rebuildBatch(List<Integer> ids) {
        var policies = policyRepository.findWithRelationsByIdIn(ids);
        Map<Integer, com.themoa.youthcentersearch.policy.domain.PolicySourceSnapshot> snapshots =
                snapshotRepository.findByPolicyIdIn(ids).stream()
                        .collect(Collectors.toMap(snapshot -> snapshot.getPolicy().getId(), Function.identity()));
        long missingSnapshot = 0;
        for (Policy policy : policies) {
            ProjectionSource source = source(policy, snapshots.get(policy.getId()));
            upsert(policy, source.fields(), source.missingSnapshot());
            if (source.missingSnapshot()) missingSnapshot++;
        }
        return new ProjectionBatchResult(policies.size(), missingSnapshot);
    }

    private ProjectionSource source(Policy policy) {
        return source(policy, snapshotRepository.findByPolicyId(policy.getId()).orElse(null));
    }

    private ProjectionSource source(Policy policy, com.themoa.youthcentersearch.policy.domain.PolicySourceSnapshot snapshot) {
        if (snapshot != null) {
            try {
                Map<String, Object> fields = objectMapper.readValue(snapshot.getRawPolicyJson(), new TypeReference<>() {
                });
                return new ProjectionSource(fields, false);
            } catch (Exception ex) {
                log.warn("Policy snapshot parse failed for search projection. policyId={}", policy.getId(), ex);
            }
        }
        Map<String, Object> fallback = new LinkedHashMap<>();
        fallback.put("plcyNm", policy.getTitle());
        fallback.put("plcyExplnCn", policy.getSummary());
        fallback.put("sprvsnInstCdNm", policy.getAgencyName());
        if (policy.getCondition() != null) {
            fallback.put("ptcpPrpTrgtCn", policy.getCondition().getConditionSummary());
        }
        fallback.put("lclsfNm", policy.getCategory() == null ? "" : policy.getCategory().name());
        return new ProjectionSource(fallback, true);
    }

    private void upsert(Policy policy, Map<String, Object> fields, boolean missingSnapshot) {
        String title = text(fields, "plcyNm", "title", "policyName");
        if (!StringUtils.hasText(title)) {
            title = policy.getTitle();
        }
        String keyword = text(fields, "plcyKywdNm", "keyword");
        String category = join(text(fields, "lclsfNm"), text(fields, "mclsfNm"));
        String description = text(fields, "plcyExplnCn");
        String support = text(fields, "plcySprtCn");
        String target = text(fields, "ptcpPrpTrgtCn");
        String qualification = join(text(fields, "addAplyQlfcCndCn"), text(fields, "earnEtcCn"), text(fields, "bizPrdEtcCn"));
        String application = text(fields, "plcyAplyMthdCn");
        String institution = join(text(fields, "sprvsnInstCdNm"), text(fields, "operInstCdNm"),
                text(fields, "rgtrInstCdNm"), text(fields, "rgtrUpInstCdNm"), text(fields, "rgtrHghrkInstCdNm"));
        String full = Stream.of(title, keyword, category, description, support, target, qualification, application, institution)
                .filter(StringUtils::hasText)
                .collect(Collectors.joining("\n"));
        var existing = projectionRepository.findByPolicyId(policy.getId());
        PolicySearchProjection projection = existing.orElseGet(() -> new PolicySearchProjection(policy));
        projection.update(normalizer.normalize(title), title, keyword, category, description, support,
                target, qualification, application, institution, full, VERSION, missingSnapshot);
        if (existing.isEmpty()) {
            entityManager.persist(projection);
        }
    }

    private String text(Map<String, Object> fields, String... keys) {
        for (String key : keys) {
            Object value = fields.get(key);
            if (value != null && StringUtils.hasText(String.valueOf(value))) {
                return String.valueOf(value).trim();
            }
        }
        return "";
    }

    private String join(String... values) {
        return Stream.of(values).filter(StringUtils::hasText).collect(Collectors.joining(" "));
    }

    private record ProjectionSource(Map<String, Object> fields, boolean missingSnapshot) {
    }

    public record ProjectionRebuildResult(long total, long processed, long missingSnapshot) {
    }

    public record ProjectionBatchResult(long processed, long missingSnapshot) {
    }
}
