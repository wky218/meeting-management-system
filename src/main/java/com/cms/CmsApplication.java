package com.cms;

import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;

@SpringBootApplication
@MapperScan("com.cms.mapper")
@EnableScheduling
public class CmsApplication {
    private static final String dateTimeFormat = "yyyy-MM-dd HH:mm:ss";
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jsonCustomizer() {
        return builder -> {
            // 支持多种日期格式
            builder.deserializerByType(LocalDateTime.class, new LocalDateTimeDeserializer(
                    new DateTimeFormatterBuilder()
                            .appendPattern("yyyy-MM-dd['T'][ ]HH:mm:ss")
                            .toFormatter()
            ));
        };
    }
    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
    }
}
