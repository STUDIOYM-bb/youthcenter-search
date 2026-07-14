package com.themoa.youthcentersearch.rag.dto;

import org.springframework.util.StringUtils;

public enum SearchDomain {
    EMPLOYMENT("청년 일자리 취업 구직 지원 정책"),
    HOUSING("청년 주거 월세 전세 임차 지원 정책"),
    EDUCATION("청년 교육 훈련 역량 강화 정책"),
    WELFARE("청년 복지 생활 안정 지원 정책"),
    FINANCE("청년 금융 저축 계좌 자산형성 지원 정책"),
    STARTUP("청년 창업 사업화 지원 정책"),
    CULTURE("청년 문화 예술 활동 지원 정책"),
    HEALTH("청년 건강 의료 심리 지원 정책"),
    CARE("청년 돌봄 가족 지원 정책"),
    GENERAL("청년 지원 정책");

    private final String koreanQuery;

    SearchDomain(String koreanQuery) {
        this.koreanQuery = koreanQuery;
    }

    public String koreanQuery() {
        return koreanQuery;
    }

    public static SearchDomain fromRaw(String value) {
        if (!StringUtils.hasText(value)) return GENERAL;
        String lower = value.toLowerCase();
        if (containsAny(lower, "employment", "job", "일자리", "취업", "구직", "면접")) return EMPLOYMENT;
        if (containsAny(lower, "housing", "주거", "월세", "전세", "임차")) return HOUSING;
        if (containsAny(lower, "education", "교육", "훈련", "역량")) return EDUCATION;
        if (containsAny(lower, "welfare", "복지", "생활", "수당")) return WELFARE;
        if (containsAny(lower, "financial", "finance", "금융", "저축", "계좌", "자산", "대출")) return FINANCE;
        if (containsAny(lower, "startup", "창업", "사업화")) return STARTUP;
        if (containsAny(lower, "culture", "문화", "예술")) return CULTURE;
        if (containsAny(lower, "health", "건강", "의료", "심리")) return HEALTH;
        if (containsAny(lower, "care", "돌봄", "가족")) return CARE;
        return GENERAL;
    }

    private static boolean containsAny(String value, String... needles) {
        for (String needle : needles) {
            if (value.contains(needle)) return true;
        }
        return false;
    }
}
