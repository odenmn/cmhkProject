package com.cmhk.business;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.cmhk.business.**.mapper")
@SpringBootApplication
public class CmhkBusinessApplication {

    public static void main(String[] args) {
        SpringApplication.run(CmhkBusinessApplication.class, args);
    }
}

