package com.cooling.platform.common.exception;

import com.cooling.platform.common.response.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
    public Result<Void> handleBusinessException(BusinessException ex) {
        log.warn("业务异常: code={}, message={}", ex.getCode(), ex.getMessage());
        return Result.fail(ex.getCode(), ex.getMessage());
    }

    /**
     * 处理请求体参数校验失败（@RequestBody + @Valid）。
     *
     * @param ex 校验异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.isBindingFailure()
                        ? "参数 " + error.getField() + " 的取值不合法: " + error.getRejectedValue()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
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
    public Result<Void> handleBindException(BindException ex) {
        String message = ex.getFieldErrors().stream()
                .map(error -> error.isBindingFailure()
                        ? "参数 " + error.getField() + " 的取值不合法: " + error.getRejectedValue()
                        : error.getDefaultMessage())
                .collect(Collectors.joining("；"));
        log.warn("参数绑定失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理方法参数上的约束校验失败（@Validated + @RequestParam）。
     *
     * @param ex 约束校验异常
     * @return 失败响应
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.joining("；"));
        log.warn("参数校验失败: {}", message);
        return Result.fail(400, message);
    }

    /**
     * 处理请求体无法解析（JSON 格式错误、字段类型不匹配、枚举取值非法）。
     *
     * @param ex 消息不可读异常
     * @return 失败响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleMessageNotReadable(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return Result.fail(400, "请求参数格式不正确，请检查字段类型与枚举取值");
    }

    /**
     * 处理路径变量 / 查询参数类型不匹配（如把非数字传给数值参数）。
     *
     * @param ex 类型不匹配异常
     * @return 失败响应
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        log.warn("参数类型不匹配: name={}, value={}", ex.getName(), ex.getValue());
        return Result.fail(400, "参数 " + ex.getName() + " 的取值不合法: " + ex.getValue());
    }

    /**
     * 兜底异常处理：记录完整堆栈，避免异常被吞掉。
     *
     * @param ex 未预期异常
     * @return 失败响应
     */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception ex) {
        log.error("系统异常", ex);
        return Result.fail(500, "系统内部错误，请联系管理员");
    }
}
