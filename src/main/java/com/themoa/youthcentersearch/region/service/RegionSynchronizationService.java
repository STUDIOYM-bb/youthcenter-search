package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.domain.RegionSyncError;
import com.themoa.youthcentersearch.policy.domain.RegionSyncRun;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.repository.RegionExternalCodeRepository;
import com.themoa.youthcentersearch.policy.repository.RegionSyncErrorRepository;
import com.themoa.youthcentersearch.policy.repository.RegionSyncRunRepository;
import com.themoa.youthcentersearch.admin.service.JobProgressUpdate;
import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.SgisApiException;
import com.themoa.youthcentersearch.region.sgis.SgisRegionClient;
import com.themoa.youthcentersearch.region.sgis.dto.SgisRegionItem;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class RegionSynchronizationService {
    private final RegionSyncProperties properties;
    private final SgisRegionClient regionClient;
    private final RegionCatalog regionCatalog;
    private final RegionSynchronizationState state;
    private final MunicipalityHierarchyResolver hierarchyResolver;
    private final RegionProvincePersistenceService persistenceService;
    private final RegionSyncRunRepository syncRunRepository;
    private final RegionSyncErrorRepository syncErrorRepository;
    private final RegionExternalCodeRepository externalCodeRepository;

    public RegionSynchronizationService(RegionSyncProperties properties,
                                        SgisRegionClient regionClient,
                                        RegionCatalog regionCatalog,
                                        RegionSynchronizationState state,
                                        MunicipalityHierarchyResolver hierarchyResolver,
                                        RegionProvincePersistenceService persistenceService,
                                        RegionSyncRunRepository syncRunRepository,
                                        RegionSyncErrorRepository syncErrorRepository,
                                        RegionExternalCodeRepository externalCodeRepository) {
        this.properties = properties;
        this.regionClient = regionClient;
        this.regionCatalog = regionCatalog;
        this.state = state;
        this.hierarchyResolver = hierarchyResolver;
        this.persistenceService = persistenceService;
        this.syncRunRepository = syncRunRepository;
        this.syncErrorRepository = syncErrorRepository;
        this.externalCodeRepository = externalCodeRepository;
    }

    public RegionSynchronizationResult synchronize() {
        return synchronize(null);
    }

    public RegionSynchronizationResult synchronize(Consumer<JobProgressUpdate> progressConsumer) {
        try {
            return doSynchronize(progressConsumer);
        } catch (RuntimeException ex) {
            state.failed(ex.getMessage());
            throw ex;
        }
    }

    private RegionSynchronizationResult doSynchronize(Consumer<JobProgressUpdate> progressConsumer) {
        if (!properties.enabled()) {
            throw new SgisApiException("SGIS 지역 동기화가 비활성화되어 있습니다.");
        }
        if (!properties.credentialsConfigured()) {
            throw new SgisApiException("SGIS 인증 정보가 설정되지 않았습니다.");
        }
        Instant startedAt = Instant.now();
        RegionSyncRun run = syncRunRepository.save(RegionSyncRun.start());
        List<String> failedProvinceCodes = new ArrayList<>();
        notify(progressConsumer, new JobProgressUpdate("AUTHENTICATING", "SGIS 인증 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "SGIS 인증 중입니다."));
        List<SgisRegionItem> provinces = regionClient.fetchProvinces();
        Counter counter = new Counter();
        counter.provinceReceived = provinces.size();
        run.provinceCount(provinces.size());
        syncRunRepository.save(run);

        int processedProvince = 0;
        int successProvince = 0;
        for (SgisRegionItem province : provinces) {
            if (province == null || province.cd() == null || province.addrName() == null) {
                counter.failed++;
                continue;
            }
            RegionCode savedProvince;
            try {
                var persisted = persistenceService.upsertProvince(province.cd(), province.addrName());
                savedProvince = persisted.region();
                counter.add(persisted);
            } catch (RuntimeException ex) {
                counter.failed++;
                failedProvinceCodes.add(province.cd());
                syncErrorRepository.save(new RegionSyncError(run, province.cd(), province.addrName(), ex.getClass().getSimpleName(), ex.getMessage(), 0));
                continue;
            }
            try {
                run.current(province.cd(), province.addrName(), processedProvince);
                syncRunRepository.save(run);
                notify(progressConsumer, new JobProgressUpdate("SYNCING_CHILDREN", "시·군 동기화 중", provinces.size(), processedProvince,
                        successProvince, counter.failed, 0, 0, 0, 0, province.addrName(), 0, 0, province.addrName() + " 하위 지역 동기화 중"));
                sleep();
                List<SgisRegionItem> children = regionClient.fetchChildren(province.cd());
                counter.childReceived += children.size();
                for (SgisRegionItem child : children) {
                    if (child == null || child.cd() == null || child.addrName() == null) {
                        counter.failed++;
                        continue;
                    }
                    NormalizedMunicipality normalized = hierarchyResolver.normalize(province.addrName(), child.cd(), child.addrName(), child.fullAddr());
                    if (!normalized.supported()) {
                        continue;
                    }
                    var persisted = persistenceService.upsertMunicipality(savedProvince, normalized);
                    counter.add(persisted);
                }
            } catch (RuntimeException ex) {
                counter.failed++;
                failedProvinceCodes.add(province.cd());
                syncErrorRepository.save(new RegionSyncError(run, province.cd(), province.addrName(), ex.getClass().getSimpleName(), ex.getMessage(), 0));
            }
            processedProvince++;
            if (!failedProvinceCodes.contains(province.cd())) {
                successProvince++;
            }
            notify(progressConsumer, new JobProgressUpdate("SYNCING_CHILDREN", "시·군 동기화 중", provinces.size(), processedProvince,
                    successProvince, counter.failed, 0, 0, 0, 0, province.addrName(), 0, 0, province.addrName() + " 처리 완료"));
        }

        notify(progressConsumer, new JobProgressUpdate("REFRESHING_CACHE", "지역 캐시 갱신 중", provinces.size(), provinces.size(),
                successProvince, counter.failed, 0, 0, 0, 0, null, 0, 0, "지역 캐시를 갱신합니다."));
        regionCatalog.refreshCache();
        run.counts(counter.childReceived, counter.inserted, counter.updated, counter.unchanged, counter.failed);
        String status = counter.failed == 0 ? "COMPLETED" : successProvince == 0 ? "FAILED" : "COMPLETED_WITH_ERRORS";
        run.complete(status, failedProvinceCodes.isEmpty() ? null : "failedProvinceCodes=" + failedProvinceCodes);
        syncRunRepository.save(run);
        RegionSynchronizationResult result = new RegionSynchronizationResult(
                counter.provinceReceived,
                counter.childReceived,
                counter.inserted,
                counter.updated,
                counter.unchanged,
                counter.failed,
                List.copyOf(failedProvinceCodes),
                Duration.between(startedAt, Instant.now()).toMillis()
        );
        state.completed(result, status);
        return result;
    }

    private void sleep() {
        if (properties.requestDelay().isZero() || properties.requestDelay().isNegative()) {
            return;
        }
        try {
            Thread.sleep(properties.requestDelay().toMillis());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SgisApiException("SGIS 지역 동기화가 중단되었습니다.", ex);
        }
    }

    private void notify(Consumer<JobProgressUpdate> consumer, JobProgressUpdate update) {
        if (consumer != null) {
            consumer.accept(update);
        }
    }

    private static class Counter {
        int provinceReceived;
        int childReceived;
        int inserted;
        int updated;
        int unchanged;
        int failed;

        void add(RegionProvincePersistenceService.PersistedRegion persisted) {
            inserted += persisted.inserted();
            updated += persisted.updated();
            unchanged += persisted.unchanged();
        }
    }
}
