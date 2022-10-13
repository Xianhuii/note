package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 测试CORS的Controller
 *
 * @author jxh
 * @date 2022年10月13日 12:55
 */
@RestController
public class CorsController {
    @RequestMapping("cors")
    public String cors() {
        System.out.println("Hello Cors!");
        return "Hello Cors!";
    }
}
