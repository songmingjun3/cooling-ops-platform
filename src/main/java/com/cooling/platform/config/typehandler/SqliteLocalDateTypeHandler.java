package com.cooling.platform.config.typehandler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;
import org.apache.ibatis.type.MappedTypes;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;

/**
 * {@link LocalDate} 与 SQLite 文本列的映射处理器。
 *
 * <p>SQLite 没有原生日期类型，统一以 {@code yyyy-MM-dd} 文本存储。
 * sqlite-jdbc 的 {@code getObject(Class)} 对 JSR-310 支持不完整，
 * 因此显式实现「文本 ↔ LocalDate」的转换，保证跨驱动版本稳定。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@MappedTypes(LocalDate.class)
public class SqliteLocalDateTypeHandler extends BaseTypeHandler<LocalDate> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, LocalDate parameter, JdbcType jdbcType)
            throws SQLException {
        ps.setString(i, parameter.toString());
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return parse(rs.getString(columnName));
    }

    @Override
    public LocalDate getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return parse(rs.getString(columnIndex));
    }

    @Override
    public LocalDate getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return parse(cs.getString(columnIndex));
    }

    private LocalDate parse(String text) {
        if (text == null || text.trim().isEmpty()) {
            return null;
        }
        try {
            // 兼容库中可能存在的「日期 + 时间」写法（如 2024-06-15 00:00:00）
            String value = text.trim();
            if (value.length() > 10) {
                value = value.substring(0, 10);
            }
            return LocalDate.parse(value);
        } catch (DateTimeParseException ex) {
            throw new IllegalStateException("日期格式非法（期望 yyyy-MM-dd）: " + text, ex);
        }
    }
}
