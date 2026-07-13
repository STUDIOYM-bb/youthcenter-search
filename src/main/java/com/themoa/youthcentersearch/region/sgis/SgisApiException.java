package com.themoa.youthcentersearch.region.sgis;

public class SgisApiException extends RuntimeException {
    public SgisApiException(String message) {
        super(message);
    }

    public SgisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
