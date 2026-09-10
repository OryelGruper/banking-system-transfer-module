package com.bankingsystem.transfer;

import com.bankingsystem.transfer.config.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class TransferModuleApplication {

    public static void main(String[] args) {
        SpringApplication.run(TransferModuleApplication.class, args);
    }
}
