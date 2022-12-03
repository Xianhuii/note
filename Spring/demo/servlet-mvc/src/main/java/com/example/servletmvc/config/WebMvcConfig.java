package com.example.servletmvc.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ContentNegotiationConfigurer;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 使用WebMvcCofigurer全局配置CORS
 *
 * @author jxh
 * @date 2022年10月17日 12:32
 */
@Configuration
@EnableWebMvc
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api1/**")
                .allowedOrigins("https://domain1.com")
                .allowedMethods("PUT", "DELETE")
                .allowedHeaders("header1", "header2", "header3")
                .exposedHeaders("header1", "header2").
                allowCredentials(true).maxAge(3600);
        registry.addMapping("/api2/**")
                .allowedOrigins("https://domain2.com")
                .allowedMethods("PUT", "DELETE")
                .allowedHeaders("header1", "header2", "header3")
                .exposedHeaders("header1", "header2").
                allowCredentials(true).maxAge(3600);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 添加自定义拦截器
    }

    @Override
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {
        // 添加自定义内容协商管理器
    }

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 添加自定义地址解析器
    }
}
