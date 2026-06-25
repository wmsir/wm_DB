package com.wmdb.config;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Configuration;

/**
 * MyBatis-Plus 配置类
 */
@Configuration
@MapperScan("com.wmdb.mapper")
public class MybatisPlusConfig {
}
