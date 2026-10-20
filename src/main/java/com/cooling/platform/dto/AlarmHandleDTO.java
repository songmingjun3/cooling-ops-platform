package com.cooling.platform.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

/**
 * 告警处理请求（AM-04）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Data
public class AlarmHandleDTO {

    /** 处理人。 */
    @NotBlank(message = "处理人不能为空")
    @Size(max = 30, message = "处理人长度不能超过 30 个字符")
    private String handler;

    /** 处理措施。 */
    @NotBlank(message = "处理措施不能为空")
    @Size(max = 200, message = "处理措施长度不能超过 200 个字符")
    private String measure;
}
