package com.xianhuii.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class Consumer implements ApplicationRunner {
    @Autowired
    private LoadBalancerClient loadBalancerClient;
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        String result = loadBalancerClient.execute("service-provider", (instance) -> {
            String host = instance.getHost();
            int port = instance.getPort();
            String url = "http://" + host + ":" + port + "/test";
            return restTemplate.getForEntity(url, String.class).getBody();
        });
        System.out.println(result);
    }
}
