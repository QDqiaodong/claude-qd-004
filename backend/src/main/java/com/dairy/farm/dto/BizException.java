package com.dairy.farm.dto;

public class BizException extends RuntimeException {
    public BizException(String message) {
        super(message);
    }
}
