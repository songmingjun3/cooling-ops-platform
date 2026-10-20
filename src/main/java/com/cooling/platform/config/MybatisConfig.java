package com.cooling.platform.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis 配置。
 *
 * <p>把 {@code @MapperScan} 从启动类中剥离出来，使 {@code @WebMvcTest}
 * 这类切片测试只装配 MVC 组件时不会被迫创建 Mapper Bean。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@Configuration
@MapperScan("com.cooling.platform.mapper")
public class MybatisConfig {
}
