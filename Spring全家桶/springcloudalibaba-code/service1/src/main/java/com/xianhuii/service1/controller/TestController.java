package com.xianhuii.service1.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {
    @Autowired
    private Environment environment;

    @GetMapping("/config")
    public void config() {
        System.out.println(environment.getProperty("name"));
        System.out.println(environment.getProperty("age"));
    }

    @GetMapping("/test")
    public String test() {
        return "test";
    }
}
