package com.example.servletmvc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

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
        StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();
        multipartResolver.setResolveLazily(true);
        return multipartResolver;
    }
}
