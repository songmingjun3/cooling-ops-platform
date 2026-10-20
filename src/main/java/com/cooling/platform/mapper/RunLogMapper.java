package com.cooling.platform.mapper;

import com.cooling.platform.entity.RunLog;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 设备运行日志数据访问接口（PM-02）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface RunLogMapper {

    /**
     * 写入一条运行快照。
     *
     * @param runLog 运行日志
     * @return 影响行数
     */
    @Insert("INSERT INTO run_log (device_id, device_type, status, params, collect_time)"
            + " VALUES (#{deviceId}, #{deviceType}, #{status}, #{params}, #{collectTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(RunLog runLog);

    /**
     * 分页查询运行日志。
     *
     * @param deviceId 设备编号，为空表示全部
     * @param offset   偏移量
     * @param limit    条数
     * @return 日志列表
     */
    @Select("<script>SELECT id, device_id, device_type, status, params, collect_time FROM run_log"
            + "<where><if test='deviceId != null and deviceId != \"\"'>device_id = #{deviceId}</if></where>"
            + " ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<RunLog> selectPage(@Param("deviceId") String deviceId,
                            @Param("offset") int offset,
                            @Param("limit") int limit);

    /**
     * 统计运行日志条数。
     *
     * @param deviceId 设备编号，为空表示全部
     * @return 条数
     */
    @Select("<script>SELECT COUNT(1) FROM run_log"
            + "<where><if test='deviceId != null and deviceId != \"\"'>device_id = #{deviceId}</if></where></script>")
    long count(@Param("deviceId") String deviceId);

    /**
     * 删除某时间点之前的运行日志。
     *
     * @param cutoff 截止时间（yyyy-MM-dd HH:mm:ss）
     * @return 影响行数
     */
    @Delete("DELETE FROM run_log WHERE collect_time < #{cutoff}")
    int deleteOlderThan(@Param("cutoff") String cutoff);
}
