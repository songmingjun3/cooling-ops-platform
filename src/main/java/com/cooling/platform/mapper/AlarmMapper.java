package com.cooling.platform.mapper;

import com.cooling.platform.common.enums.AlarmStatus;
import com.cooling.platform.dto.AlarmQueryDTO;
import com.cooling.platform.entity.Alarm;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 告警记录数据访问接口。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface AlarmMapper {

    /**
     * 新增告警。
     *
     * @param alarm 告警记录
     * @return 影响行数
     */
    int insert(Alarm alarm);

    /**
     * 按主键查询告警。
     *
     * @param id 主键
     * @return 告警记录
     */
    Alarm selectById(@Param("id") Long id);

    /**
     * 更新告警处理结果。
     *
     * @param id           主键
     * @param handler      处理人
     * @param handleTime   处理时间
     * @param handleMeasure 处理措施
     * @return 影响行数
     */
    int updateHandle(@Param("id") Long id,
                     @Param("handler") String handler,
                     @Param("handleTime") LocalDateTime handleTime,
                     @Param("handleMeasure") String handleMeasure);

    /**
     * 按条件分页查询告警。
     *
     * @param query  查询条件
     * @param offset 偏移量
     * @param limit  条数
     * @return 告警列表
     */
    List<Alarm> selectByCondition(@Param("q") AlarmQueryDTO query,
                                  @Param("offset") int offset,
                                  @Param("limit") int limit);

    /**
     * 按条件统计告警数。
     *
     * @param query 查询条件
     * @return 总数
     */
    long countByCondition(@Param("q") AlarmQueryDTO query);

    /**
     * 统计指定状态的告警数。
     *
     * @param status 状态
     * @return 数量
     */
    long countByStatus(@Param("status") AlarmStatus status);

    /**
     * 统计告警总数。
     *
     * @return 数量
     */
    long countAll();

    /**
     * 统计指定设备参数在最近 N 分钟内是否已存在未处理告警（用于告警抑制）。
     *
     * @param deviceId 设备编号
     * @param paramKey 参数键
     * @param minutes  时间窗口（分钟）
     * @return 数量
     */
    long countRecentUnhandled(@Param("deviceId") String deviceId,
                              @Param("paramKey") String paramKey,
                              @Param("minutes") int minutes);

    /**
     * 查询指定前缀下最大的告警编号。
     *
     * @param prefix 前缀，如 AL-20261020-
     * @return 告警编号，无记录时返回 {@code null}
     */
    String selectMaxAlarmNo(@Param("prefix") String prefix);
}
