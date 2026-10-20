package com.cooling.platform.controller;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.common.response.Result;
import com.cooling.platform.dto.DeviceCreateDTO;
import com.cooling.platform.dto.DeviceQueryDTO;
import com.cooling.platform.dto.DeviceUpdateDTO;
import com.cooling.platform.service.DeviceService;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.DeviceVO;
import com.cooling.platform.vo.ZoneGroupVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;

/**
 * 设备管理接口（需求文档 4.1，FM-01 ~ FM-06）。
 *
 * <p>本层只做参数校验与协议转换：不写业务逻辑，不直接访问 Mapper。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Validated
@RestController
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    /**
     * 构造控制器。
     *
     * @param deviceService 设备业务接口
     */
    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    /**
     * 多条件分页查询设备清单（FM-04 / FM-05）。
     *
     * @param query 查询条件（编号、关键字、类型、区域、状态、分页）
     * @return 分页结果
     */
    @GetMapping
    public Result<PageResult<DeviceVO>> list(@Valid DeviceQueryDTO query) {
        return Result.ok(deviceService.query(query));
    }

    /**
     * 按区域分组查看设备（FM-06）。
     *
     * @return 分组结果
     */
    @GetMapping("/group-by-zone")
    public Result<List<ZoneGroupVO>> groupByZone() {
        return Result.ok(deviceService.groupByZone());
    }

    /**
     * 查询设备详情（MM-01）。
     *
     * @param deviceId 设备编号
     * @return 设备详情
     */
    @GetMapping("/{deviceId}")
    public Result<DeviceDetailVO> detail(@PathVariable String deviceId) {
        return Result.ok(deviceService.detail(deviceId));
    }

    /**
     * 新增设备，设备编号由后端生成（FM-01）。
     *
     * @param dto 新增请求
     * @return 新建设备编号
     */
    @PostMapping
    public Result<String> create(@RequestBody @Valid DeviceCreateDTO dto) {
        String deviceId = deviceService.create(dto);
        log.info("接口新增设备: {}", deviceId);
        return Result.ok(deviceId);
    }

    /**
     * 修改设备基本信息（FM-03）。
     *
     * @param deviceId 设备编号
     * @param dto      修改请求
     * @return 操作结果
     */
    @PutMapping("/{deviceId}")
    public Result<Void> update(@PathVariable String deviceId, @RequestBody @Valid DeviceUpdateDTO dto) {
        deviceService.update(deviceId, dto);
        return Result.ok();
    }

    /**
     * 删除设备（FM-02）。
     *
     * <p>删除为不可恢复操作，前端须弹出二次确认后再调用。
     * 删除设备会同步清理其采集数据，告警与运行日志作为历史留痕保留。</p>
     *
     * @param deviceId 设备编号
     * @return 操作结果
     */
    @DeleteMapping("/{deviceId}")
    public Result<Void> delete(@PathVariable String deviceId) {
        deviceService.delete(deviceId);
        return Result.ok();
    }
}
