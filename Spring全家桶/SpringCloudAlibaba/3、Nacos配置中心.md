> 预备知识：[SpringCloudAlibaba项目搭建流程](https://www.cnblogs.com/Xianhuii/p/17111321.html)
# 1 使用
## 1.1 Nacos服务器
参考文档：[Nacos 快速开始](https://nacos.io/zh-cn/docs/quick-start.html)。。

在公司中，Nacos服务器一般不用我们开发人员去搭建。

但是在学习时，需要在本地电脑搭建简单的Nacos服务。

简单来说，包括以下两个步骤：
1. 下载：[Releases · alibaba/nacos (github.com)](https://github.com/alibaba/nacos/releases)
2. 启动：`startup.cmd -m standalone`

## 1.2 Nacos配置
首先，通常需要创建命名空间，会生成唯一的`命名空间ID`：
![[Pasted image 20230211202721.png]]

然后，在对应命名空间中创建配置文件，主要是要指定`Data Id`，格式为`应用名`-`profile`-`.文件格式`。
![[Pasted image 20230211202042.png]]

## 1.3 项目示例
### 1.3.1 依赖
作为配置客户端，需要配置连接配置中心的依赖：
```xml
<dependency>  
    <groupId>com.alibaba.cloud</groupId>  
    <artifactId>spring-cloud-starter-alibaba-nacos-config</artifactId>  
</dependency>
```

### 1.3.2 本地配置
>配置文件对应源码位于`com.alibaba.cloud.nacos.NacosConfigProperties`。

需要在`bootstrap.properties`中配置注册中心信息。

在本地配置必须指定配置应用名和中心的地址：
```properties
spring.application.name=myDataId
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
```

如果设置了Nacos用户名和密码，也需要添加相关配置：
```properties
spring.cloud.nacos.config.username=root  
spring.cloud.nacos.config.password=root
```

如果指定了命名空间，也需要进行配置：
```properties
spring.cloud.nacos.config.namespace=b93fda8d-6585-4aa2-8678-b0dc37511029
```

### 1.3.3 启动项目
启动项目不需要其他配置，像普通Spring Boot项目启动即可：
```java
@SpringBootApplication  
public class Main {  
    public static void main(String[] args) {  
        SpringApplication.run(Main.class, args);  
    }  
}
```

我们在项目中可以直接从`environment`中获取到配置中心的配置：
```java
@RestController  
public class TestController {  
    @Autowired  
    private Environment environment;  
  
    @GetMapping("/config")  
    public void config() {  
        System.out.println(environment.getProperty("name"));  
        System.out.println(environment.getProperty("age"));  
    }
}
```

Nacos支持将修改的配置文件实时推送给实例，这表示我们可以在不停止实例的情况下，动态更新配置信息。

# 2 理论
## 2.1 配置一致性模型
Nacos 配置管理一致性协议分为两个大部分：
- Server间一致性协议
- SDK 与 Server 的一致性协议

配置作为分布式系统中非强一致数据，在出现脑裂的时候可用性高于一致性，因此阿里配置中心是采用 AP 一致性协议。

### 2.1.1 Server间的一致性协议
#### 有 DB 模式（读写分离架构）
一致性的核心是 Server 与 DB 保持数据一致性，从而保证 Server 数据一致；Server 之间都是对等的。数据写任何一个 Server，优先持久化，持久化成功后异步通知其他节点到数据库中拉取最新配置值，并且通知写入成功。
![[img-65.jpg]]

#### 无 DB 模式
Server 间采用 Raft 协议保证数据一致性，方便用户本机运行，降低对存储依赖。

### 2.1.2 SDK 与 Server 的一致性协议
SDK 与 Server 一致性协议的核心是通过 MD5 值是否一致，如果不一致就拉取最新值。

Nacos 1.X 采用 Http 1.1 短链接模拟长链接，每 30s 发一个心跳跟 Server 对比 SDK 配置 MD5 值是否跟 Server 保持一致，如果一致就 hold 住链接，如果有不一致配置，就把不一致的配置返回，然后 SDK 获取最新配置值。

![[img-66.jpg]]

Nacos 2.x 相比上面 30s 一次的长轮训，升级成长链接模式，配置变更，启动建立长链接，配置变 更服务端推送变更配置列表，然后 SDK 拉取配置更新，因此通信效率大幅提升。

# 3 源码
TODO……