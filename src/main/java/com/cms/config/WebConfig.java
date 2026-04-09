package com.cms.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import java.io.FileNotFoundException;
/**
 * @author starsea
 * @date
 */
//@Configuration
//public class WebConfig implements WebMvcConfigurer {
//
//    @Value("${uploadDir}")
//    private String uploadDir;
//
//    @Override
//    public void addResourceHandlers(ResourceHandlerRegistry registry) {
//        try {
//            uploadDir = ResourceUtils.getURL(uploadDir).getPath();
//        } catch (FileNotFoundException e) {
//            throw new RuntimeException(e);
//        }
//        registry.addResourceHandler("/petimg/**")
//                .addResourceLocations("file:" +uploadDir+"\\petimg\\");
//
//        registry.addResourceHandler("/avatar/**")
//                .addResourceLocations("file:" +uploadDir+"\\avatar\\");
//
//        registry.addResourceHandler("/defaultpetimg/**")
//                .addResourceLocations("file:" +uploadDir+"\\defaultpetimg\\");
//
//        registry.addResourceHandler("/defaultavatar/**")
//                .addResourceLocations("file:" +uploadDir+"\\defaultavatar\\");
//
//        registry.addResourceHandler("/ai/**")
//                .addResourceLocations("file:" +uploadDir+"\\ai\\");
//    }
//}
@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        return objectMapper;
    }
    @Value("${upload.dir}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/files/**")
                .addResourceLocations("file:" + uploadDir + "/");
    }
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("http://localhost:8080")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}



