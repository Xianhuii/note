package com.xianhuii.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient("service1")
public interface Service1Client {

    @GetMapping("/test")
    String test();
}
