package com.cooling.platform.common.exception;

import com.cooling.platform.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>需求文档 5.1 要求禁止吞异常、异常需记录日志或向用户反馈：所有异常在此统一记录并转为
 * {@link Result} 结构返回，Controller 中不再出现空的 catch 块。</p>
 *
 * <p>响应体的 {@code code} 与 HTTP 状态码保持一致（400 / 404 / 500），
 * 便于前端与接口测试直接按 HTTP 语义判断成败，无需解析业务码。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常。
     *
     * @param ex 业务异常
     * @return 失败响应
     */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<Result<Void>> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理请求体参数校验失败（@RequestBody + @Valid）。
     *
     * @param ex 校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Result<Void>> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.isBindingFailure()
                        ? "参数 " + error.getField() + " 的取值不合法: " + error.getRejectedValue()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理表单/查询参数绑定校验失败。
     *
     * <p>绑定失败（如枚举取值非法）时给出中文提示，避免把框架异常原文抛给前端。</p>
     *
     * @param ex 绑定异常
     * @return 失败响应
     */
    @ExceptionHandler(BindException.class)
    public ResponseEntity<Result<Void>> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(error -> error.isBindingFailure()
                        ? "参数 " + error.getField() + " 的取值不合法: " + error.getRejectedValue()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败: {}", message);
        return fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理方法参数上的约束校验失败（@Validated + @RequestParam）。
     *
     * @param ex 约束校验异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Result<Void>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return fail(HttpStatus.BAD_REQUEST.value(), message);
    }

    /**
     * 处理请求体无法解析（JSON 格式错误、字段类型不匹配、枚举取值非法）。
     *
     * @param ex 消息不可读异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<Void>> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return fail(HttpStatus.BAD_REQUEST.value(), "请求参数格式不正确，请检查字段类型与枚举取值");
    }

    /**
     * 处理路径变量 / 查询参数类型不匹配（如把非数字传给数值参数）。
     *
     * @param ex 类型不匹配异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Result<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配: name={}, value={}", ex.getName(), ex.getValue());
        return fail(HttpStatus.BAD_REQUEST.value(),
                "参数 " + ex.getName() + " 的取值不合法: " + ex.getValue());
    }

    /**
     * 处理缺少必填请求参数（如 {@code @RequestParam} 未传值）。
     *
     * <p>若不单独处理，会落到兜底分支被误判为 500；按 HTTP 语义应为 400。</p>
     *
     * @param ex 缺参异常
     * @return 失败响应
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Result<Void>> handleMissingParameter(MissingServletRequestParameterException ex) {
        log.warn("缺少必填参数: name={}", ex.getParameterName());
        return fail(HttpStatus.BAD_REQUEST.value(), "缺少必填参数: " + ex.getParameterName());
    }

    /**
     * 处理请求方法不支持（如对只读接口发起 POST）。
     *
     * @param ex 方法不支持异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        log.warn("请求方法不支持: method={}, uri 支持={}", ex.getMethod(), ex.getSupportedHttpMethods());
        return fail(HttpStatus.METHOD_NOT_ALLOWED.value(),
                "该接口不支持 " + ex.getMethod() + " 请求");
    }

    /**
     * 处理请求内容类型不支持（如未声明 JSON 却提交 JSON 体）。
     *
     * @param ex 媒体类型不支持异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<Result<Void>> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException ex) {
        log.warn("请求内容类型不支持: contentType={}", ex.getContentType());
        return fail(HttpStatus.UNSUPPORTED_MEDIA_TYPE.value(),
                "请求内容类型不支持: " + ex.getContentType() + "，请使用 application/json");
    }

    /**
     * 兜底异常处理：记录完整堆栈，避免异常被吞掉。
     *
     * @param ex 未预期异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<Void>> handleException(Exception ex) {
        log.error("系统异常", ex);
        return fail(HttpStatus.INTERNAL_SERVER_ERROR.value(), "系统内部错误，请联系管理员");
    }

    /**
     * 构造失败响应，使 HTTP 状态码与业务码保持一致。
     *
     * @param code    业务码
     * @param message 提示信息
     * @return 失败响应
     */
    private ResponseEntity<Result<Void>> fail(int code, String message) {
        HttpStatus status = HttpStatus.resolve(code);
        if (status == null || status.is2xxSuccessful()) {
            status = HttpStatus.BAD_REQUEST;
        }
        return ResponseEntity.status(status).body(Result.fail(code, message));
    }
}
