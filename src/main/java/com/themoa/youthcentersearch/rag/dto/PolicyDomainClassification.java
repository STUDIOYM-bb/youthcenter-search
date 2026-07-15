package com.themoa.youthcentersearch.rag.dto;

import java.util.List;
import java.util.Set;

public record PolicyDomainClassification(
        SearchDomain primaryDomain,
        Set<SearchDomain> secondaryDomains,
        Set<SupportIntent> supportIntents,
        double confidence,
        List<String> evidence
) {
    public PolicyDomainClassification {
        primaryDomain = primaryDomain == null ? SearchDomain.GENERAL : primaryDomain;
        secondaryDomains = secondaryDomains == null ? Set.of() : Set.copyOf(secondaryDomains);
        supportIntents = supportIntents == null ? Set.of() : Set.copyOf(supportIntents);
        evidence = evidence == null ? List.of() : List.copyOf(evidence);
    }
}
