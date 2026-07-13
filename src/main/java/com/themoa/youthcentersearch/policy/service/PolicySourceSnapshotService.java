package com.themoa.youthcentersearch.policy.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.themoa.youthcentersearch.policy.domain.Policy;
import com.themoa.youthcentersearch.policy.domain.PolicyRawData;
import com.themoa.youthcentersearch.policy.domain.PolicySource;
import com.themoa.youthcentersearch.policy.domain.PolicySourceSnapshot;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PolicySourceSnapshotService {
    private final PolicySourceSnapshotRepository repository;
    private final PolicyRepository policyRepository;
    private final ObjectMapper objectMapper;

    public PolicySourceSnapshotService(PolicySourceSnapshotRepository repository,
                                       PolicyRepository policyRepository,
                                       ObjectMapper objectMapper) {
        this.repository = repository;
        this.policyRepository = policyRepository;
        this.objectMapper = objectMapper;
    }

    public void upsert(Integer policyId, String sourcePolicyId, PolicyRawData rawData, Map<String, Object> fields) {
        Policy policy = policyRepository.findById(policyId).orElseThrow();
        String rawJson = writeJson(fields);
        String hash = DigestUtils.md5DigestAsHex(rawJson.getBytes(StandardCharsets.UTF_8));
        repository.findByPolicyId(policyId)
                .ifPresentOrElse(snapshot -> snapshot.update(rawData, rawJson, hash),
                        () -> repository.save(new PolicySourceSnapshot(policy, rawData, PolicySource.YOUTH_CENTER.name(),
                                sourcePolicyId, rawJson, hash)));
    }

    private String writeJson(Map<String, Object> fields) {
        try {
            return objectMapper.writeValueAsString(fields == null ? Map.of() : fields);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("정책 원본 JSON 직렬화에 실패했습니다.", ex);
        }
    }
}
