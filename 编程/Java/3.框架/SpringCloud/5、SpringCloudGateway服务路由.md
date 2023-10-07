> 预备知识：[SpringCloudAlibaba项目搭建流程](https://www.cnblogs.com/Xianhuii/p/17111321.html)
# 1 使用
## 1.1 依赖
首先要引入`spring-cloud-starter-gateway`的依赖：
```xml
<dependency>  
    <groupId>org.springframework.cloud</groupId>  
    <artifactId>spring-cloud-starter-gateway</artifactId>  
</dependency>
```

由于通常使用Nacos作为注册中心，所以也需要引入`spring-cloud-starter-alibaba-nacos-discovery`，这里就不过多介绍。

## 1.2 配置
在`application.yml`中，除了注册中心的相关配置，通常会开启根据`服务名`路由的规则：
```yml
spring:
  cloud:
    gateway:  
      discovery:  
        locator:  
          # 开启从注册中心动态创建路由的功能，利用微服务名进行路由  
          enabled: true
```

## 1.3 启动
按照普通项目启动即可：
```java
@SpringBootApplication  
public class Main {  
    public static void main(String[] args) {  
        SpringApplication.run(Main.class, args);  
    }  
}
```

# 2 理论
TODO……

# 3 源码
TODO……