package com.example.servletmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 使用WebMvcCofigurer全局配置CORS
 *
 * @author jxh
 * @date 2022年10月17日 12:32
 */
//@Configuration
@EnableWebMvc
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins("https://domain2.com")
                .allowedMethods("PUT", "DELETE").
                allowedHeaders("header1", "header2", "header3")
                .exposedHeaders("header1", "header2").
                allowCredentials(true).maxAge(3600);
    }
}
