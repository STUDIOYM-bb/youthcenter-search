package com.themoa.youthcentersearch.region.sgis;

import com.themoa.youthcentersearch.region.config.RegionSyncProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

import java.net.SocketTimeoutException;
import java.util.concurrent.Callable;

@Component
public class SgisRetryExecutor {
    private final RegionSyncProperties properties;

    public SgisRetryExecutor(RegionSyncProperties properties) {
        this.properties = properties;
    }

    public <T> T execute(Callable<T> callable) {
        RuntimeException last = null;
        int maxRetries = Math.max(0, properties.maxRetries());
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try {
                return callable.call();
            } catch (RuntimeException ex) {
                last = ex;
                if (attempt >= maxRetries || !retryable(ex)) {
                    throw ex;
                }
                sleep(attempt);
            } catch (Exception ex) {
                last = new SgisApiException("SGIS 요청에 실패했습니다.", ex);
                if (attempt >= maxRetries) {
                    throw last;
                }
                sleep(attempt);
            }
        }
        throw last == null ? new SgisApiException("SGIS 요청에 실패했습니다.") : last;
    }

    private boolean retryable(RuntimeException ex) {
        if (ex instanceof HttpServerErrorException) {
            return true;
        }
        if (ex instanceof HttpClientErrorException clientError) {
            int code = clientError.getStatusCode().value();
            return code == 401 || code == 408 || code == 429;
        }
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        Throwable cause = ex.getCause();
        while (cause != null) {
            if (cause instanceof SocketTimeoutException) {
                return true;
            }
            cause = cause.getCause();
        }
        return ex instanceof SgisApiException && ex.getMessage() != null
                && (ex.getMessage().contains("토큰") || ex.getMessage().contains("일시"));
    }

    private void sleep(int attempt) {
        long base = Math.max(200L, properties.requestDelay().toMillis());
        long delay = base * (1L << Math.min(attempt, 4));
        try {
            Thread.sleep(delay);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new SgisApiException("SGIS 재시도 대기가 중단되었습니다.", interrupted);
        }
    }
}
