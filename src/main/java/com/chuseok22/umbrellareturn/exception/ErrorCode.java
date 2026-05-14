package com.chuseok22.umbrellareturn.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // GLOBAL
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에 문제가 발생했습니다"),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다"),

    // Umbrella
    UMBRELLA_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 우산입니다"),
    UMBRELLA_ALREADY_EXISTS(HttpStatus.CONFLICT, "이미 등록된 우산 번호입니다"),
    UMBRELLA_NOT_AVAILABLE(HttpStatus.CONFLICT, "이미 대여 중인 우산입니다"),
    UMBRELLA_IN_USE(HttpStatus.CONFLICT, "대여 중인 우산은 삭제할 수 없습니다"),

    // Rental
    RENTAL_NOT_FOUND(HttpStatus.NOT_FOUND, "대여 정보가 일치하지 않습니다"),
    RENTAL_RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 대여 내역입니다"),
    ;

    private final HttpStatus status;
    private final String message;
}
