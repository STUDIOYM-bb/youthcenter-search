(function () {
    const rawPreview = document.getElementById("rawPreview");

    document.addEventListener("DOMContentLoaded", () => {
        const savedKey = sessionStorage.getItem("adminKey");
        if (savedKey) {
            document.getElementById("adminKeyInput").value = savedKey;
        }
        bindEvents();
        loadStatus();
    });

    function bindEvents() {
        document.getElementById("refreshStatusBtn").addEventListener("click", loadStatus);
        document.getElementById("saveAdminKeyBtn").addEventListener("click", () => {
            sessionStorage.setItem("adminKey", document.getElementById("adminKeyInput").value);
            showAlert("probeAlert", "愿由ъ옄 ?ㅻ? sessionStorage????ν뻽?듬땲??", false);
        });
        document.getElementById("clearResultBtn").addEventListener("click", clearResults);
        document.querySelector("[data-action='probe']").addEventListener("click", withButton(probe, "probeAlert"));
        document.querySelector("[data-action='legacyProbe']").addEventListener("click", withButton(legacyProbe, "probeAlert"));
        document.getElementById("filterProbeForm").addEventListener("submit", withForm(filterProbe, "filterProbeAlert"));
        document.getElementById("searchForm").addEventListener("submit", withForm(searchPolicies, "listAlert"));
        document.getElementById("detailForm").addEventListener("submit", withForm(detailPolicy, "detailAlert"));
        document.getElementById("paginationForm").addEventListener("submit", withForm(paginationTest, "paginationAlert"));
        document.getElementById("naturalSearchForm").addEventListener("submit", withForm(naturalSearch, "naturalSearchAlert"));

        document.querySelectorAll("#naturalExampleChips .chip").forEach(chip => {
            chip.addEventListener("click", () => {
                document.getElementById("naturalQueryInput").value = chip.textContent;
            });
        });

        document.querySelectorAll(".filter-example").forEach(button => {
            button.addEventListener("click", () => {
                const form = document.getElementById("filterProbeForm");
                form.elements.filterType.value = button.dataset.type;
                form.elements.value.value = button.dataset.value;
                form.requestSubmit();
            });
        });
    }

    async function loadStatus() {
        try {
            const data = await api.get("/api/youth-center/status");
            renderDefinition(document.getElementById("statusGrid"), {
                "Spring Boot": data.application,
                "API 紐⑤뱶": data.apiMode,
                "API Key": data.apiKeyConfigured ? "?ㅼ젙?? : "誘몄꽕??,
                "Base URL": data.baseUrl,
                "Current Path": data.path,
                "Redirect ?먮룞 異붿쟻": data.followRedirects ? "?ъ슜" : "誘몄궗??,
                "?먮낯 ?묐떟 ???: data.rawResponseSaveEnabled ? "?ъ슜" : "誘몄궗??
            });
        } catch (error) {
            showAlert("probeAlert", error.message, true);
        }
    }

    async function probe() {
        hideAlert("probeAlert");
        const data = await api.post("/api/youth-center/probe", {});
        renderProbe(data);
    }

    async function legacyProbe() {
        hideAlert("probeAlert");
        const data = await api.post("/api/youth-center/legacy/probe", {});
        renderProbe(data);
    }

    async function filterProbe(form) {
        hideAlert("filterProbeAlert");
        const data = await api.post("/api/youth-center/filter-probe", formData(form));
        appendFilterProbeResult(data);
        if (data.errorMessage) {
            showAlert("filterProbeAlert", data.errorMessage, true);
        }
    }

    async function searchPolicies(form) {
        hideAlert("listAlert");
        const data = await api.post("/api/youth-center/policies/search", formData(form));
        renderList(data);
        renderProbe(data);
    }

    async function detailPolicy(form) {
        hideAlert("detailAlert");
        const data = await api.post("/api/youth-center/policies/detail", formData(form));
        renderDetail(data);
        renderProbe(data);
    }

    async function paginationTest(form) {
        hideAlert("paginationAlert");
        const data = await api.post("/api/youth-center/pagination-test", formData(form));
        renderDefinition(document.getElementById("paginationSummary"), {
            "?붿껌 ?섏씠吏 ??: data.requestedPages,
            "API ?붿껌 ??: data.apiRequestCount,
            "?뚯떛 ?뺤콉 ??: data.parsedPolicyCount,
            "怨좎쑀 ?뺤콉 ??: data.uniquePolicyCount,
            "以묐났 ?뺤콉 ??: data.duplicatePolicyCount,
            "諛섎났 ?섏씠吏 媛먯?": data.repeatedPageDetected ? "?? : "?꾨땲??,
            "醫낅즺 ?ъ쑀": data.stopReason
        });
        const tbody = document.querySelector("#paginationTable tbody");
        tbody.innerHTML = "";
        (data.pages || []).forEach(row => {
            tbody.insertAdjacentHTML("beforeend", `<tr><td>${safe(row.page)}</td><td>${safe(row.statusCode)}</td><td>${safe(row.receivedCount)}</td><td>${safe(row.firstPolicyNumber)}</td><td>${safe(row.lastPolicyNumber)}</td><td>${row.duplicate ? "?? : "?꾨땲??}</td><td>${safe(row.elapsedTimeMs)}ms</td></tr>`);
        });
    }

    async function naturalSearch(form) {
        hideAlert("naturalSearchAlert");
        document.getElementById("naturalSummary").innerHTML = "";
        document.getElementById("naturalResults").innerHTML = "";
        const requestTable = document.getElementById("naturalGeneratedRequests");
        requestTable.classList.add("hidden");
        requestTable.querySelector("tbody").innerHTML = "";

        const data = await api.post("/api/youth-center/natural-language/search", formData(form));
        if (data.partialSuccess) {
            showAlert("naturalSearchAlert", "?쇰? 寃??議곌굔 ?붿껌???ㅽ뙣?섏뿬 ?깃났??寃??寃곌낵留??쒖떆?⑸땲??", false);
        } else if ((data.warnings || []).length > 0) {
            showAlert("naturalSearchAlert", data.warnings.join(" "), false);
        }

        const condition = data.interpretedCondition || {};
        renderDefinition(document.getElementById("naturalSummary"), {
            "議곌굔 遺꾩꽍 諛⑹떇": data.parserMode,
            "fallback ?щ?": data.fallback ? "?? : "?꾨땲??,
            "fallback ?ъ쑀": data.fallbackReason,
            "?댁꽍??吏??: condition.region,
            "?섏씠": condition.age,
            "痍⑥뾽 ?곹깭": condition.employmentStatus,
            "?숈깮 ?곹깭": condition.studentStatus,
            "愿??遺꾩빞": condition.category,
            "?듭떖 ?ㅼ썙??: (condition.keywords || []).join(", "),
            "API ?붿껌 ??: data.apiRequestCount,
            "?깃났 ?붿껌 ??: data.successfulApiRequestCount,
            "?ㅽ뙣 ?붿껌 ??: data.failedApiRequestCount,
            "API ?섏떊 ?뺤콉 ??: data.apiReceivedCount,
            "以묐났 ?쒓굅 ????: data.uniquePolicyCount,
            "理쒖쥌 寃곌낵 ??: data.finalResultCount,
            "?뚯슂 ?쒓컙": `${data.elapsedTimeMs}ms`
        });
        renderGeneratedRequests(data.generatedApiRequests || []);
        renderNaturalResults(data.results || []);
    }

    function renderGeneratedRequests(requests) {
        const wrapper = document.getElementById("naturalGeneratedRequests");
        const tbody = wrapper.querySelector("tbody");
        tbody.innerHTML = "";
        requests.forEach(request => {
            tbody.insertAdjacentHTML("beforeend", `<tr><td>${safe(request.filterType)}</td><td>${safe(request.filterValue)}</td><td>${safe(request.pagesRequested)}</td><td>${safe(request.receivedCount)}</td><td>${request.succeeded ? "?? : "?꾨땲??}</td><td>${safe(request.statusCode)}</td><td>${safe(request.responseType)}</td><td>${safe(request.errorMessage)}</td></tr>`);
        });
        wrapper.classList.toggle("hidden", requests.length === 0);
    }

    function renderNaturalResults(results) {
        const target = document.getElementById("naturalResults");
        target.innerHTML = "";
        results.forEach(result => {
            const policy = result.policy || {};
            target.insertAdjacentHTML("beforeend", `<article class="policy-result-card">
                <h3>${safe(policy.policyName)}</h3>
                <div class="meta-list">
                    <span>?뺤콉 踰덊샇: ${safe(policy.policyNumber)}</span>
                    <span>?뺤콉 ?ㅼ썙?? ${safe((policy.keywordNames || []).join(", "))}</span>
                    <span>遺꾩빞: ${safe([policy.majorCategory, policy.middleCategory].filter(Boolean).join(" / "))}</span>
                    <span>二쇨? 湲곌?: ${safe(policy.supervisingInstitution)}</span>
                    <span>????섏씠: ${safe(ageText(policy))}</span>
                    <span>?좎껌 湲곌컙: ${safe(policy.applicationPeriod)}</span>
                    <span>?좎껌 諛⑸쾿: ${safe(shorten(policy.applicationMethod, 120))}</span>
                    <span>寃??愿?⑤룄: ${safe(result.relevanceScore)}</span>
                    <span>吏???먮퀎: ${safe(result.regionMatchStatus)} / ?섏씠 ?먮퀎: ${safe(result.ageMatchStatus)}</span>
                </div>
                <p>${safe(shorten(policy.supportContent || policy.policyDescription, 180))}</p>
                <strong>?쇱튂 ?댁쑀</strong>
                <ul class="reason-list">${listItems(result.matchedReasons)}</ul>
                <strong>?뺤씤 ?꾩슂 議곌굔</strong>
                <ul class="reason-list">${listItems(result.missingConditions)}</ul>
                <button class="detail-link" data-policy="${escapeAttr(policy.policyNumber)}" type="button">?곸꽭議고쉶</button>
            </article>`);
        });
        bindDetailLinks(target);
    }

    function renderProbe(data) {
        renderDefinition(document.getElementById("probeResult"), {
            "?붿껌 諛⑹떇": data.sourceMode,
            "留덉뒪???붿껌 URL": data.maskedRequestUrl,
            "HTTP ?곹깭": data.statusCode,
            "Content-Type": data.contentType,
            "媛먯????묐떟 ?뺤떇": data.responseType,
            "?묐떟 湲몄씠": data.responseLength,
            "?뚯슂 ?쒓컙": `${data.elapsedTimeMs}ms`,
            "Redirect ?щ?": data.redirected ? "?? : "?꾨땲??,
            "Redirect Location": data.redirectLocation,
            "理쒖쥌 URL": data.finalUrl,
            "API ?ㅻ쪟 ?щ?": data.errorResponse ? "?? : "?꾨땲??,
            "?ㅻ쪟 肄붾뱶": data.errorCode,
            "?ㅻ쪟 硫붿떆吏": data.errorMessage,
            "紐⑸줉 ?몃뱶 諛쒓껄 ?щ?": data.listNodeFound ? "?? : "?꾨땲??,
            "紐⑸줉 ?몃뱶 寃쎈줈": data.listNodePath,
            "?뚯떛 嫄댁닔": data.parsedCount,
            "泥?踰덉㎏ ?뺤콉紐?: data.firstPolicy && data.firstPolicy.policyName,
            "?묐떟 ?뚯씪 寃쎈줈": data.rawResponseFilePath
        });
        renderSchema(data.schemaAnalysis);
        rawPreview.textContent = data.responsePreview || "寃곌낵 ?놁쓬";
        if (data.statusCode === 302) {
            showAlert("probeAlert", "HTTP 302 Redirect ?묐떟?낅땲?? Location ?ㅻ뜑? 理쒖쥌 ?대룞 二쇱냼瑜??뺤씤?섏꽭??", true);
        } else if (data.responseType === "HTML") {
            showAlert("probeAlert", "?뺤콉 JSON/XML ???HTML ?묐떟??諛쏆븯?듬땲?? ?몄쬆?? URL, Redirect ?먮뒗 ?쒕퉬???곹깭瑜??뺤씤?섏꽭??", true);
        } else if (data.errorResponse) {
            showAlert("probeAlert", data.errorMessage || "API ?ㅻ쪟 ?묐떟?낅땲??", true);
        }
    }

    function renderList(data) {
        renderDefinition(document.getElementById("listSummary"), {
            "HTTP ?곹깭": data.statusCode,
            "?묐떟 ?뺤떇": data.responseType,
            "?뚯떛 ?뺤콉 ??: data.parsedCount,
            "?꾩껜 嫄댁닔": data.totalCount,
            "?꾩옱 ?섏씠吏": data.currentPage,
            "?붿껌 ?쒓컙": `${data.elapsedTimeMs}ms`,
            "紐⑸줉 ?몃뱶 寃쎈줈": data.listNodePath,
            "?먮낯 ?묐떟 ???寃쎈줈": data.rawResponseFilePath
        });
        const tbody = document.querySelector("#policyTable tbody");
        tbody.innerHTML = "";
        (data.policies || []).forEach((item, index) => {
            tbody.insertAdjacentHTML("beforeend", `<tr><td>${index + 1}</td><td>${safe(item.policyNumber)}</td><td><button class="secondary detail-link" data-policy="${escapeAttr(item.policyNumber)}">${safe(item.policyName)}</button></td><td>${safe(shorten(item.policyDescription, 120))}</td><td>${safe((item.keywordNames || []).join(", "))}</td><td>${safe(Object.keys(item.rawFields || {}).length)}</td><td><button class="detail-link" data-policy="${escapeAttr(item.policyNumber)}">?곸꽭議고쉶</button></td></tr>`);
        });
        bindDetailLinks(tbody);
    }

    function renderDetail(data) {
        const policy = data.policy || {};
        renderDefinition(document.getElementById("detailSummary"), {
            "?뺤콉 踰덊샇": policy.policyNumber,
            "?뺤콉紐?: policy.policyName,
            "?뺤콉 ?ㅻ챸": policy.policyDescription,
            "?뺤콉 ?ㅼ썙??: (policy.keywordNames || []).join(", "),
            "?먮낯 ?묐떟 ???寃쎈줈": data.rawResponseFilePath
        });
        const tbody = document.querySelector("#detailFieldsTable tbody");
        tbody.innerHTML = "";
        Object.entries(policy.rawFields || {}).forEach(([key, value]) => {
            tbody.insertAdjacentHTML("beforeend", `<tr><td>${escapeHtml(key)}</td><td>${safe(value)}</td><td>${value === null ? "null" : typeof value}</td></tr>`);
        });
    }

    function appendFilterProbeResult(data) {
        const tbody = document.querySelector("#filterProbeTable tbody");
        tbody.insertAdjacentHTML("afterbegin", `<tr><td>${safe(data.filterType)}</td><td>${safe(data.filterValue)}</td><td>${safe(data.statusCode)}</td><td>${safe(data.contentType)}</td><td>${safe(data.responseType)}</td><td>${safe(data.parsedCount)}</td><td>${safe(data.totalCount)}</td><td>${safe(data.elapsedTimeMs)}ms</td><td>${safe(data.errorMessage)}</td></tr>`);
    }

    function renderSchema(schema) {
        const target = document.getElementById("schemaResult");
        if (!schema) {
            target.innerHTML = "<p>?뺣낫 ?놁쓬</p>";
            return;
        }
        const arrays = (schema.candidateArrays || []).map(item => `<tr><td>${safe(item.path)}</td><td>${safe(item.size)}</td><td>${safe((item.fields || []).join(", "))}</td></tr>`).join("");
        target.innerHTML = `<div class="summary-grid">${definitionHtml({
            "猷⑦듃 ?꾨뱶": (schema.rootFields || []).join(", "),
            "?뺤콉 踰덊샇 ?꾨낫": (schema.policyNumberCandidates || []).join(", "),
            "?뺤콉紐??꾨낫": (schema.policyNameCandidates || []).join(", "),
            "?꾩껜 嫄댁닔 ?꾨낫": (schema.totalCountCandidates || []).join(", "),
            "?꾩옱 ?섏씠吏 ?꾨낫": (schema.currentPageCandidates || []).join(", "),
            "XML 猷⑦듃": schema.xmlRootElement
        })}</div><div class="table-wrapper"><table><thead><tr><th>諛곗뿴 寃쎈줈</th><th>諛곗뿴 ?ш린</th><th>泥???ぉ ?꾨뱶</th></tr></thead><tbody>${arrays}</tbody></table></div>`;
    }

    function bindDetailLinks(container) {
        container.querySelectorAll(".detail-link").forEach(button => {
            button.addEventListener("click", () => {
                document.getElementById("policyNumberInput").value = button.dataset.policy || "";
                document.getElementById("detailForm").requestSubmit();
            });
        });
    }

    function renderDefinition(target, values) {
        target.innerHTML = definitionHtml(values);
    }

    function definitionHtml(values) {
        return Object.entries(values).map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${safe(value)}</dd></div>`).join("");
    }

    function formData(form) {
        const data = {};
        new FormData(form).forEach((value, key) => {
            if (value !== "") {
                data[key] = isNumberField(key) ? Number(value) : value;
            }
        });
        return data;
    }

    function isNumberField(key) {
        return ["pageNum", "pageSize", "startPage", "maxPages", "resultSize"].includes(key);
    }

    function withButton(fn, alertId) {
        return async (event) => {
            await runWithDisabled(event.currentTarget, () => fn(), alertId);
        };
    }

    function withForm(fn, alertId) {
        return async (event) => {
            event.preventDefault();
            await runWithDisabled(event.currentTarget.querySelector("button[type='submit']"), () => fn(event.currentTarget), alertId);
        };
    }

    async function runWithDisabled(button, fn, alertId) {
        button.disabled = true;
        try {
            await fn();
        } catch (error) {
            showAlert(alertId, error.message, true);
        } finally {
            button.disabled = false;
        }
    }

    function clearResults() {
        ["probeResult", "schemaResult", "listSummary", "detailSummary", "paginationSummary", "naturalSummary"].forEach(id => {
            document.getElementById(id).innerHTML = "";
        });
        ["naturalSearchAlert", "probeAlert", "filterProbeAlert", "listAlert", "detailAlert", "paginationAlert"].forEach(hideAlert);
        document.getElementById("naturalResults").innerHTML = "";
        document.getElementById("naturalGeneratedRequests").classList.add("hidden");
        document.querySelector("#naturalGeneratedRequests tbody").innerHTML = "";
        document.querySelector("#filterProbeTable tbody").innerHTML = "";
        document.querySelector("#policyTable tbody").innerHTML = "";
        document.querySelector("#detailFieldsTable tbody").innerHTML = "";
        document.querySelector("#paginationTable tbody").innerHTML = "";
        rawPreview.textContent = "寃곌낵 ?놁쓬";
    }

    function showAlert(id, message, danger) {
        const target = document.getElementById(id);
        target.textContent = message;
        target.className = danger ? "alert danger" : "alert";
    }

    function hideAlert(id) {
        const target = document.getElementById(id);
        if (!target) return;
        target.className = "alert hidden";
        target.textContent = "";
    }

    function listItems(values) {
        const items = values || [];
        if (items.length === 0) {
            return "<li>?뺣낫 ?놁쓬</li>";
        }
        return items.map(item => `<li>${safe(item)}</li>`).join("");
    }

    function shorten(value, length) {
        if (!value) return value;
        const text = String(value);
        return text.length > length ? text.slice(0, length) + "..." : text;
    }

    function safe(value) {
        if (value === null || value === undefined || value === "") return "?뺣낫 ?놁쓬";
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

    function ageText(policy) {
        const min = policy.minimumAge || "";
        const max = policy.maximumAge || "";
        if (!min && !max) return "?뺤씤 ?꾩슂";
        return `${min || "?뺣낫 ?놁쓬"}~${max || "?뺣낫 ?놁쓬"}`;
    }
})();
