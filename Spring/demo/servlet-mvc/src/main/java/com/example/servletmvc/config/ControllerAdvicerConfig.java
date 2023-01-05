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
    @ExceptionHandler(value = ArithmeticException.class)
    @ResponseBody
    public String exception(Exception e) {
        return this.getClass().getName() + "\r\n" + e.getClass().getName() + "\r\n" + e.getMessage();
    }
}
