package com.cooling.platform.controller;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.common.response.Result;
import com.cooling.platform.dto.AlarmHandleDTO;
import com.cooling.platform.dto.AlarmQueryDTO;
import com.cooling.platform.service.AlarmService;
import com.cooling.platform.vo.AlarmVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

/**
 * 告警管理接口（需求文档 4.3，AM-03 / AM-04）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/alarms")
public class AlarmController {

    private final AlarmService alarmService;

    /**
     * 构造控制器。
     *
     * @param alarmService 告警业务接口
     */
    public AlarmController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    /**
     * 分页查询告警列表（AM-03）。
     *
     * @param query 查询条件（状态、级别、设备、分页）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<AlarmVO>> list(@Valid AlarmQueryDTO query) {
        return Result.ok(alarmService.query(query));
    }

    /**
     * 处理告警（AM-04）：记录处理人、处理时间与处理措施。
     *
     * @param id  告警主键
     * @param dto 处理信息
     * @return 操作结果
     */
    @PatchMapping("/{id}/handle")
    public Result<Void> handle(@PathVariable Long id, @RequestBody @Valid AlarmHandleDTO dto) {
        alarmService.handle(id, dto);
        return Result.ok();
    }
}
