(function () {
    const alertBox = document.getElementById("adminAlert");
    let pollingTimer = null;
    let statusTimer = null;

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
        const resolveBtn = document.getElementById("resolveRegionBtn");
        if (resolveBtn) {
            resolveBtn.addEventListener("click", resolveRegion);
        }
        loadStatus();
        resumeLatestJob();
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
                "Snapshot 보유 정책 수": data.policySnapshotCount,
                "Snapshot 미보유 정책 수": data.policySnapshotMissingCount,
                "Qdrant 문서 수": data.qdrantDocumentCount,
                "NATIONWIDE 정책 수": data.nationwidePolicyCount,
                "PROVINCE 정책 수": data.provincePolicyCount,
                "시·군·자치구 정책 수": data.cityPolicyCount,
                "MULTIPLE 정책 수": data.multiplePolicyCount,
                "UNKNOWN 정책 수": data.unknownPolicyCount,
                "SGIS 지역 동기화": data.sgisRegionSyncEnabled ? "활성" : "비활성",
                "SGIS 인증 정보": data.sgisCredentialsConfigured ? "설정됨" : "미설정",
                "지역 전체 수": data.regionTotalCount,
                "시·도 수": data.regionProvinceCount,
                "시·군 수": data.regionCityCount,
                "마지막 지역 동기화": data.lastRegionSyncTime,
                "마지막 지역 동기화 상태": data.lastRegionSyncStatus
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
            if (job === "region-catalog-sync") {
                endpoint = "/api/admin/jobs/region-catalog-sync";
            }
            const data = await api.post(endpoint, {});
            document.getElementById("adminRaw").textContent = JSON.stringify(data, null, 2);
            if (data.jobId) {
                renderJob(data);
                startPolling(data.jobId);
            }
            await loadStatus();
        } catch (error) {
            showAlert(error.message, true);
        }
    }

    function renderJob(data) {
        renderProgress(data);
        renderDefinition(document.getElementById("jobStatus"), {
            "현재 작업": data.jobType,
            "상태": data.status,
            "현재 단계": data.stageLabel || data.stage,
            "전체 진행률": percent(data.overallProgressPercent),
            "단계 진행률": percent(data.stageProgressPercent),
            "전체": data.totalCount,
            "처리": data.processedCount,
            "성공": data.successCount,
            "실패": data.failedCount,
            "남음": data.remainingCount,
            "현재 페이지": data.currentPage,
            "전체 페이지": data.totalPages,
            "현재 배치": data.currentBatch,
            "전체 배치": data.totalBatches,
            "현재 항목": data.currentItem,
            "API 요청": data.apiRequestCount,
            "재시도": data.retryCount,
            "시작 시간": formatTime(data.startedAt),
            "경과 시간": formatDuration(data.elapsedTimeMs),
            "처리 속도": data.throughputPerSecond ? `${data.throughputPerSecond.toFixed(2)}건/초` : null,
            "예상 남은 시간": data.estimatedRemainingSeconds == null ? null : `약 ${formatSeconds(data.estimatedRemainingSeconds)}`,
            "메시지": data.message
        });
    }

    function startPolling(jobId) {
        stopPolling();
        pollingTimer = setInterval(async () => {
            try {
                const data = await api.get(`/api/admin/jobs/${jobId}`);
                renderJob(data);
                document.getElementById("adminRaw").textContent = JSON.stringify(data, null, 2);
                setButtonsDisabled(data.status === "RUNNING");
                if (["COMPLETED", "COMPLETED_WITH_ERRORS", "FAILED", "CANCELLED"].includes(data.status)) {
                    stopPolling();
                    setButtonsDisabled(false);
                    await loadStatus();
                }
            } catch (error) {
                stopPolling();
                setButtonsDisabled(false);
                showAlert(error.message, true);
            }
        }, 1500);
        statusTimer = setInterval(loadStatus, 5000);
    }

    function stopPolling() {
        if (pollingTimer) clearInterval(pollingTimer);
        if (statusTimer) clearInterval(statusTimer);
        pollingTimer = null;
        statusTimer = null;
    }

    async function resumeLatestJob() {
        try {
            const data = await api.get("/api/admin/jobs/latest");
            if (data && data.jobId) {
                renderJob(data);
                if (data.status === "RUNNING") {
                    startPolling(data.jobId);
                    setButtonsDisabled(true);
                }
            }
        } catch (error) {
            // 관리자 키 미입력 상태에서는 조용히 무시한다.
        }
    }

    function renderProgress(data) {
        const target = document.getElementById("jobProgress");
        const overall = data.overallProgressPercent == null ? 0 : data.overallProgressPercent;
        const stage = data.stageProgressPercent == null ? 0 : data.stageProgressPercent;
        target.innerHTML = `
            <div class="progress-row">
                <div class="progress-label"><span>전체 진행률</span><strong>${data.indeterminate ? "계산 중" : overall + "%"}</strong></div>
                <div class="progress-track"><div class="progress-fill" style="width:${Math.max(0, Math.min(overall, 100))}%"></div></div>
            </div>
            <div class="progress-row">
                <div class="progress-label"><span>현재 단계: ${safe(data.stageLabel || data.stage)}</span><strong>${data.stageProgressPercent == null ? "계산 중" : stage + "%"}</strong></div>
                <div class="progress-track secondary"><div class="progress-fill" style="width:${Math.max(0, Math.min(stage, 100))}%"></div></div>
            </div>`;
    }

    function setButtonsDisabled(disabled) {
        document.querySelectorAll("[data-job]").forEach(button => {
            button.disabled = disabled;
        });
    }

    function renderDefinition(target, values) {
        target.innerHTML = Object.entries(values)
                .map(([key, value]) => `<div><dt>${escapeHtml(key)}</dt><dd>${safe(value)}</dd></div>`)
                .join("");
    }

    async function resolveRegion() {
        try {
            hideAlert();
            const query = document.getElementById("regionResolveInput").value;
            const data = await api.get(`/api/admin/regions/resolve?q=${encodeURIComponent(query)}`);
            renderDefinition(document.getElementById("regionResolveResult"), {
                "입력 원문": query,
                "판정 상태": data.status,
                "정규화 시·도": data.province,
                "정규화 시·군·구": data.city || data.district,
                "코드": data.regionCode,
                "후보": Array.isArray(data.candidates) ? data.candidates.join(", ") : data.candidates
            });
            document.getElementById("adminRaw").textContent = JSON.stringify(data, null, 2);
        } catch (error) {
            showAlert(error.message, true);
        }
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

    function percent(value) {
        return value === null || value === undefined ? "계산 중" : `${value}%`;
    }

    function formatTime(value) {
        return value ? new Date(value).toLocaleString() : null;
    }

    function formatDuration(ms) {
        if (!ms && ms !== 0) return null;
        return formatSeconds(Math.floor(ms / 1000));
    }

    function formatSeconds(seconds) {
        const min = Math.floor(seconds / 60);
        const sec = seconds % 60;
        return min > 0 ? `${min}분 ${sec}초` : `${sec}초`;
    }

    function escapeHtml(value) {
        return String(value)
                .replace(/&/g, "&amp;")
                .replace(/</g, "&lt;")
                .replace(/>/g, "&gt;");
    }
})();
