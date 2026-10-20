package com.cooling.platform.factory;

import com.cooling.platform.common.constant.DeviceConstants;
import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.mapper.DeviceMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 设备编号生成器。
 *
 * <p>编号规则（需求文档 3.1）：{@code 设备类型前缀-序号}，序号 3 位左补零，如 {@code CH-001}。
 * 生成方式为「查出该前缀已用的最大序号 + 1」，因此删号后不会复用，符合台账管理的通常做法。</p>
 *
 * <p>并发安全：本方法加方法级锁，配合「生成 + 插入」在同一事务内完成，
 * 保证并发新增同一类型设备时不会产生重复编号。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Slf4j
@Component
public class DeviceIdGenerator {

    private final DeviceMapper deviceMapper;

    /**
     * 构造编号生成器。
     *
     * @param deviceMapper 设备数据访问接口
     */
    public DeviceIdGenerator(DeviceMapper deviceMapper) {
        this.deviceMapper = deviceMapper;
    }

    /**
     * 生成指定类型的下一个设备编号。
     *
     * @param type 设备类型
     * @return 设备编号，如 {@code CH-003}
     */
    public synchronized String nextId(DeviceType type) {
        String prefix = type.getPrefix();
        List<String> existingIds = deviceMapper.selectIdsByPrefix(prefix);
        int maxSeq = 0;
        for (String id : existingIds) {
            int seq = parseSeq(id, prefix);
            if (seq > maxSeq) {
                maxSeq = seq;
            }
        }
        if (maxSeq >= DeviceConstants.ID_MAX_SEQ) {
            throw new BusinessException(500,
                    type.getLabel() + "编号已达上限 " + DeviceConstants.ID_MAX_SEQ + "，请扩展编号位数");
        }
        String id = String.format("%s%s%03d", prefix, DeviceConstants.ID_SEPARATOR, maxSeq + 1);
        log.debug("生成设备编号: type={}, id={}", type, id);
        return id;
    }

    /**
     * 从设备编号中解析序号，无法解析时返回 0（忽略历史脏数据）。
     *
     * @param deviceId 设备编号
     * @param prefix   设备类型前缀
     * @return 序号
     */
    private int parseSeq(String deviceId, String prefix) {
        if (deviceId == null || !deviceId.startsWith(prefix + DeviceConstants.ID_SEPARATOR)) {
            return 0;
        }
        String seqText = deviceId.substring(prefix.length() + DeviceConstants.ID_SEPARATOR.length());
        try {
            return Integer.parseInt(seqText);
        } catch (NumberFormatException ex) {
            log.warn("设备编号序号非数字，已忽略: {}", deviceId);
            return 0;
        }
    }
}
