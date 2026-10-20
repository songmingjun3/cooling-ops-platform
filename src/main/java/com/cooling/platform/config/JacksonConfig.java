package com.cooling.platform.config;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateSerializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.format.DateTimeFormatter;

/**
 * Jackson 全局日期格式配置。
 *
 * <p>统一 JSON 中的时间格式，避免前端分别处理 ISO 与自定义格式：
 * 日期 {@code yyyy-MM-dd}，日期时间 {@code yyyy-MM-dd HH:mm:ss}。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Configuration
public class JacksonConfig {

    /** 日期时间格式。 */
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    /** 日期格式。 */
    public static final String DATE_PATTERN = "yyyy-MM-dd";

    /**
     * 注册 JSR-310 的序列化 / 反序列化器。
     *
     * @return Jackson 定制器
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer javaTimeCustomizer() {
        DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern(DATE_TIME_PATTERN);
        DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern(DATE_PATTERN);
        return builder -> {
            builder.serializers(new LocalDateTimeSerializer(dateTimeFormatter),
                    new LocalDateSerializer(dateFormatter));
            builder.deserializers(new LocalDateTimeDeserializer(dateTimeFormatter),
                    new LocalDateDeserializer(dateFormatter));
        };
    }
}
