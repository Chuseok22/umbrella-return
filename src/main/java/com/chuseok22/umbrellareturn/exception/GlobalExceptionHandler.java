package com.chuseok22.umbrellareturn.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // 컨트롤러에서 잡지 못한 CustomException 처리 (예상치 못한 경로)
    @ExceptionHandler(CustomException.class)
    public String handleCustomException(CustomException e, Model model) {
        log.warn("처리되지 않은 CustomException: code={}, message={}", e.getErrorCode(), e.getMessage());
        model.addAttribute("errorResponse", ErrorResponse.of(e.getErrorCode()));
        return "error";
    }

    @ExceptionHandler(Exception.class)
    public String handleException(Exception e, Model model) {
        log.error("처리되지 않은 예외 발생", e);
        model.addAttribute("errorResponse", ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR));
        return "error";
    }
}
