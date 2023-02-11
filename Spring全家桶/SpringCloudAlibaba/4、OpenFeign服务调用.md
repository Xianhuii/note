# 1 使用
## 1.1 依赖
为了使用OpenFeign，首先需要引入相关依赖：
```xml
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-openfeign</artifactId>  
</dependency>
```

## 1.2 配置
在`application.properties`中，我们可以配置针对特定第三方微服务的基本配置：
```properteis
feign.client.config.service1.connect-timeout=5000  
feign.client.config.service1.read-timeout=5000
```

当然，这些基本配置有默认值，我们也可以直接使用默认值。

## 1.3 业务开发
首先要为第三方微服务定义一个请求接口。通过`@FeignClient`设置第三方微服务的`服务名`，然后再定义请求接口：
```java
@FeignClient("service1")  
public interface Service1Client {  
    @GetMapping("/test")  
    String test();  
}
```

然后，我们在业务中就可以直接调用这个接口了：
```java
@RestController  
public class ConsumerController {  
    @Autowired  
    private Service1Client service1Client;  

    @GetMapping("/consumer2")  
    public void consumer2() {  
        System.out.println(service1Client.test());  
    }  
}
```

OpenFeign可以和Nacos服务注册与发现功能自动配合，只需要设置好`服务名`，不需要我们额外配置。

## 1.4 启动
在启动时需要在配置类中标注`@EnableFeignClients`：
```java
@SpringBootApplication  
@EnableFeignClients  
public class Main {  
    public static void main(String[] args) {  
        SpringApplication.run(Main.class, args);  
    }  
}
```

# 2 源码
TODO……