package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

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
    public Map requestParam(String value) {
        Map<String, String> res = new HashMap<>();
        res.put("value", value);
        return res;
    }
}
