package com.themoa.youthcentersearch.common.exception;

import com.themoa.youthcentersearch.youthcenter.parser.ResponseType;

public class YouthCenterApiResponseException extends YouthCenterApiException {
    private final int statusCode;
    private final String apiErrorCode;
    private final String apiErrorMessage;
    private final String contentType;
    private final ResponseType responseType;
    private final String maskedRequestUrl;
    private final String responsePreview;
    private final String redirectLocation;

    public YouthCenterApiResponseException(
            String message,
            int statusCode,
            String apiErrorCode,
            String apiErrorMessage,
            String contentType,
            ResponseType responseType,
            String maskedRequestUrl,
            String responsePreview,
            String redirectLocation
    ) {
        super(message);
        this.statusCode = statusCode;
        this.apiErrorCode = apiErrorCode;
        this.apiErrorMessage = apiErrorMessage;
        this.contentType = contentType;
        this.responseType = responseType;
        this.maskedRequestUrl = maskedRequestUrl;
        this.responsePreview = responsePreview;
        this.redirectLocation = redirectLocation;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public String getApiErrorCode() {
        return apiErrorCode;
    }

    public String getApiErrorMessage() {
        return apiErrorMessage;
    }

    public String getContentType() {
        return contentType;
    }

    public ResponseType getResponseType() {
        return responseType;
    }

    public String getMaskedRequestUrl() {
        return maskedRequestUrl;
    }

    public String getResponsePreview() {
        return responsePreview;
    }

    public String getRedirectLocation() {
        return redirectLocation;
    }
}
