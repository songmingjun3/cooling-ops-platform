package com.cooling.platform.common.exception;

import lombok.Getter;

/**
 * 业务异常。
 *
 * <p>用于表达「可预期的业务失败」（如设备不存在、设备编号重复、状态不允许操作），
 * 由 {@link GlobalExceptionHandler} 统一转换为标准响应，避免在各层写重复的 try-catch。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Getter
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** 业务错误码。 */
    private final int code;

    /**
     * 使用默认错误码构造业务异常。
     *
     * @param message 提示信息
     */
    public BusinessException(String message) {
        this(-1, message);
    }

    /**
     * 构造业务异常。
     *
     * @param code    业务错误码
     * @param message 提示信息
     */
    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    /**
     * 构造业务异常（保留原始异常链，便于排查）。
     *
     * @param code    业务错误码
     * @param message 提示信息
     * @param cause   原始异常
     */
    public BusinessException(int code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }
}
