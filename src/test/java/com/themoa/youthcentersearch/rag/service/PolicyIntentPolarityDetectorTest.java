package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.SearchDomain;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicyIntentPolarityDetectorTest {
    private final PolicyIntentPolarityDetector detector = new PolicyIntentPolarityDetector();

    @Test
    void detectsEmploymentExclusionWithoutTreatingItAsDesiredEmployment() {
        var result = detector.detect("""
                난 수원에 살고 있고 22살 대학생이야.
                현재 대학교 근로소득은 없는데 내가 받을 수 있는 정책 혜택이 있을까?
                단 취업 생각은 아직 없어.
                """);

        assertThat(result.excludedDomains()).contains(SearchDomain.EMPLOYMENT);
        assertThat(result.desiredDomains()).doesNotContain(SearchDomain.EMPLOYMENT);
        assertThat(result.positiveTerms()).contains("대학생");
        assertThat(result.excludedTerms()).contains("취업");
    }

    @Test
    void keepsPositiveEmploymentRequestSeparateFromEmploymentStateNegation() {
        var result = detector.detect("현재 취업은 안 했지만 취업 지원 정책을 찾고 있어.");

        assertThat(result.desiredDomains()).contains(SearchDomain.EMPLOYMENT);
        assertThat(result.excludedDomains()).doesNotContain(SearchDomain.EMPLOYMENT);
    }

    @Test
    void detectsDomainReplacement() {
        var result = detector.detect("취업 정책 말고 대학생 월세 정책을 찾아줘.");

        assertThat(result.excludedDomains()).contains(SearchDomain.EMPLOYMENT);
        assertThat(result.desiredDomains()).contains(SearchDomain.HOUSING);
        assertThat(result.positiveTerms()).contains("대학생", "월세");
        assertThat(result.excludedTerms()).contains("취업");
    }

    @Test
    void handlesOtherExcludedDomains() {
        assertThat(detector.detect("창업은 관심 없고 주거 지원").excludedDomains()).contains(SearchDomain.STARTUP);
        assertThat(detector.detect("창업은 관심 없고 주거 지원").desiredDomains()).contains(SearchDomain.HOUSING);
        assertThat(detector.detect("교육 프로그램 제외하고 현금성 지원").excludedDomains()).contains(SearchDomain.EDUCATION);
        assertThat(detector.detect("월세 정책은 필요 없고 취업 지원").excludedDomains()).contains(SearchDomain.HOUSING);
        assertThat(detector.detect("월세 정책은 필요 없고 취업 지원").desiredDomains()).contains(SearchDomain.EMPLOYMENT);
    }
}
