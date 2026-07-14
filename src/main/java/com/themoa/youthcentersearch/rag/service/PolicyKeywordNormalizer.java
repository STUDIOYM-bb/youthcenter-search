package com.themoa.youthcentersearch.rag.service;

import org.springframework.stereotype.Component;

@Component
public class PolicyKeywordNormalizer {
    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase()
                .replaceAll("[\\s\\-\\u2010\\u2011\\u2012\\u2013\\u2014\\u2212·ㆍ,()\\[\\]{}<>\"'`~!@#$%^&*_=+|\\\\:;?/.]", "");
    }
}
