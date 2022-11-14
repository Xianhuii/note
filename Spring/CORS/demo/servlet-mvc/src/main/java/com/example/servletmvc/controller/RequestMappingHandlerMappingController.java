package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * RequestMappingHandlerMappingController示例
 *
 * @author jxh
 * @date 2022年11月14日 13:48
 */
@RestController
@RequestMapping("/RequestMappingHandlerMapping")
public class RequestMappingHandlerMappingController {
    @RequestMapping("/requestParam")
    public String requestParam(String value) {
        System.out.println(value);
        return value;
    }
}
