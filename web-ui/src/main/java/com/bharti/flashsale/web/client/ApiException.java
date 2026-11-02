package com.bharti.flashsale.web.client;

import lombok.Getter;

/** An error answer from the gateway (4xx/5xx), with the human-readable detail from its ProblemDetail body. */
@Getter
public class ApiException extends RuntimeException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public boolean isUnauthorized() {
        return status == 401;
    }

    public boolean isRateLimited() {
        return status == 429;
    }
}
