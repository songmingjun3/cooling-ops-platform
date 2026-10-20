package com.cooling.platform;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据中心冷却系统智能运维平台 —— 应用启动类。
 *
 * <p>技术栈：Spring Boot + Spring MVC + MyBatis + SQLite。</p>
 *
 * <p>MyBatis 的 {@code @MapperScan} 刻意放在独立的
 * {@link com.cooling.platform.config.MybatisConfig} 中：{@code @WebMvcTest}
 * 只装配 MVC 层组件，若把扫描注解写在启动类上会连带创建 Mapper Bean，
 * 导致接口测试因缺少 {@code SqlSessionFactory} 而启动失败。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@EnableScheduling
@SpringBootApplication
public class CoolingPlatformApplication {

    /**
     * 程序入口。
     *
     * @param args 启动参数
     */
    public static void main(String[] args) {
        SpringApplication.run(CoolingPlatformApplication.class, args);
    }
}
