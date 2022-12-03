package com.example.servletmvc.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.http.HttpServletResponse;

/**
 * 测试CORS的Controller
 *
 * @author jxh
 * @date 2022年10月13日 12:55
 */
//@CrossOrigin
@RestController
public class CorsController {
    @Autowired
    private DispatcherServlet dispatcherServlet;

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
    @CrossOrigin
    @RequestMapping("CrossOrigin")
    public String CrossOrigin() {
        System.out.println("Hello Cors!");
        return "Hello Cors!";
    }
}
