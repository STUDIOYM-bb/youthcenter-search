package com.themoa.youthcentersearch.youthcenter.dto.request;

import com.themoa.youthcentersearch.youthcenter.config.YouthCenterApiProperties;
import jakarta.validation.constraints.NotBlank;

public record YouthPolicyDetailRequest(
        @NotBlank String policyNumber,
        String returnType
) {
    public String effectiveReturnType(YouthCenterApiProperties properties) {
        return returnType == null || returnType.isBlank() ? properties.getDefaultReturnType() : returnType.toLowerCase();
    }
}
