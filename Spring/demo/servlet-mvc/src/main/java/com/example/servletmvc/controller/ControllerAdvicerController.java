package com.example.servletmvc.controller;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ControllerAdvicerConfig
 *
 * @author jxh
 * @date 2022年11月14日 14:46
 */
@RestController
@RequestMapping("ControllerAdvicerConfig")
public class ControllerAdvicerController {
    @RequestMapping("byZero")
    public int byZero() {
        return 1/0;
    }

    private String test() {
        return null;
    }
}
