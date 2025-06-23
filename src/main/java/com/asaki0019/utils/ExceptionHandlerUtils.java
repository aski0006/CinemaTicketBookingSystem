package com.asaki0019.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

/**
 * 全局异常处理工具类，适用于Spring Boot项目
 * 支持自定义异常、常见异常的统一处理和日志记录
 */
@RestControllerAdvice
public class ExceptionHandlerUtils {
    private static final Logger logger = LoggerFactory.getLogger(ExceptionHandlerUtils.class);

    /**
     * 处理所有未捕获的异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleException(Exception ex) {
        logger.error("[全局异常] {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("code", 500);
        body.put("msg", "服务器内部错误");
        body.put("detail", ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    /**
     * 处理参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        logger.warn("[参数校验异常] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("msg", "参数校验失败");
        body.put("detail", ex.getBindingResult().getFieldError().getDefaultMessage());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理缺少请求参数异常
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        logger.warn("[缺少参数] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", 400);
        body.put("msg", "缺少请求参数: " + ex.getParameterName());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 处理自定义业务异常
     */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<Map<String, Object>> handleServiceException(ServiceException ex) {
        logger.warn("[业务异常] {}", ex.getMessage());
        Map<String, Object> body = new HashMap<>();
        body.put("code", ex.getCode());
        body.put("msg", ex.getMsg());
        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    /**
     * 自定义业务异常类
     */
    public static class ServiceException extends RuntimeException {
        private final int code;
        private final String msg;

        public ServiceException(int code, String msg) {
            super(msg);
            this.code = code;
            this.msg = msg;
        }

        public int getCode() {
            return code;
        }

        public String getMsg() {
            return msg;
        }
    }
}