package com.themoa.youthcentersearch.rag.service;

import com.themoa.youthcentersearch.rag.dto.ConditionMatchStatus;

public record EmploymentAudienceMatch(ConditionMatchStatus status, String reason) {
}
