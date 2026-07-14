package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.region.RegionCompatibility;
import com.themoa.youthcentersearch.rag.dto.PolicySearchResultItem;
import com.themoa.youthcentersearch.rag.dto.SearchQueryType;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;

@Component
public class RegionCoverageResultSelector {

    public Selection select(List<PolicySearchResultItem> sortedResults, int page, int size, SearchQueryType queryType) {
        if (page != 0 || sortedResults.isEmpty()
                || queryType == SearchQueryType.POLICY_NAME
                || queryType == SearchQueryType.TOPIC_SEARCH) {
            return new Selection(sortedResults, 0, 0, 0);
        }
        LinkedHashSet<Integer> promoted = new LinkedHashSet<>();
        int exact = promote(sortedResults, promoted, RegionCompatibility.EXACT_SIGUNGU);
        int parent = promote(sortedResults, promoted, RegionCompatibility.PARENT_SIDO);
        if (parent == 0) {
            parent = promote(sortedResults, promoted, RegionCompatibility.EXACT_SIDO);
        }
        List<PolicySearchResultItem> reordered = new ArrayList<>();
        promoted.forEach(id -> sortedResults.stream()
                .filter(item -> item.policyId().equals(id))
                .findFirst()
                .ifPresent(reordered::add));
        sortedResults.stream()
                .filter(item -> !promoted.contains(item.policyId()))
                .forEach(reordered::add);
        int nationwideSelected = (int) reordered.stream()
                .limit(size)
                .filter(item -> RegionCompatibility.NATIONWIDE.name().equals(item.regionCompatibility()))
                .count();
        return new Selection(reordered, exact, parent, nationwideSelected);
    }

    private int promote(List<PolicySearchResultItem> results, LinkedHashSet<Integer> promoted, RegionCompatibility compatibility) {
        return results.stream()
                .filter(item -> compatibility.name().equals(item.regionCompatibility()))
                .max(Comparator.comparingDouble(PolicySearchResultItem::finalScore))
                .map(item -> {
                    promoted.add(item.policyId());
                    return 1;
                })
                .orElse(0);
    }

    public record Selection(List<PolicySearchResultItem> orderedResults,
                            int exactSigunguSelectedCount,
                            int parentSidoSelectedCount,
                            int nationwideSelectedCount) {
    }
}
