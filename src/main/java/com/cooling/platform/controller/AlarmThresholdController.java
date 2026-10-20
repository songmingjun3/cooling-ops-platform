package com.cooling.platform.controller;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.response.Result;
import com.cooling.platform.dto.AlarmThresholdDTO;
import com.cooling.platform.service.AlarmService;
import com.cooling.platform.vo.AlarmThresholdVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 告警阈值配置接口（需求文档 4.3，AM-01）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/alarm-thresholds")
public class AlarmThresholdController {

    private final AlarmService alarmService;

    /**
     * 构造控制器。
     *
     * @param alarmService 告警业务接口
     */
    public AlarmThresholdController(AlarmService alarmService) {
        this.alarmService = alarmService;
    }

    /**
     * 查询阈值配置列表。
     *
     * @param deviceType 设备类型，可为空表示全部
     * @return 阈值列表
     */
    @GetMapping
    public Result<List<AlarmThresholdVO>> list(@RequestParam(required = false) DeviceType deviceType) {
        return Result.ok(alarmService.listThresholds(deviceType));
    }

    /**
     * 新增阈值。
     *
     * @param dto 阈值配置
     * @return 新阈值主键
     */
    @PostMapping
    public Result<Long> create(@RequestBody @Valid AlarmThresholdDTO dto) {
        return Result.ok(alarmService.createThreshold(dto));
    }

    /**
     * 修改阈值。
     *
     * @param id  主键
     * @param dto 阈值配置
     * @return 操作结果
     */
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid AlarmThresholdDTO dto) {
        alarmService.updateThreshold(id, dto);
        return Result.ok();
    }

    /**
     * 删除阈值。
     *
     * @param id 主键
     * @return 操作结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        alarmService.deleteThreshold(id);
        return Result.ok();
    }

    /**
     * 启用 / 停用阈值。
     *
     * @param id      主键
     * @param enabled 是否启用
     * @return 操作结果
     */
    @PatchMapping("/{id}/enabled")
    public Result<Void> toggle(@PathVariable Long id, @RequestParam boolean enabled) {
        alarmService.toggleThreshold(id, enabled);
        return Result.ok();
    }
}
