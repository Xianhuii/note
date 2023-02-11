> 预备知识：[SpringCloudAlibaba项目搭建流程](https://www.cnblogs.com/Xianhuii/p/17111321.html)
# 1 使用
## 1.1 Nacos服务器

## 1.2 服务提供者
### 1.2.1 依赖
服务提供者需要将自己注册到Nacos，所以需要引入：
```xml
<dependency>  
    <groupId>com.alibaba.cloud</groupId>  
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>  
</dependency>
```

它作为服务提供者，还需要对外提供接口，所以需要引入：
```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-web</artifactId>  
</dependency>
```

### 1.2.2 配置
对于Spring MVC，可以设置端口号和应用名：
```properties
server.port=8081  
spring.application.name=provider-service
```

对于Nacos，需要设置服务注册地址和服务名：
```properties
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
spring.cloud.nacos.discovery.service=provider-service
```

实际上，服务名默认会取`spring.application.name`配置：
```java
@Value("${spring.cloud.nacos.discovery.service:${spring.application.name:}}")  
private String service;
```

如果设置了Nacos用户名和密码，也需要添加相关配置：
```properties
spring.cloud.nacos.discovery.username=root  
spring.cloud.nacos.discovery.password=root
```

### 1.2.3 暴露接口
接着，我们可以按照Spring MVC的功能编写接口：
```java
@RestController  
public class TestController {  
    @GetMapping("/test")  
    public String test() {  
        return "test";  
    }  
}
```

### 1.2.4 启动
需要在配置类中添加`EnableDiscoverClient`注解：
```java
@SpringBootApplication  
@EnableDiscoveryClient  
public class Main {  
    public static void main(String[] args) {  
        SpringApplication.run(Main.class, args);  
    }  
}
```

## 1.3 服务消费者
### 1.2.1 依赖
服务消费者需要将自己注册到Nacos，所以需要引入：
```xml
<dependency>  
    <groupId>com.alibaba.cloud</groupId>  
    <artifactId>spring-cloud-starter-alibaba-nacos-discovery</artifactId>  
</dependency>
```

### 1.2.2 配置
对于Nacos配置，需要设置服务注册地址和服务名：
```properties
spring.cloud.nacos.discovery.server-addr=127.0.0.1:8848
spring.cloud.nacos.discovery.service=provider-consumer
```

如果设置了Nacos用户名和密码，也需要添加相关配置：
```properties
spring.cloud.nacos.discovery.username=root  
spring.cloud.nacos.discovery.password=root
```

### 1.2.3 调用接口
接着，我们可以使用Nacos提供的`LoadBalancerClient`获取第三方微服务信息，调用第三方接口：
```java
@Component  
public class Consumer implements ApplicationRunner {  
    @Autowired  
    private LoadBalancerClient loadBalancerClient;  
  
    @Override  
    public void run(ApplicationArguments args) throws Exception {  
        loadBalancerClient.execute("service-provider", (instance) -> {  
            String host = instance.getHost();  
            int port = instance.getPort();  
            String url = "http://" + host + ":" + port + "/test";  
            // HTTP请求  
            String result = "响应";  
            return result;  
        });  
    }  
}
```

`LoadBalancerClient`配置案例如下：
```java
@Bean  
public SpringClientFactory springClientFactory() {  
    return new SpringClientFactory();  
}  
  
@Bean  
public LoadBalancerClient loadBalancerClient(SpringClientFactory springClientFactory) {  
    return new RibbonLoadBalancerClient(springClientFactory);  
}
```

### 1.2.4 启动
需要在配置类中添加`EnableDiscoverClient`注解：
```java
@SpringBootApplication  
@EnableDiscoveryClient  
public class Main {  
    public static void main(String[] args) {  
        SpringApplication.run(Main.class, args);  
    }  
}
```

# 2 原理
