package com.juzi.aipassagecreator;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.juzi.aipassagecreator.mapper")
@SpringBootApplication
public class AiPassageCreatorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiPassageCreatorApplication.class, args);
    }

}
