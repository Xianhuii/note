package com.xianhuii.controller;

import com.xianhuii.feign.Service1Client;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@RestController
public class ConsumerController {
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private Service1Client service1Client;

    @GetMapping("/consumer")
    public void consumer() throws IOException {
        String result = loadBalancerClient.execute("service1", (instance) -> {
            String host = instance.getHost();
            int port = instance.getPort();
            String url = "http://" + host + ":" + port + "/test";
            return restTemplate.getForEntity(url, String.class).getBody();
        });
        System.out.println(result);
    }

    @GetMapping("/consumer2")
    public void consumer2() {
        System.out.println(service1Client.test());
    }
}
