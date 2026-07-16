package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.PolicySearchDiagnostics;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PolicySearchDiagnosticsBuilderTest {
    @Test
    void mapsNamedFieldsToDiagnosticsRecordPositions() {
        PolicySearchDiagnostics diagnostics = PolicySearchDiagnosticsBuilder.builder()
                .vectorCandidateCount(10)
                .lexicalCandidateCount(8)
                .mysqlTitleCandidateCount(3)
                .regionFilteredCount(2)
                .ageFilteredCount(1)
                .employmentFilteredCount(4)
                .targetFilteredCount(5)
                .primaryCandidateCount(6)
                .needsConfirmationCandidateCount(7)
                .finalResultCount(9)
                .elapsedTimeMs(123)
                .build();

        assertThat(diagnostics.vectorCandidateCount()).isEqualTo(10);
        assertThat(diagnostics.lexicalCandidateCount()).isEqualTo(8);
        assertThat(diagnostics.mysqlTitleCandidateCount()).isEqualTo(3);
        assertThat(diagnostics.regionFilteredCount()).isEqualTo(2);
        assertThat(diagnostics.ageFilteredCount()).isEqualTo(1);
        assertThat(diagnostics.employmentFilteredCount()).isEqualTo(4);
        assertThat(diagnostics.targetFilteredCount()).isEqualTo(5);
        assertThat(diagnostics.primaryCandidateCount()).isEqualTo(6);
        assertThat(diagnostics.needsConfirmationCandidateCount()).isEqualTo(7);
        assertThat(diagnostics.finalResultCount()).isEqualTo(9);
        assertThat(diagnostics.elapsedTimeMs()).isEqualTo(123);
    }
}
