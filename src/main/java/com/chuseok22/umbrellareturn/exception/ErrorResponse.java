package com.chuseok22.umbrellareturn.exception;

public record ErrorResponse(
    ErrorCode errorCode,
    String errorMessage
) {
    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode, errorCode.getMessage());
    }
}
