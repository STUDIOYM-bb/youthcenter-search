package com.themoa.youthcentersearch.rag.dto;

/**
 * 사용자가 현재 어떤 취업 상태인지 나타낸다.
 * 정책 분야 EMPLOYMENT 선호와 별개로, 신청 자격 hard filter에만 사용한다.
 */
public enum UserEmploymentStatus {
    EMPLOYED,
    UNEMPLOYED,
    UNKNOWN
}
