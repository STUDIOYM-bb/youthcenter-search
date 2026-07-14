package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.policy.repository.PolicySearchProjectionRepository;
import org.springframework.stereotype.Component;

@Component
public class PolicyLexicalIndexBuilder {
    private final PolicySearchProjectionRepository projectionRepository;
    private final PolicyKeywordNormalizer normalizer;
    private volatile PolicyLexicalIndex index;

    public PolicyLexicalIndexBuilder(PolicySearchProjectionRepository projectionRepository,
                                     PolicyKeywordNormalizer normalizer) {
        this.projectionRepository = projectionRepository;
        this.normalizer = normalizer;
    }

    public PolicyLexicalIndex current() {
        PolicyLexicalIndex cached = index;
        if (cached != null) {
            return cached;
        }
        synchronized (this) {
            if (index == null) {
                index = new PolicyLexicalIndex(projectionRepository.findAllActive(), normalizer);
            }
            return index;
        }
    }

    public PolicyLexicalIndex refresh() {
        synchronized (this) {
            index = new PolicyLexicalIndex(projectionRepository.findAllActive(), normalizer);
            return index;
        }
    }
}
