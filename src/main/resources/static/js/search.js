(function () {
    const form = document.getElementById("policySearchForm");
    const queryInput = document.getElementById("policySearchQuery");
    const alertBox = document.getElementById("searchAlert");

    document.addEventListener("DOMContentLoaded", () => {
        form.addEventListener("submit", search);
        document.querySelectorAll(".chip").forEach(chip => {
            chip.addEventListener("click", () => {
                queryInput.value = chip.textContent;
                form.requestSubmit();
            });
        });
    });

    async function search(event) {
        event.preventDefault();
        hideAlert();
        document.getElementById("searchSummary").innerHTML = "";
        document.getElementById("diagnostics").innerHTML = "";
        document.getElementById("searchResults").innerHTML = "";
        document.getElementById("answerBox").textContent = "검색 중입니다.";
        const button = form.querySelector("button[type='submit']");
        button.disabled = true;
        try {
            const data = await api.post("/api/policies/search", formData(form));
            render(data);
        } catch (error) {
            showAlert(error.message, true);
            document.getElementById("answerBox").textContent = "검색에 실패했습니다.";
        } finally {
            button.disabled = false;
        }
    }

    function render(data) {
        document.getElementById("answerBox").textContent = data.answer || "요약 없음";
        const c = data.interpretedCondition || {};
        renderDefinition(document.getElementById("searchSummary"), {
            "분석 방식": data.parserMode,
            "검색 모드": modeLabel((data.diagnostics && data.diagnostics.searchMode) || data.searchMode),
            "해석한 지역": regionText(c),
            "지역 단위": regionLevelText(c),
            "지역 후보": regionCandidateText(c),
            "명시 지역 여부": yesNo(c.regionExplicit),
            "나이": c.age,
            "명시 나이 여부": yesNo(c.ageExplicit),
            "취업 상태": c.employmentStatus,
            "명시 취업 상태 여부": yesNo(c.employmentExplicit),
            "학생 상태": c.studentStatus === null || c.studentStatus === undefined ? "" : c.studentStatus,
            "분야": c.category,
            "지원 형태": (c.supportTypes || []).join(", "),
            "핵심 키워드": data.diagnostics && data.diagnostics.coreKeywords,
            "확장 키워드": data.diagnostics && data.diagnostics.expandedKeywords,
            "정규화된 검색 목적": data.diagnostics && data.diagnostics.normalizedGoal,
            "원하는 분야": data.diagnostics && data.diagnostics.desiredDomains,
            "제외 분야": data.diagnostics && data.diagnostics.excludedDomains,
            "긍정 키워드": data.diagnostics && data.diagnostics.positiveKeywords,
            "제외 키워드": data.diagnostics && data.diagnostics.excludedKeywords,
            "명시적 제외 여부": data.diagnostics && yesNo(data.diagnostics.explicitExclusion),
            "Semantic Query": data.diagnostics && data.diagnostics.semanticQuery,
            "Original Vector 사용": data.diagnostics && yesNo(data.diagnostics.originalVectorUsed),
            "Qdrant 정규화 후보": data.diagnostics && data.diagnostics.normalizedVectorCandidateCount,
            "제외 분야 제거": data.diagnostics && data.diagnostics.excludedDomainFilteredCount,
            "사용자 교육 단계": data.diagnostics && data.diagnostics.userEducationStages,
            "교육 단계 명시 여부": data.diagnostics && yesNo(data.diagnostics.educationStageExplicit),
            "대상 단계 불일치 제외": data.diagnostics && data.diagnostics.targetStageMismatchFilteredCount,
            "대상 단계 확인 필요": data.diagnostics && data.diagnostics.targetStageUnknownCount,
            "의도 분석 충돌": data.diagnostics && yesNo(data.diagnostics.semanticConflictDetected),
            "의도 분석 충돌 사유": data.diagnostics && data.diagnostics.semanticConflictReason,
            "Qdrant 후보 수": data.diagnostics && data.diagnostics.vectorCandidateCount,
            "Qdrant 원문 후보": data.diagnostics && data.diagnostics.originalVectorCandidateCount,
            "Qdrant 의도 후보": data.diagnostics && data.diagnostics.intentVectorCandidateCount,
            "Qdrant 확장 후보": data.diagnostics && data.diagnostics.expandedVectorCandidateCount,
            "Qdrant 카테고리 후보": data.diagnostics && data.diagnostics.categoryVectorCandidateCount,
            "MySQL 키워드 후보 수": data.diagnostics && data.diagnostics.lexicalCandidateCount,
            "MySQL 제목 후보": data.diagnostics && data.diagnostics.mysqlTitleCandidateCount,
            "MySQL 키워드 후보": data.diagnostics && data.diagnostics.mysqlKeywordCandidateCount,
            "MySQL 요약 후보": data.diagnostics && data.diagnostics.mysqlSummaryCandidateCount,
            "MySQL 분야 후보": data.diagnostics && data.diagnostics.mysqlCategoryCandidateCount,
            "병합 후보 수": data.diagnostics && data.diagnostics.mergedCandidateCount,
            "중복 후보 수": data.diagnostics && data.diagnostics.duplicateCandidateCount,
            "지역 적격 Pool": data.diagnostics && data.diagnostics.regionEligiblePoolCount,
            "정확 시·군 Pool": data.diagnostics && data.diagnostics.exactSigunguPoolCount,
            "상위 시·도 Pool": data.diagnostics && data.diagnostics.parentSidoPoolCount,
            "전국 Pool": data.diagnostics && data.diagnostics.nationwidePoolCount,
            "복수 지역 Pool": data.diagnostics && data.diagnostics.multipleRegionPoolCount,
            "UNKNOWN 제외": data.diagnostics && data.diagnostics.unknownExcludedCount,
            "다른 지역 제외": data.diagnostics && data.diagnostics.wrongRegionExcludedCount,
            "첫 페이지 정확 지역 보장": data.diagnostics && data.diagnostics.exactSigunguSelectedCount,
            "첫 페이지 상위 시·도 보장": data.diagnostics && data.diagnostics.parentSidoSelectedCount,
            "지역 필터": data.diagnostics && data.diagnostics.regionFilterApplied ? "적용" : "미적용",
            "나이 필터": data.diagnostics && data.diagnostics.ageFilterApplied ? "적용" : "미적용",
            "취업 상태 필터": data.diagnostics && data.diagnostics.employmentFilterApplied ? "적용" : "미적용",
            "시·군·자치구 정확 일치": data.diagnostics && (data.diagnostics.exactSigunguMatchedCount ?? data.diagnostics.cityMatchedCount),
            "시·도 전체 정확 일치": data.diagnostics && data.diagnostics.exactSidoMatchedCount,
            "상위 시·도 전체": data.diagnostics && data.diagnostics.parentSidoMatchedCount,
            "전국 정책": data.diagnostics && (data.diagnostics.nationwideMatchedCount ?? data.diagnostics.nationwideCandidateCount),
            "복수 지역 일치": data.diagnostics && data.diagnostics.multipleRegionMatchedCount,
            "다른 지역 후보": data.diagnostics && data.diagnostics.regionNotMatchedCount,
            "지역 확인 필요 후보": data.diagnostics && data.diagnostics.regionUnknownCount,
            "Topic 통과": data.diagnostics && data.diagnostics.topicThresholdPassedCount,
            "Topic 제외": data.diagnostics && data.diagnostics.topicThresholdFailedCount,
            "Topic 필터 제외": data.diagnostics && data.diagnostics.topicFilteredCount,
            "나이 일치": data.diagnostics && data.diagnostics.ageMatchedCount,
            "나이 확인 필요": data.diagnostics && data.diagnostics.ageUnknownCount,
            "나이 불일치": data.diagnostics && data.diagnostics.ageMismatchedCount,
            "취업 일치": data.diagnostics && data.diagnostics.employmentMatchedCount,
            "취업 확인 필요": data.diagnostics && data.diagnostics.employmentUnknownCount,
            "취업 불일치": data.diagnostics && data.diagnostics.employmentMismatchedCount,
            "조건 통과 수": data.filteredCount,
            "최종 결과 수": data.diagnostics && data.diagnostics.finalResultCount,
            "실패 원인": data.diagnostics && data.diagnostics.fallbackReason,
            "소요 시간": data.diagnostics ? `${data.diagnostics.elapsedTimeMs}ms` : ""
        });
        renderDefinition(document.getElementById("diagnostics"), data.diagnostics || {});
        const target = document.getElementById("searchResults");
        (data.results || []).forEach(item => {
            target.insertAdjacentHTML("beforeend", `<article class="policy-result-card">
                <h3>${safe(item.title)}</h3>
                <div class="meta-list">
                    <span>분야: ${safe(item.category)}</span>
                    <span>주요 정책 분야: ${safe(item.primaryDomain)}</span>
                    <span>보조 정책 분야: ${safe((item.secondaryDomains || []).join(", "))}</span>
                    <span>지원 의도: ${safe((item.supportIntents || []).join(", "))}</span>
                    <span>제외 분야 통과: ${safe(item.excludedDomainPassed ? "통과" : "제외 대상")}</span>
                    <span>정책 대상 교육 단계: ${safe((item.targetEducationStages || []).join(", "))}</span>
                    <span>사용자 단계 일치: ${safe(item.targetStageMatchStatus)} - ${safe(item.targetStageMatchReason)}</span>
                    <span>정책 적용 지역: ${safe(item.region)}</span>
                    <span>지역 판정: ${safe(item.regionMatchLabel || item.regionMatchDescription)}</span>
                    <span>지역 일치 사유: ${safe(item.regionMatchReason || item.regionMatchDescription)}</span>
                    <span>지역 점수: ${safe(item.regionScore)}</span>
                    <span>기관: ${safe(item.agencyName)}</span>
                    <span>나이 조건: ${safe(ageText(item))}</span>
                    <span>취업 조건: ${safe(item.employmentStatus)}</span>
                    <span>신청 기간: ${safe(periodText(item))}</span>
                    <span>신청 상태: ${safe(item.applicationStatus)}</span>
                    <span>검색 관련도: ${safe(item.finalScore)}</span>
                    <span>주제 관련도: ${safe(item.topicScore)}</span>
                </div>
                <p>${safe(item.summary)}</p>
                <strong>조건 일치 이유</strong>
                <ul class="reason-list">${listItems(item.matchedReasons)}</ul>
                <strong>지역 판정 근거</strong>
                <ul class="reason-list">${listItems(item.regionEvidence)}</ul>
                <strong>확인 필요 조건</strong>
                <ul class="reason-list">${listItems(item.needCheckReasons)}</ul>
                <strong>후보 검색 경로</strong>
                <ul class="reason-list">${listItems(item.candidateSources)}</ul>
                <strong>분야 판정 근거</strong>
                <ul class="reason-list">${listItems(item.domainEvidence)}</ul>
                <strong>대상 단계 판정 근거</strong>
                <ul class="reason-list">${listItems(item.targetEducationEvidence)}</ul>
                <div class="button-row">
                    ${item.officialUrl ? `<a class="button-link" href="${escapeAttr(item.officialUrl)}" target="_blank" rel="noreferrer">공식 링크</a>` : ""}
                    <button type="button" class="secondary detail-button" data-id="${escapeAttr(item.policyId)}">상세보기</button>
                </div>
            </article>`);
        });
    }

    function regionText(condition) {
        if (condition.regionResolutionStatus === "AMBIGUOUS") {
            return "지역 확인 필요";
        }
        return [condition.province, condition.city].filter(Boolean).join(" ") || "정보 없음";
    }

    function regionLevelText(condition) {
        if (condition.regionResolutionStatus === "AMBIGUOUS") {
            return "지역 확인 필요";
        }
        if (condition.regionLevel === "SIDO") {
            return "시·도 전체";
        }
        if (condition.regionLevel === "SIGUNGU") {
            return "시·군·자치구";
        }
        return "";
    }

    function regionCandidateText(condition) {
        if (condition.regionResolutionStatus !== "AMBIGUOUS") {
            return "";
        }
        return (condition.regionCandidates || []).join(" / ");
    }

    function formData(formElement) {
        const data = {};
        new FormData(formElement).forEach((value, key) => {
            if (value !== "") {
                data[key] = key === "resultSize" ? Number(value) : value;
            }
        });
        return data;
    }

    function modeLabel(mode) {
        if (mode === "KEYWORD") return "키워드 검색";
        if (mode === "CONDITION") return "조건 맞춤 검색";
        if (mode === "HYBRID") return "키워드 + 조건 검색";
        return mode || "";
    }

    function yesNo(value) {
        return value ? "예" : "아니오";
    }

    function renderDefinition(target, values) {
        target.innerHTML = Object.entries(values)
                .map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${safe(value)}</dd></div>`)
                .join("");
    }

    function ageText(item) {
        if (!item.minAge && !item.maxAge) {
            return "확인 필요";
        }
        return `${item.minAge || "정보 없음"}~${item.maxAge || "정보 없음"}`;
    }

    function periodText(item) {
        if (!item.startDate && !item.dueDate) {
            return "확인 필요";
        }
        return `${item.startDate || ""} ~ ${item.dueDate || ""}`;
    }

    function listItems(values) {
        const items = values || [];
        return items.length ? items.map(value => `<li>${safe(value)}</li>`).join("") : "<li>정보 없음</li>";
    }

    function showAlert(message, danger) {
        alertBox.textContent = message;
        alertBox.className = danger ? "alert danger" : "alert";
    }

    function hideAlert() {
        alertBox.className = "alert hidden";
        alertBox.textContent = "";
    }

    function safe(value) {
        if (value === null || value === undefined || value === "") {
            return "정보 없음";
        }
        return escapeHtml(String(value));
    }

    function escapeAttr(value) {
        return escapeHtml(value || "").replace(/"/g, "&quot;");
    }

    function escapeHtml(value) {
        return String(value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;");
    }
})();
