package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.policy.domain.RegionCode;
import com.themoa.youthcentersearch.policy.region.RegionCatalog;
import com.themoa.youthcentersearch.policy.repository.RegionCodeRepository;
import com.themoa.youthcentersearch.admin.service.JobProgressUpdate;
import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import com.themoa.youthcentersearch.region.sgis.SgisApiException;
import com.themoa.youthcentersearch.region.sgis.SgisRegionClient;
import com.themoa.youthcentersearch.region.sgis.dto.SgisRegionItem;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Service
public class RegionSynchronizationService {
    private final RegionSyncProperties properties;
    private final SgisRegionClient regionClient;
    private final RegionCodeRepository regionCodeRepository;
    private final RegionCatalog regionCatalog;
    private final RegionSynchronizationState state;
    private final TransactionTemplate transactionTemplate;
    private final MunicipalityHierarchyResolver hierarchyResolver;

    public RegionSynchronizationService(RegionSyncProperties properties,
                                        SgisRegionClient regionClient,
                                        RegionCodeRepository regionCodeRepository,
                                        RegionCatalog regionCatalog,
                                        RegionSynchronizationState state,
                                        TransactionTemplate transactionTemplate,
                                        MunicipalityHierarchyResolver hierarchyResolver) {
        this.properties = properties;
        this.regionClient = regionClient;
        this.regionCodeRepository = regionCodeRepository;
        this.regionCatalog = regionCatalog;
        this.state = state;
        this.transactionTemplate = transactionTemplate;
        this.hierarchyResolver = hierarchyResolver;
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
        List<String> failedProvinceCodes = new ArrayList<>();
        notify(progressConsumer, new JobProgressUpdate("AUTHENTICATING", "SGIS 인증 중", 0, 0, 0, 0, 0, 0, 0, 0, null, 0, 0, "SGIS 인증 중입니다."));
        List<SgisRegionItem> provinces = regionClient.fetchProvinces();
        Counter counter = new Counter();
        counter.provinceReceived = provinces.size();

        int processedProvince = 0;
        int successProvince = 0;
        for (SgisRegionItem province : provinces) {
            if (province == null || province.cd() == null || province.addrName() == null) {
                counter.failed++;
                continue;
            }
            RegionCode savedProvince = transactionTemplate.execute(status ->
                    upsert(null, province.cd(), province.addrName(), null, "PROVINCE", counter));
            try {
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
                    transactionTemplate.execute(status -> {
                        upsertMunicipality(savedProvince, normalized, counter);
                        return null;
                    });
                }
            } catch (RuntimeException ex) {
                counter.failed++;
                failedProvinceCodes.add(province.cd());
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
        state.completed(result);
        return result;
    }

    private RegionCode upsert(RegionCode parent, String code, String province, String city, String level, Counter counter) {
        return regionCodeRepository.findByRegionCode(code)
                .map(existing -> {
                    if (existing.update(parent, province, city, level)) {
                        counter.updated++;
                    } else {
                        counter.unchanged++;
                    }
                    return existing;
                })
                .orElseGet(() -> {
                    counter.inserted++;
                    return regionCodeRepository.save(new RegionCode(parent, code, province, city, level));
                });
    }

    private RegionCode upsertMunicipality(RegionCode parent, NormalizedMunicipality normalized, Counter counter) {
        return regionCodeRepository.findByProvinceAndCity(normalized.provinceName(), normalized.municipalityName()).stream()
                .filter(region -> "CITY".equals(region.getRegionLevel()))
                .findFirst()
                .map(existing -> {
                    counter.unchanged++;
                    return existing;
                })
                .orElseGet(() -> upsert(parent, normalized.sourceCode(), normalized.provinceName(), normalized.municipalityName(), "CITY", counter));
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
    }
}
