package com.themoa.youthcentersearch.rag.service;

import org.springframework.stereotype.Component;

@Component
public class PolicyKeywordNormalizer {
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\s·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }
}
