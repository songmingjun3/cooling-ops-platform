package com.cooling.platform.factory;

import com.cooling.platform.common.enums.DeviceType;
import com.cooling.platform.common.exception.BusinessException;
import com.cooling.platform.mapper.DeviceMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * 设备编号生成器单元测试（FM-01 自动生成设备编号）。
 *
 * @author 项目组
 * @since 1.0.0
 */
@DisplayName("设备编号生成器")
class DeviceIdGeneratorTest {

    private static DeviceIdGenerator generatorWith(List<String> existingIds) {
        DeviceMapper mapper = mock(DeviceMapper.class);
        when(mapper.selectIdsByPrefix(anyString())).thenReturn(existingIds);
        return new DeviceIdGenerator(mapper);
    }

    @Test
    @DisplayName("库中无设备时从 001 开始")
    void shouldStartFromOneWhenEmpty() {
        assertEquals("CH-001", generatorWith(Collections.<String>emptyList()).nextId(DeviceType.CHILLER));
        assertEquals("CT-001", generatorWith(Collections.<String>emptyList()).nextId(DeviceType.COOLING_TOWER));
        assertEquals("PM-001", generatorWith(Collections.<String>emptyList()).nextId(DeviceType.PUMP));
        assertEquals("CU-001", generatorWith(Collections.<String>emptyList()).nextId(DeviceType.COOLING_UNIT));
    }

    @Test
    @DisplayName("取最大序号加一，而不是简单按数量递增")
    void shouldIncrementFromMaxSequence() {
        DeviceIdGenerator generator = generatorWith(Arrays.asList("CH-001", "CH-007", "CH-003"));
        assertEquals("CH-008", generator.nextId(DeviceType.CHILLER),
                "删除过中间编号后按数量递增会撞主键，必须取最大序号");
    }

    @Test
    @DisplayName("序号左补零到三位")
    void shouldPadSequence() {
        assertEquals("CH-002", generatorWith(Arrays.asList("CH-001")).nextId(DeviceType.CHILLER));
        assertEquals("CH-010", generatorWith(Arrays.asList("CH-009")).nextId(DeviceType.CHILLER));
        assertEquals("CH-100", generatorWith(Arrays.asList("CH-099")).nextId(DeviceType.CHILLER));
    }

    @Test
    @DisplayName("忽略序号非法或前后缀不匹配的脏数据")
    void shouldIgnoreMalformedIds() {
        DeviceIdGenerator generator = generatorWith(Arrays.asList(
                "CH-001", "CH-abc", "CH-", "CH001", "CT-999", null));
        assertEquals("CH-002", generator.nextId(DeviceType.CHILLER));
    }

    @Test
    @DisplayName("达到 999 上限时抛出业务异常")
    void shouldRejectWhenExceedLimit() {
        DeviceIdGenerator generator = generatorWith(Arrays.asList("CH-999"));
        BusinessException ex = assertThrows(BusinessException.class, () -> generator.nextId(DeviceType.CHILLER));
        assertEquals(500, ex.getCode());
        assertTrue(ex.getMessage().contains("999"));
    }

    @Test
    @DisplayName("生成结果不与既有编号冲突（多次连续调用）")
    void shouldNotCollideOnRepeatedCalls() {
        List<String> ids = new ArrayList<>(Arrays.asList("PM-001", "PM-002", "PM-004"));
        DeviceMapper mapper = mock(DeviceMapper.class);
        when(mapper.selectIdsByPrefix(anyString())).thenAnswer(invocation -> new ArrayList<>(ids));
        DeviceIdGenerator generator = new DeviceIdGenerator(mapper);

        String first = generator.nextId(DeviceType.PUMP);
        ids.add(first);
        String second = generator.nextId(DeviceType.PUMP);

        assertEquals("PM-005", first);
        assertEquals("PM-006", second);
    }
}
