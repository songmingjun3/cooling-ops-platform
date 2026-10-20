package com.cooling.platform.common.util;

import java.util.List;

/**
 * CSV 导出工具（需求文档 PM-04）。
 *
 * <p>输出带 UTF-8 BOM 与 CRLF 换行，保证用 Excel / WPS 直接双击打开时中文不乱码。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
public final class CsvExporter {

    /** UTF-8 BOM，Excel 识别 UTF-8 的标记。 */
    private static final String BOM = "\uFEFF";

    /** 换行符，CSV 规范建议 CRLF。 */
    private static final String CRLF = "\r\n";

    private CsvExporter() {
    }

    /**
     * 生成 CSV 文本。
     *
     * @param header 表头
     * @param rows   数据行
     * @return CSV 文本（含 BOM）
     */
    public static String build(String[] header, List<String[]> rows) {
        StringBuilder builder = new StringBuilder(BOM);
        appendRow(builder, header);
        for (String[] row : rows) {
            appendRow(builder, row);
        }
        return builder.toString();
    }

    /**
     * 转义单个单元格：包含逗号、引号或换行时用双引号包裹，内部引号翻倍。
     *
     * @param value 原始值
     * @return 转义后的值
     */
    public static String escape(String value) {
        if (value == null) {
            return "";
        }
        boolean needQuote = value.indexOf(',') >= 0 || value.indexOf('"') >= 0
                || value.indexOf('\n') >= 0 || value.indexOf('\r') >= 0;
        String escaped = value.replace("\"", "\"\"");
        return needQuote ? "\"" + escaped + "\"" : escaped;
    }

    private static void appendRow(StringBuilder builder, String[] row) {
        if (row == null) {
            builder.append(CRLF);
            return;
        }
        for (int i = 0; i < row.length; i++) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append(escape(row[i]));
        }
        builder.append(CRLF);
    }
}
