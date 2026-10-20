package com.cooling.platform.mapper;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.entity.AlarmThreshold;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/**
 * 告警阈值配置数据访问接口。
 *
 * <p>本表 SQL 均为简单单表操作，按项目约定使用注解映射（复杂动态 SQL 才用 XML）。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface AlarmThresholdMapper {

    String COLUMNS = "id, device_type, param_key, param_name, compare_op, threshold_value, "
            + "deviation_percent, unit, alarm_level, enabled, remark, update_time";

    /**
     * 新增阈值。
     *
     * @param threshold 阈值配置
     * @return 影响行数
     */
    @Insert("INSERT INTO alarm_threshold (device_type, param_key, param_name, compare_op, threshold_value,"
            + " deviation_percent, unit, alarm_level, enabled, remark, update_time)"
            + " VALUES (#{deviceType}, #{paramKey}, #{paramName}, #{compareOp}, #{thresholdValue},"
            + " #{deviationPercent}, #{unit}, #{alarmLevel}, #{enabled}, #{remark}, datetime('now','localtime'))")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AlarmThreshold threshold);

    /**
     * 修改阈值。
     *
     * @param threshold 阈值配置
     * @return 影响行数
     */
    @Update("UPDATE alarm_threshold SET device_type = #{deviceType}, param_key = #{paramKey},"
            + " param_name = #{paramName}, compare_op = #{compareOp}, threshold_value = #{thresholdValue},"
            + " deviation_percent = #{deviationPercent}, unit = #{unit}, alarm_level = #{alarmLevel},"
            + " enabled = #{enabled}, remark = #{remark}, update_time = datetime('now','localtime')"
            + " WHERE id = #{id}")
    int update(AlarmThreshold threshold);

    /**
     * 删除阈值。
     *
     * @param id 主键
     * @return 影响行数
     */
    @Delete("DELETE FROM alarm_threshold WHERE id = #{id}")
    int deleteById(@Param("id") Long id);

    /**
     * 按主键查询。
     *
     * @param id 主键
     * @return 阈值配置
     */
    @Select("SELECT " + COLUMNS + " FROM alarm_threshold WHERE id = #{id}")
    AlarmThreshold selectById(@Param("id") Long id);

    /**
     * 查询阈值列表，可按设备类型过滤。
     *
     * @param deviceType 设备类型，为空表示全部
     * @return 阈值列表
     */
    @Select("<script>SELECT " + COLUMNS + " FROM alarm_threshold"
            + "<where><if test='deviceType != null'>device_type = #{deviceType}</if></where>"
            + " ORDER BY device_type, param_key</script>")
    List<AlarmThreshold> selectList(@Param("deviceType") DeviceType deviceType);

    /**
     * 查询某设备类型下已启用的阈值（供自动检测使用）。
     *
     * @param deviceType 设备类型
     * @return 阈值列表
     */
    @Select("SELECT " + COLUMNS + " FROM alarm_threshold"
            + " WHERE device_type = #{deviceType} AND enabled = 1 ORDER BY id")
    List<AlarmThreshold> selectEnabledByType(@Param("deviceType") DeviceType deviceType);

    /**
     * 判断是否已存在同一「设备类型 + 参数 + 比较方式」的阈值。
     *
     * @param deviceType 设备类型
     * @param paramKey   参数键
     * @param compareOp  比较方式
     * @param excludeId  需排除的主键（修改场景），可为 {@code null}
     * @return 冲突条数
     */
    @Select("<script>SELECT COUNT(1) FROM alarm_threshold"
            + " WHERE device_type = #{deviceType} AND param_key = #{paramKey} AND compare_op = #{compareOp}"
            + "<if test='excludeId != null'> AND id != #{excludeId}</if></script>")
    long countDuplicate(@Param("deviceType") DeviceType deviceType,
                        @Param("paramKey") String paramKey,
                        @Param("compareOp") String compareOp,
                        @Param("excludeId") Long excludeId);

    /**
     * 启用 / 停用阈值。
     *
     * @param id      主键
     * @param enabled 是否启用
     * @return 影响行数
     */
    @Update("UPDATE alarm_threshold SET enabled = #{enabled}, update_time = datetime('now','localtime')"
            + " WHERE id = #{id}")
    int updateEnabled(@Param("id") Long id, @Param("enabled") boolean enabled);
}
