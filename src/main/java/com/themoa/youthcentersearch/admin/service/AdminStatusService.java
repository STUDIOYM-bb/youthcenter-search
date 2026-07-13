package com.themoa.youthcentersearch.admin.service;

import com.themoa.youthcentersearch.admin.dto.AdminStatusResponse;
import com.themoa.youthcentersearch.common.config.LocalSecretConfigurationStatus;
import com.themoa.youthcentersearch.policy.repository.PolicyEmbeddingSyncRepository;
import com.themoa.youthcentersearch.policy.repository.PolicyRepository;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.policy.repository.PolicySourceSnapshotRepository;
import com.themoa.youthcentersearch.policy.region.RegionScope;
import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.service.RegionSynchronizationState;
import com.themoa.youthcentersearch.rag.config.RagProperties;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class AdminStatusService {
    private final PolicyRepository policyRepository;
    private final PolicyEmbeddingSyncRepository syncRepository;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagProperties ragProperties;
    private final LocalSecretConfigurationStatus configurationStatus;
    private final RegionCodeRepository regionCodeRepository;
    private final PolicySourceSnapshotRepository snapshotRepository;
    private final RegionSyncProperties regionSyncProperties;
    private final RegionSynchronizationState regionSynchronizationState;

    public AdminStatusService(PolicyRepository policyRepository,
                              PolicyEmbeddingSyncRepository syncRepository,
                              ObjectProvider<VectorStore> vectorStoreProvider,
                              RagProperties ragProperties,
                              LocalSecretConfigurationStatus configurationStatus,
                              RegionCodeRepository regionCodeRepository,
                              PolicySourceSnapshotRepository snapshotRepository,
                              RegionSyncProperties regionSyncProperties,
                              RegionSynchronizationState regionSynchronizationState) {
        this.policyRepository = policyRepository;
        this.syncRepository = syncRepository;
        this.vectorStoreProvider = vectorStoreProvider;
        this.ragProperties = ragProperties;
        this.configurationStatus = configurationStatus;
        this.regionCodeRepository = regionCodeRepository;
        this.snapshotRepository = snapshotRepository;
        this.regionSyncProperties = regionSyncProperties;
        this.regionSynchronizationState = regionSynchronizationState;
    }

    public AdminStatusResponse status() {
        long totalPolicyCount = 0;
        long activePolicyCount = 0;
        long pendingCount = 0;
        long processingCount = 0;
        long syncedCount = 0;
        long failedCount = 0;
        long nationwidePolicyCount = 0;
        long provincePolicyCount = 0;
        long cityPolicyCount = 0;
        long districtPolicyCount = 0;
        long multiplePolicyCount = 0;
        long unknownPolicyCount = 0;
        long regionTotalCount = 0;
        long regionProvinceCount = 0;
        long regionCityCount = 0;
        long regionDistrictCount = 0;
        long policySnapshotCount = 0;
        long policySnapshotMissingCount = 0;
        boolean mysqlAvailable = true;
        try {
            totalPolicyCount = policyRepository.count();
            activePolicyCount = policyRepository.countByActiveTrue();
            pendingCount = syncRepository.countBySyncStatus("PENDING");
            processingCount = syncRepository.countBySyncStatus("PROCESSING");
            syncedCount = syncRepository.countBySyncStatus("SYNCED");
            failedCount = syncRepository.countBySyncStatus("FAILED");
            ScopeCounts scopeCounts = scopeCounts();
            nationwidePolicyCount = scopeCounts.nationwide;
            provincePolicyCount = scopeCounts.province;
            cityPolicyCount = scopeCounts.city;
            districtPolicyCount = scopeCounts.district;
            multiplePolicyCount = scopeCounts.multiple;
            unknownPolicyCount = scopeCounts.unknown;
            regionTotalCount = regionCodeRepository.count();
            regionProvinceCount = regionCodeRepository.countByRegionLevel("PROVINCE");
            regionCityCount = regionCodeRepository.countByRegionLevel("CITY");
            regionDistrictCount = regionCodeRepository.countByRegionLevel("DISTRICT");
            policySnapshotCount = snapshotRepository.countByPolicyActiveTrue();
            policySnapshotMissingCount = Math.max(0, activePolicyCount - policySnapshotCount);
        } catch (RuntimeException ex) {
            mysqlAvailable = false;
        }
        boolean qdrantAvailable = vectorStoreProvider.getIfAvailable() != null;
        return new AdminStatusResponse(
                "UP",
                configurationStatus.secretConfigFileFound(),
                configurationStatus.youthCenterApiKeyConfigured(),
                configurationStatus.openAiApiKeyConfigured(),
                configurationStatus.springAiChatModel(),
                configurationStatus.springAiEmbeddingModel(),
                configurationStatus.chatModelAvailable(),
                configurationStatus.embeddingModelAvailable(),
                mysqlAvailable,
                mysqlAvailable,
                qdrantAvailable,
                qdrantAvailable,
                configurationStatus.chatModelAvailable(),
                configurationStatus.embeddingModelAvailable(),
                ragProperties.isEnabled(),
                ragProperties.getCollectionName(),
                totalPolicyCount,
                activePolicyCount,
                pendingCount,
                processingCount,
                syncedCount,
                failedCount,
                null,
                policySnapshotCount,
                policySnapshotMissingCount,
                nationwidePolicyCount,
                provincePolicyCount,
                cityPolicyCount,
                districtPolicyCount,
                multiplePolicyCount,
                unknownPolicyCount,
                regionSyncProperties.enabled(),
                regionSyncProperties.credentialsConfigured(),
                regionTotalCount,
                regionProvinceCount,
                regionCityCount,
                regionDistrictCount,
                regionSynchronizationState.lastSyncTime() == null ? null : regionSynchronizationState.lastSyncTime().toString(),
                regionSynchronizationState.lastStatus()
        );
    }

    private ScopeCounts scopeCounts() {
        ScopeCounts counts = new ScopeCounts();
        int page = 0;
        while (true) {
            var ids = policyRepository.findActivePolicyIds(org.springframework.data.domain.PageRequest.of(page++, 500));
            if (ids.isEmpty()) break;
            for (var policy : policyRepository.findWithRelationsByIdIn(ids)) {
                switch (scope(policy)) {
                    case NATIONWIDE -> counts.nationwide++;
                    case PROVINCE -> counts.province++;
                    case CITY -> counts.city++;
                    case DISTRICT -> counts.district++;
                    case MULTIPLE -> counts.multiple++;
                    case UNKNOWN -> counts.unknown++;
                }
            }
        }
        return counts;
    }

    private RegionScope scope(com.themoa.youthcentersearch.policy.domain.Policy policy) {
        var regions = policy.getRegions();
        if (regions.isEmpty()) return RegionScope.UNKNOWN;
        if (regions.stream().anyMatch(region -> "KR".equals(region.getRegion().getRegionCode()))) return RegionScope.NATIONWIDE;
        if (regions.size() > 1) return RegionScope.MULTIPLE;
        String level = regions.iterator().next().getRegion().getRegionLevel();
        return switch (level) {
            case "PROVINCE" -> RegionScope.PROVINCE;
            case "CITY" -> RegionScope.CITY;
            case "DISTRICT" -> RegionScope.DISTRICT;
            default -> RegionScope.UNKNOWN;
        };
    }

    private static class ScopeCounts {
        long nationwide;
        long province;
        long city;
        long district;
        long multiple;
        long unknown;
    }
}
