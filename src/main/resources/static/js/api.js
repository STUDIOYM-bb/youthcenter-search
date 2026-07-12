(function () {
    const DEFAULT_TIMEOUT = 30000;

    async function request(method, url, body) {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), DEFAULT_TIMEOUT);
        const headers = { "Accept": "application/json" };
        const adminKey = sessionStorage.getItem("adminKey");
        if (adminKey) {
            headers["X-Admin-Key"] = adminKey;
        }

        const options = { method, headers, signal: controller.signal };
        if (body !== undefined) {
            headers["Content-Type"] = "application/json";
            options.body = JSON.stringify(body);
        }

        try {
            const response = await fetch(url, options);
            const contentType = response.headers.get("content-type") || "";
            const text = await response.text();
            let payload = null;

            if (contentType.includes("application/json") && text) {
                payload = JSON.parse(text);
            }

            if (!response.ok) {
                throw new Error(payload && payload.message ? payload.message : text || `HTTP ${response.status}`);
            }
            if (!payload || typeof payload.success === "undefined") {
                throw new Error("서버 응답 형식이 올바르지 않습니다.");
            }
            if (!payload.success) {
                throw new Error(payload.message || "요청 처리에 실패했습니다.");
            }
            return payload.data;
        } catch (error) {
            if (error.name === "AbortError") {
                throw new Error("요청 시간이 초과되었습니다.");
            }
            throw error;
        } finally {
            clearTimeout(timeoutId);
        }
    }

    window.api = {
        get: (url) => request("GET", url),
        post: (url, body) => request("POST", url, body)
    };
})();
