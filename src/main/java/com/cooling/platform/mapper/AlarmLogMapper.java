package com.cooling.platform.mapper;

import com.cooling.platform.entity.AlarmLog;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 告警流水日志数据访问接口（PM-03）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@Mapper
public interface AlarmLogMapper {

    /**
     * 追加一条日志。
     *
     * @param log 日志
     * @return 影响行数
     */
    @Insert("INSERT INTO alarm_log (alarm_no, action, detail, operator, log_time)"
            + " VALUES (#{alarmNo}, #{action}, #{detail}, #{operator}, #{logTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(AlarmLog log);

    /**
     * 分页查询日志。
     *
     * @param alarmNo 告警编号，为空表示全部
     * @param offset  偏移量
     * @param limit   条数
     * @return 日志列表
     */
    @Select("<script>SELECT id, alarm_no, action, detail, operator, log_time FROM alarm_log"
            + "<where><if test='alarmNo != null and alarmNo != \"\"'>alarm_no = #{alarmNo}</if></where>"
            + " ORDER BY id DESC LIMIT #{limit} OFFSET #{offset}</script>")
    List<AlarmLog> selectPage(@Param("alarmNo") String alarmNo,
                              @Param("offset") int offset,
                              @Param("limit") int limit);

    /**
     * 统计日志条数。
     *
     * @param alarmNo 告警编号，为空表示全部
     * @return 条数
     */
    @Select("<script>SELECT COUNT(1) FROM alarm_log"
            + "<where><if test='alarmNo != null and alarmNo != \"\"'>alarm_no = #{alarmNo}</if></where></script>")
    long count(@Param("alarmNo") String alarmNo);
}
