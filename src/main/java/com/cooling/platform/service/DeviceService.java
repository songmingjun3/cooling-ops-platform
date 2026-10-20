package com.cooling.platform.service;

import com.cooling.platform.common.response.PageResult;
import com.cooling.platform.dto.DeviceCreateDTO;
import com.cooling.platform.dto.DeviceQueryDTO;
import com.cooling.platform.dto.DeviceUpdateDTO;
import com.cooling.platform.vo.DeviceDetailVO;
import com.cooling.platform.vo.DeviceVO;
import com.cooling.platform.vo.ZoneGroupVO;

import java.util.List;

/**
 * 设备管理业务接口（需求文档 4.1，FM-01 ~ FM-06）。
 *
 * @author 项目组
 * @since 1.0.0
 */
public interface DeviceService {

    /**
     * 多条件分页查询设备（FM-04 / FM-05）。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<DeviceVO> query(DeviceQueryDTO query);

    /**
     * 查询设备详情（MM-01）。
     *
     * @param deviceId 设备编号
     * @return 设备详情
     */
    DeviceDetailVO detail(String deviceId);

    /**
     * 新增设备，编号由系统自动生成（FM-01）。
     *
     * @param dto 新增请求
     * @return 新建设备编号
     */
    String create(DeviceCreateDTO dto);

    /**
     * 修改设备基本信息（FM-03）。
     *
     * @param deviceId 设备编号
     * @param dto      修改请求
     */
    void update(String deviceId, DeviceUpdateDTO dto);

    /**
     * 删除设备（FM-02）。
     *
     * @param deviceId 设备编号
     */
    void delete(String deviceId);

    /**
     * 按区域分组查看设备（FM-06）。
     *
     * @return 分组列表，按区域名升序
     */
    List<ZoneGroupVO> groupByZone();

    /**
     * 导出设备清单为 CSV 文本（PM-04）。
     *
     * @return CSV 文本（含 UTF-8 BOM）
     */
    String exportCsv();
}
