package com.themoa.youthcentersearch.rag.dto;

/**
 * 사용자 또는 정책 대상의 학교 단계다.
 * 기존 Boolean studentStatus는 유지하되, 고교생/대학생처럼 서로 배타적인 대상 판정은 이 enum으로 분리한다.
 */
public enum EducationStage {
    ELEMENTARY,
    MIDDLE_SCHOOL,
    HIGH_SCHOOL,
    UNIVERSITY,
    GRADUATE_SCHOOL,
    ALL_STUDENTS,
    GENERAL_YOUTH,
    UNKNOWN
}
