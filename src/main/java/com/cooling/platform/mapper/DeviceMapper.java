package com.cooling.platform.mapper;

import com.cooling.platform.dto.DeviceQueryDTO;
import com.cooling.platform.entity.DevicePO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 设备表数据访问接口。
 *
 * <p>只负责 {@code device} 与 {@code sensor_data} 的 SQL 映射，不含业务判断。
 * 对应 SQL 见 {@code resources/mapper/DeviceMapper.xml}。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface DeviceMapper {

    /**
     * 新增设备。
     *
     * @param po 设备持久化对象
     * @return 影响行数
     */
    int insert(DevicePO po);

    /**
     * 修改设备基本信息（不含编号、类型、状态与运行参数）。
     *
     * @param po 设备持久化对象
     * @return 影响行数
     */
    int updateBasicInfo(DevicePO po);

    /**
     * 按编号删除设备。
     *
     * @param deviceId 设备编号
     * @return 影响行数
     */
    int deleteById(@Param("deviceId") String deviceId);

    /**
     * 更新设备运行状态（MM-03 / MM-04）。
     *
     * @param deviceId 设备编号
     * @param status   目标状态枚举名
     * @return 影响行数
     */
    int updateStatus(@Param("deviceId") String deviceId, @Param("status") String status);

    /**
     * 更新设备专有属性 JSON（模拟采集写入最新量测值，MM-05）。
     *
     * @param deviceId 设备编号
     * @param params   参数 JSON
     * @return 影响行数
     */
    int updateParams(@Param("deviceId") String deviceId, @Param("params") String params);

    /**
     * 按编号查询设备。
     *
     * @param deviceId 设备编号
     * @return 设备，不存在时返回 {@code null}
     */
    DevicePO selectById(@Param("deviceId") String deviceId);

    /**
     * 多条件分页查询设备。
     *
     * @param query  查询条件
     * @param offset SQL 偏移量
     * @param limit  每页条数
     * @return 设备列表
     */
    List<DevicePO> selectByCondition(@Param("q") DeviceQueryDTO query,
                                     @Param("offset") int offset,
                                     @Param("limit") int limit);

    /**
     * 统计满足条件的设备数量。
     *
     * @param query 查询条件
     * @return 总记录数
     */
    long countByCondition(@Param("q") DeviceQueryDTO query);

    /**
     * 查询全部设备，按区域、编号排序（用于按区域分组）。
     *
     * @return 设备列表
     */
    List<DevicePO> selectAll();

    /**
     * 查询指定前缀的全部设备编号（用于生成下一个编号）。
     *
     * @param prefix 设备类型前缀，如 CH
     * @return 设备编号列表
     */
    List<String> selectIdsByPrefix(@Param("prefix") String prefix);

    /**
     * 删除设备关联的传感器数据（设备删除时的数据清理）。
     *
     * @param deviceId 设备编号
     * @return 影响行数
     */
    int deleteSensorDataByDevice(@Param("deviceId") String deviceId);
}
