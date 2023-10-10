package com.zero.exchange;

import com.zero.exchange.db.DbTemplate;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
public class TradeEngineApplication {

    public static void main(String[] args) {
        SpringApplication.run(TradeEngineApplication.class, args);
    }
}
