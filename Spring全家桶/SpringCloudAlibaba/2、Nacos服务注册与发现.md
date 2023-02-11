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


## 1.3 服务消费者


# 2 原理
