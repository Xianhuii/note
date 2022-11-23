package com.example.servletmvc.controller;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import static org.springframework.web.servlet.function.RequestPredicates.accept;
import static org.springframework.web.servlet.function.RouterFunctions.route;

/**
 * RouterFunctionMapping使用
 *
 * @author jxh
 * @date 2022年11月14日 10:49
 */
@Configuration
public class RouterFunctionMappingController {
    @Bean
    public RouterFunction<ServerResponse> person() {
        return route().GET("/RouterFunctionMappingController", accept(MediaType.APPLICATION_JSON), request -> {
            return ServerResponse.status(HttpStatus.OK).body("Hello World") ;
        }).build() ;
    }
}
