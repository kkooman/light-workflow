package com.kkooman.lightworkflow;

import org.mybatis.spring.annotation.MapperScan;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan(basePackages = "com.kkooman.lightworkflow.watchlist.repository", annotationClass = Mapper.class)
public class LightWorkflowApplication {

    public static void main(String[] args) {
        SpringApplication.run(LightWorkflowApplication.class, args);
    }
}
