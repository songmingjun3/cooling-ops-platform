package com.cooling.platform.service;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.dto.AlarmHandleDTO;
import com.cooling.platform.dto.AlarmQueryDTO;
import com.cooling.platform.dto.AlarmThresholdDTO;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.entity.Device;
import com.cooling.platform.vo.AlarmThresholdVO;
import com.cooling.platform.vo.AlarmVO;

import java.util.List;

/**
 * 告警管理业务接口（需求文档 4.3，AM-01 ~ AM-05）。
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface AlarmService {

    /**
     * 分页查询告警（AM-03）。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<AlarmVO> query(AlarmQueryDTO query);

    /**
     * 处理告警（AM-04）。
     *
     * @param id  告警主键
     * @param dto 处理信息
     */
    void handle(Long id, AlarmHandleDTO dto);

    /**
     * 查询阈值配置（AM-01）。
     *
     * @param deviceType 设备类型，可为空表示全部
     * @return 阈值列表
     */
    List<AlarmThresholdVO> listThresholds(DeviceType deviceType);

    /**
     * 新增阈值（AM-01）。
     *
     * @param dto 阈值配置
     * @return 新阈值主键
     */
    Long createThreshold(AlarmThresholdDTO dto);

    /**
     * 修改阈值（AM-01）。
     *
     * @param id  主键
     * @param dto 阈值配置
     */
    void updateThreshold(Long id, AlarmThresholdDTO dto);

    /**
     * 删除阈值（AM-01）。
     *
     * @param id 主键
     */
    void deleteThreshold(Long id);

    /**
     * 启用 / 停用阈值（AM-01）。
     *
     * @param id      主键
     * @param enabled 是否启用
     */
    void toggleThreshold(Long id, boolean enabled);

    /**
     * 对一台设备执行阈值检测并落库（AM-02 / AM-05）。
     *
     * @param device 已完成本次采集的设备
     * @return 新产生的告警数量
     */
    int detect(Device device);
}
