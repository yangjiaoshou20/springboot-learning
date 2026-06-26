package com.github.lybgeek.exception;

import lombok.Getter;
import lombok.Setter;

public class BizException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    protected final String message;

    @Setter
    @Getter
    protected Integer errorCode;

    public BizException(String message) {
        this.message = message;
    }

    public BizException(String message, Throwable e) {
        super(message, e);
        this.message = message;
    }

    public BizException(Integer errorCode, String message, Throwable e) {
        super(message, e);
        this.message = message;
        this.errorCode = errorCode;
    }

    public BizException(Integer errorCode, String message) {
        this.message = message;
        this.errorCode = errorCode;
    }

    @Override
    public String getMessage() {
        return message;
    }

}
