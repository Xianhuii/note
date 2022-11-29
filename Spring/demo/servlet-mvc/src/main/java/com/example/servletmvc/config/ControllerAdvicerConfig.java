package com.example.servletmvc.config;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * ControllerAdvicerConfig
 *
 * @author jxh
 * @date 2022年11月14日 14:43
 */
@ControllerAdvice
public class ControllerAdvicerConfig {
    @ExceptionHandler(value = Exception.class)
    @ResponseBody
    public String exception(Exception e) {
        return e.getMessage();
    }

    private String test() {
        return null;
    }
}
