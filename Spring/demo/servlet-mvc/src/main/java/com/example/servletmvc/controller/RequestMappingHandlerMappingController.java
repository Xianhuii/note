package com.example.servletmvc.controller;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * RequestMappingHandlerMappingController示例
 *
 * @author jxh
 * @date 2022年11月14日 13:48
 */
//@RestController
@Component
@RequestMapping("/RequestMappingHandlerMapping")
public class RequestMappingHandlerMappingController {
    @RequestMapping("/requestParam")
    @ResponseBody
    public Map requestParam(String value) {
        Map<String, String> res = new HashMap<>();
        res.put("value", value);
        return res;
    }
}
