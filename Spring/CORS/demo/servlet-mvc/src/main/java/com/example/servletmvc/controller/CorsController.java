package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    @RequestMapping("corsResponse")
    public String corsResponse(HttpServletResponse response) {
        response.addHeader("Access-Control-Allow-Origin", "*");
        System.out.println("Hello Cors!");
        return "Hello Cors!";
    }
}
