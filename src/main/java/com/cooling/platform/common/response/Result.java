package com.cooling.platform.common.response;

import lombok.Getter;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 统一响应结果封装。
 *
 * <p>所有 REST 接口统一返回本结构，前端只需判断 {@code code}：{@code 0} 表示成功，
 * 非 {@code 0} 表示业务失败，{@code message} 可直接展示给用户。</p>
 *
 * @param <T> 业务数据类型
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public class Result<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 成功码。 */
    public static final int CODE_SUCCESS = 0;

    /** 通用失败码。 */
    public static final int CODE_FAIL = -1;

    /** 业务错误码。 */
    private final int code;

    /** 提示信息。 */
    private final String message;

    /** 业务数据。 */
    private final T data;

    /** 响应时间。 */
    private final LocalDateTime timestamp;

    private Result(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    /**
     * 成功响应（无数据）。
     *
     * @param <T> 业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> ok() {
        return new Result<>(CODE_SUCCESS, "操作成功", null);
    }

    /**
     * 成功响应（携带数据）。
     *
     * @param data 业务数据
     * @param <T>  业务数据类型
     * @return 成功结果
     */
    public static <T> Result<T> ok(T data) {
        return new Result<>(CODE_SUCCESS, "操作成功", data);
    }

    /**
     * 失败响应（通用失败码）。
     *
     * @param message 提示信息
     * @param <T>     业务数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(String message) {
        return new Result<>(CODE_FAIL, message, null);
    }

    /**
     * 失败响应（指定错误码）。
     *
     * @param code    错误码
     * @param message 提示信息
     * @param <T>     业务数据类型
     * @return 失败结果
     */
    public static <T> Result<T> fail(int code, String message) {
        return new Result<>(code, message, null);
    }
}
