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
            "해석한 지역": [c.province, c.city, c.district].filter(Boolean).join(" "),
            "나이": c.age,
            "취업 상태": c.employmentStatus,
            "학생 상태": c.studentStatus === null || c.studentStatus === undefined ? "" : c.studentStatus,
            "분야": c.category,
            "지원 형태": (c.supportTypes || []).join(", "),
            "검색 방식": data.searchMode,
            "Qdrant 후보 수": data.diagnostics && data.diagnostics.vectorCandidateCount,
            "조건 통과 수": data.filteredCount,
            "MySQL 보완 여부": data.diagnostics && data.diagnostics.mysqlFallbackUsed ? "사용" : "미사용",
            "최종 결과 수": data.diagnostics && data.diagnostics.finalResultCount,
            "소요 시간": data.diagnostics ? `${data.diagnostics.elapsedTimeMs}ms` : ""
        });
        renderDefinition(document.getElementById("diagnostics"), data.diagnostics || {});
        const target = document.getElementById("searchResults");
        (data.results || []).forEach(item => {
            target.insertAdjacentHTML("beforeend", `<article class="policy-result-card">
                <h3>${safe(item.title)}</h3>
                <div class="meta-list">
                    <span>분야: ${safe(item.category)}</span>
                    <span>지역: ${safe(item.region)}</span>
                    <span>기관: ${safe(item.agencyName)}</span>
                    <span>나이 조건: ${safe(ageText(item))}</span>
                    <span>취업 조건: ${safe(item.employmentStatus)}</span>
                    <span>신청 기간: ${safe(periodText(item))}</span>
                    <span>신청 상태: ${safe(item.applicationStatus)}</span>
                    <span>검색 관련도: ${safe(item.finalScore)}</span>
                </div>
                <p>${safe(item.summary)}</p>
                <strong>조건 일치 이유</strong>
                <ul class="reason-list">${listItems(item.matchedReasons)}</ul>
                <strong>확인 필요 조건</strong>
                <ul class="reason-list">${listItems(item.needCheckReasons)}</ul>
                <div class="button-row">
                    ${item.officialUrl ? `<a class="button-link" href="${escapeAttr(item.officialUrl)}" target="_blank" rel="noreferrer">공식 링크</a>` : ""}
                    <button type="button" class="secondary detail-button" data-id="${escapeAttr(item.policyId)}">상세보기</button>
                </div>
            </article>`);
        });
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

    function renderDefinition(target, values) {
        target.innerHTML = Object.entries(values).map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${safe(value)}</dd></div>`).join("");
    }

    function ageText(item) {
        if (!item.minAge && !item.maxAge) return "확인 필요";
        return `${item.minAge || "정보 없음"}~${item.maxAge || "정보 없음"}`;
    }

    function periodText(item) {
        if (!item.startDate && !item.dueDate) return "확인 필요";
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
        if (value === null || value === undefined || value === "") return "정보 없음";
        return escapeHtml(String(value));
    }

    function escapeAttr(value) {
        return escapeHtml(value || "").replace(/"/g, "&quot;");
    }

    function escapeHtml(value) {
        return String(value).replace(/&/g, "&amp;").replace(/</g, "&lt;").replace(/>/g, "&gt;");
    }
})();
