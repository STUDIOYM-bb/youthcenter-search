package com.themoa.youthcentersearch.youthcenter.dto.parsed;

import java.util.List;

public record SchemaAnalysis(
        List<String> rootFields,
        List<String> objectPaths,
        List<CandidateArray> candidateArrays,
        List<String> policyNumberCandidates,
        List<String> policyNameCandidates,
        List<String> totalCountCandidates,
        List<String> currentPageCandidates,
        String xmlRootElement,
        List<String> xmlChildElements,
        List<XmlRepeatedElement> xmlRepeatedElements
) {
    public record CandidateArray(String path, int size, List<String> fields) {
    }

    public record XmlRepeatedElement(String path, int count, List<String> childFields) {
    }
}
