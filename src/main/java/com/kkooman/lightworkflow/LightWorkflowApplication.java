package com.kkooman.lightworkflow;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.kkooman.lightworkflow")
public class LightWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LightWorkflowApplication.class, args);
    }
}
