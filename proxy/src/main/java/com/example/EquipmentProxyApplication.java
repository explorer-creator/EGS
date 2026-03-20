package com.example; // 这里的包名要和你 pom.xml 一致

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EquipmentProxyApplication {
    public static void main(String[] args) {
        SpringApplication.run(EquipmentProxyApplication.class, args);
    }
}