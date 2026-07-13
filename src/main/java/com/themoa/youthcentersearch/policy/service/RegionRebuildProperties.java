package com.themoa.youthcentersearch.policy.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.region-rebuild")
public class RegionRebuildProperties {
    private int batchSize = 100;
    private boolean enqueueChangedPolicies = true;

    public int getBatchSize() {
        return batchSize;
    }

    public void setBatchSize(int batchSize) {
        this.batchSize = batchSize;
    }

    public boolean isEnqueueChangedPolicies() {
        return enqueueChangedPolicies;
    }

    public void setEnqueueChangedPolicies(boolean enqueueChangedPolicies) {
        this.enqueueChangedPolicies = enqueueChangedPolicies;
    }
}
