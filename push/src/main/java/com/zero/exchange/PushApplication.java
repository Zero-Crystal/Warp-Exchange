package com.zero.exchange;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;

// 禁用数据库自动配置(无DataSource, JdbcTemplate...)
@SpringBootApplication(exclude = DataSourceAutoConfiguration.class)
public class PushApplication {

    public static void main(String[] args) {
        // 系统属性，用于禁用Vert.x在类路径（classpath）中解析文件的功能
        System.setProperty("vertx.disableFileCPResolving", "true");
        //系统属性，用于指定Vert.x使用的日志委托工厂类名
        System.setProperty("vertx.logger-delegate-factory-class-name", "vertx.logger-delegate-factory-class-name");
        SpringApplication app = new SpringApplication(PushApplication.class);
        // 禁用 Spring Web
        app.setWebApplicationType(WebApplicationType.NONE);
        app.run(args);
    }
}
