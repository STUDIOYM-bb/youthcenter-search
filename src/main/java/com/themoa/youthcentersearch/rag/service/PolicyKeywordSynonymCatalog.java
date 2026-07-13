package com.themoa.youthcentersearch.rag.service;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class PolicyKeywordSynonymCatalog {
    private final Map<String, List<String>> synonyms = new LinkedHashMap<>();

    public PolicyKeywordSynonymCatalog() {
        synonyms.put("면접수당", List.of("면접 수당", "면접수당", "면접비", "면접 비용", "면접 지원금", "구직 면접비"));
        synonyms.put("월세", List.of("월세", "임차료", "주거비", "월 임대료"));
        synonyms.put("지원금", List.of("지원금", "수당", "보조금", "장려금"));
        synonyms.put("자산형성", List.of("자산형성", "목돈", "저축 지원", "매칭 저축"));
    }

    public Map<String, List<String>> synonyms() {
        return synonyms;
    }
}
