package com.themoa.youthcentersearch.policy.region;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

@Component
public class PolicyRegionResolver {
    private final PolicyGeographyClassifier geographyClassifier;

    @Autowired
    public PolicyRegionResolver(RegionCatalog catalog, InstitutionRegionResolver institutionResolver,
                                PolicyGeographyClassifier geographyClassifier) {
        this.geographyClassifier = geographyClassifier;
    }

    public PolicyRegionResolver(RegionCatalog catalog, InstitutionRegionResolver institutionResolver) {
        this(catalog, institutionResolver, new PolicyGeographyClassifier(catalog,
                new StrictPolicyRegionMentionExtractor(catalog, new RegionNameAliasGenerator(),
                        new RegionNormalizer(new RegionAliasCatalog())),
                institutionResolver));
    }

    public PolicyRegionResolution resolve(Map<String, Object> fields) {
        return geographyClassifier.classify(fields).toResolution();
    }
}
