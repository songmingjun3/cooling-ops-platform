package com.cooling.platform.mapper;

import com.cooling.platform.entity.SensorData;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 传感器采集数据访问接口。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface SensorDataMapper {

    /**
     * 批量写入采集数据。
     *
     * @param list 采集数据列表
     * @return 影响行数
     */
    int insertBatch(@Param("list") List<SensorData> list);

    /**
     * 查询指定设备每个传感器的最新一条数据。
     *
     * @param deviceId 设备编号
     * @return 最新数据列表
     */
    List<SensorData> selectLatestByDevice(@Param("deviceId") String deviceId);

    /**
     * 查询指定设备某参数最近 N 条数据（按时间倒序）。
     *
     * @param deviceId  设备编号
     * @param sensorKey 参数键
     * @param limit     条数
     * @return 数据列表
     */
    List<SensorData> selectRecent(@Param("deviceId") String deviceId,
                                  @Param("sensorKey") String sensorKey,
                                  @Param("limit") int limit);

    /**
     * 查询指定设备最早出现的参数键（用于趋势图默认参数）。
     *
     * @param deviceId 设备编号
     * @return 参数键列表
     */
    List<String> selectSensorKeys(@Param("deviceId") String deviceId);

    /**
     * 删除某时间点之前的采集数据。
     *
     * @param cutoff 截止时间（yyyy-MM-dd HH:mm:ss）
     * @return 影响行数
     */
    int deleteOlderThan(@Param("cutoff") String cutoff);
}
