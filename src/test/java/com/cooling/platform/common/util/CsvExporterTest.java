package com.cooling.platform.common.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * CSV 导出工具单元测试（PM-04 数据导出）。
 *
 * <p>重点验证两件容易出错的事：Excel 打开中文必须依赖 UTF-8 BOM；
 * 含逗号、引号、换行的字段必须正确转义，否则列会错位。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@DisplayName("CSV 导出工具")
class CsvExporterTest {

    @Test
    @DisplayName("首字符是 UTF-8 BOM，Excel 打开中文不乱码")
    void shouldStartWithBom() {
        String csv = CsvExporter.build(new String[]{"设备编号"}, Collections.<String[]>emptyList());
        assertTrue(csv.startsWith("\uFEFF"));
    }

    @Test
    @DisplayName("使用 CRLF 换行，符合 RFC 4180")
    void shouldUseCrLf() {
        String csv = CsvExporter.build(new String[]{"A", "B"},
                Collections.singletonList(new String[]{"1", "2"}));
        assertEquals("\uFEFFA,B\r\n1,2\r\n", csv);
    }

    @Test
    @DisplayName("普通值不加引号")
    void shouldNotQuotePlainValue() {
        assertEquals("冷水机组", CsvExporter.escape("冷水机组"));
        assertEquals("130.0", CsvExporter.escape("130.0"));
    }

    @Test
    @DisplayName("含逗号的值加引号，避免列错位")
    void shouldQuoteValueWithComma() {
        assertEquals("\"A区,1号机\"", CsvExporter.escape("A区,1号机"));
    }

    @Test
    @DisplayName("含引号的值加引号并把引号翻倍")
    void shouldEscapeDoubleQuote() {
        assertEquals("\"型号\"\"X\"\"\"", CsvExporter.escape("型号\"X\""));
    }

    @Test
    @DisplayName("含换行的值加引号")
    void shouldQuoteValueWithLineBreak() {
        assertEquals("\"第一行\n第二行\"", CsvExporter.escape("第一行\n第二行"));
        assertEquals("\"第一行\r第二行\"", CsvExporter.escape("第一行\r第二行"));
    }

    @Test
    @DisplayName("null 输出空串而不是 \"null\"")
    void shouldRenderNullAsEmpty() {
        assertEquals("", CsvExporter.escape(null));
    }

    @Test
    @DisplayName("空行与空列表处理正确")
    void shouldHandleEmptyRows() {
        String csv = CsvExporter.build(new String[]{"A"}, Arrays.<String[]>asList(
                new String[]{"1"}, null));
        assertEquals("\uFEFFA\r\n1\r\n\r\n", csv);
        assertFalse(csv.contains("null"));
    }

    @Test
    @DisplayName("多行多列内容可被逐行还原")
    void shouldProduceRestorableRows() {
        List<String[]> rows = Arrays.asList(
                new String[]{"CH-001", "1号冷水机组", "运行中"},
                new String[]{"CT-002", "2号冷却塔,备用", "已停机"});
        String csv = CsvExporter.build(new String[]{"编号", "名称", "状态"}, rows);
        String[] lines = csv.substring(1).split("\r\n");
        assertEquals(3, lines.length, "表头 + 2 行数据");
        assertTrue(lines[1].startsWith("CH-001,"));
        assertTrue(lines[2].contains("\"2号冷却塔,备用\""), "含逗号的字段必须被引号包裹");
    }
}
