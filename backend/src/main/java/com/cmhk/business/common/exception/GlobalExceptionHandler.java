package com.cmhk.business.common.exception;

import com.cmhk.business.common.ApiResponse;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiResponse<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException exception) {
        return validationFailure(exception.getBindingResult().getFieldError() == null
                ? "请求参数不正确"
                : exception.getBindingResult().getFieldError().getDefaultMessage());
    }

    @ExceptionHandler(BindException.class)
    public ApiResponse<Void> handleBind(BindException exception) {
        return validationFailure(exception.getBindingResult().getFieldError() == null
                ? "请求参数不正确"
                : exception.getBindingResult().getFieldError().getDefaultMessage());
    }

    @ExceptionHandler({
            ConstraintViolationException.class,
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ApiResponse<Void> handleRequestParameter(Exception exception) {
        log.info("请求参数校验失败，type={}", exception.getClass().getSimpleName());
        return ApiResponse.fail("请求参数不正确");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiResponse<Void> handleUnreadableBody(HttpMessageNotReadableException exception) {
        log.info("请求体解析失败");
        return ApiResponse.fail("请求体格式不正确");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ApiResponse<Void> handleBusinessArgument(IllegalArgumentException exception) {
        log.info("业务参数处理失败，reason={}", exception.getMessage());
        return ApiResponse.fail(exception.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ApiResponse<Void> handleUnexpected(Exception exception) {
        log.error("系统异常，type={}", exception.getClass().getSimpleName());
        return ApiResponse.fail("系统繁忙，请稍后再试");
    }

    private ApiResponse<Void> validationFailure(String message) {
        log.info("请求参数校验失败，reason={}", message);
        return ApiResponse.fail(message);
    }
}
