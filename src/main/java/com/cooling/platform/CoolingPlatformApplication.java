package com.cooling.platform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 数据中心冷却系统智能运维平台 —— 应用启动类。
 *
 * <p>技术栈：Spring Boot + Spring MVC + MyBatis + SQLite。</p>
 *
 * @author 项目组
 * @since 1.0.0
 */
@EnableScheduling
@MapperScan("com.cooling.platform.mapper")
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
