(function () {
    const alertBox = document.getElementById("adminAlert");

    document.addEventListener("DOMContentLoaded", () => {
        const saved = sessionStorage.getItem("adminKey");
        if (saved) {
            document.getElementById("adminKeyInput").value = saved;
        }
        document.getElementById("saveAdminKeyBtn").addEventListener("click", () => {
            sessionStorage.setItem("adminKey", document.getElementById("adminKeyInput").value);
            showAlert("관리자 키를 sessionStorage에 저장했습니다.", false);
        });
        document.getElementById("refreshAdminStatusBtn").addEventListener("click", loadStatus);
        document.querySelectorAll("[data-job]").forEach(button => {
            button.addEventListener("click", () => run(button.dataset.job));
        });
        loadStatus();
    });

    async function loadStatus() {
        try {
            hideAlert();
            const data = await api.get("/api/admin/status");
            renderDefinition(document.getElementById("adminStatus"), {
                "Spring Boot": data.springBoot,
                "Secret 설정 파일": data.secretConfigFileFound ? "FOUND" : "NOT FOUND",
                "온통청년 API Key": data.youthCenterApiKeyConfigured ? "설정됨" : "미설정",
                "OpenAI API Key": data.openAiApiKeyConfigured ? "설정됨" : "미설정",
                "Spring AI Chat": data.springAiChatModel,
                "Spring AI Embedding": data.springAiEmbeddingModel,
                "OpenAI ChatModel": data.chatModelAvailable ? "활성" : "비활성",
                "OpenAI EmbeddingModel": data.embeddingModelAvailable ? "활성" : "비활성",
                "MySQL": data.mysqlAvailable ? "UP" : "DOWN",
                "Qdrant": data.qdrantAvailable ? "UP" : "DOWN",
                "RAG 활성": data.ragEnabled ? "활성" : "비활성",
                "Collection Name": data.collectionName,
                "전체 정책 수": data.totalPolicyCount,
                "활성 정책 수": data.activePolicyCount,
                "PENDING": data.pendingCount,
                "PROCESSING": data.processingCount,
                "SYNCED": data.syncedCount,
                "FAILED": data.failedCount,
                "Qdrant 문서 수": data.qdrantDocumentCount,
                "NATIONWIDE 정책 수": data.nationwidePolicyCount,
                "PROVINCE 정책 수": data.provincePolicyCount,
                "CITY 정책 수": data.cityPolicyCount,
                "DISTRICT 정책 수": data.districtPolicyCount,
                "MULTIPLE 정책 수": data.multiplePolicyCount,
                "UNKNOWN 정책 수": data.unknownPolicyCount
            });
        } catch (error) {
            showAlert(error.message, true);
        }
    }

    async function run(job) {
        try {
            hideAlert();
            let endpoint = `/api/admin/jobs/${job}`;
            if (job === "probe") {
                endpoint = "/api/admin/youth-center/probe";
            }
            if (job === "qdrant-search") {
                endpoint = "/api/admin/qdrant/search";
            }
            if (job === "policy-region-rebuild") {
                endpoint = "/api/admin/jobs/policy-region-rebuild";
            }
            const data = await api.post(endpoint, {});
            document.getElementById("adminRaw").textContent = JSON.stringify(data, null, 2);
            if (data.jobId) {
                renderJob(data);
            }
            await loadStatus();
        } catch (error) {
            showAlert(error.message, true);
        }
    }

    function renderJob(data) {
        renderDefinition(document.getElementById("jobStatus"), {
            "현재 작업": data.jobType,
            "상태": data.status,
            "전체": data.totalCount,
            "처리": data.processedCount,
            "성공": data.successCount,
            "실패": data.failedCount,
            "남음": data.remainingCount,
            "현재 페이지": data.currentPage,
            "현재 배치": data.currentBatch,
            "메시지": data.message
        });
    }

    function renderDefinition(target, values) {
        target.innerHTML = Object.entries(values)
                .map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${safe(value)}</dd></div>`)
                .join("");
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

    function escapeHtml(value) {
        return String(value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;");
    }
})();
