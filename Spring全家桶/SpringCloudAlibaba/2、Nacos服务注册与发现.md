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
接着，我们可以

## 1.3 服务消费者


# 2 原理
