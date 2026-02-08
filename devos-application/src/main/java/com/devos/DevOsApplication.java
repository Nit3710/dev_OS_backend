package com.devos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(scanBasePackages = "com.devos")
@EnableCaching
@EnableAsync
@EnableScheduling
public class DevOsApplication {

    public static void main(String[] args) {
        SpringApplication.run(DevOsApplication.class, args);
        System.out.println("DevOS Backend API is running...");
    }

}
