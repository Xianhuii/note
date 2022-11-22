package com.example.servletmvc.filter;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.Collections;

/**
 * 使用spring-mvc的CorsFilter配置CORS
 *
 * @author jxh
 * @date 2022年10月17日 12:36
 */
@Configuration
public class CorsFilterConfig {
    @Bean
    public CorsFilter corsFilter() {
        // 1、创建CorsConfigurationSource配置源，使用spring-web内置实现
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        // 2、创建CorsConfiguration配置，并注册到配置源（可创建并注册多个配置）
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedOriginPatterns(Collections.singletonList("*"));
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        source.registerCorsConfiguration("/**", config);
        // 3、创建CorsFilter过滤器，设置配置源
        return new CorsFilter(source);
    }
}
