package com.themoa.youthcentersearch.region.service;

import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class RegionSynchronizationScheduler implements ApplicationRunner {
    private final RegionSyncProperties properties;
    private final RegionSynchronizationService synchronizationService;
    private final RegionSynchronizationState state;

    public RegionSynchronizationScheduler(RegionSyncProperties properties,
                                          RegionSynchronizationService synchronizationService,
                                          RegionSynchronizationState state) {
        this.properties = properties;
        this.synchronizationService = synchronizationService;
        this.state = state;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (properties.enabled() && properties.syncOnStartup()) {
            runSafely();
        }
    }

    @Scheduled(cron = "${app.region-sync.cron:0 0 4 1 * *}")
    public void scheduledSync() {
        if (properties.enabled()) {
            runSafely();
        }
    }

    private void runSafely() {
        try {
            synchronizationService.synchronize();
        } catch (RuntimeException ex) {
            state.failed(ex.getMessage());
        }
    }
}
