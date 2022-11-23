package com.example.servletmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

/**
 * MultipartResolver配置
 *
 * @author jxh
 * @date 2022年11月22日 11:20
 */
//@Configuration
public class MultipartConfig {
    @Bean
    public MultipartResolver multipartResolver() {
        MultipartResolver multipartResolver = new CommonsMultipartResolver();
        return multipartResolver;
    }
}
