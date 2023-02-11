> 预备知识：[SpringCloudAlibaba项目搭建流程](https://www.cnblogs.com/Xianhuii/p/17111321.html)
# 1 使用
## 1.1 Nacos服务器
参考文档：[Nacos 快速开始](https://nacos.io/zh-cn/docs/quick-start.html)。。

在公司中，Nacos服务器一般不用我们开发人员去搭建。

但是在学习时，需要在本地电脑搭建简单的Nacos服务。

简单来说，包括以下两个步骤：
1. 下载：[Releases · alibaba/nacos (github.com)](https://github.com/alibaba/nacos/releases)
2. 启动：`startup.cmd -m standalone`

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
接着，我们可以使用Nacos默认提供的`LoadBalancerClient`获取第三方微服务信息，调用第三方接口：
```java
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

# 2 理论
> 参考《Nacos架构与原理》
## 2.1 数据模型
在服务发现领域中，`服务`指的是由应用程序提供的一个或一组软件功能的一种抽象概念。

`服务实例`（以下简称实例）是某个服务的具体提供能力的节点，一个实例仅从属于一个服务，而一个服务可以包含一个或多个实例。

### 2.1.1 服务
在 Nacos 中，服务的定义包括以下几个内容：
- 命名空间（Namespace）
- 分组（Group）
- 服务名（Name）

服务的元数据是进一步定义了 Nacos 中服务的细节属性和描述信息：
- 健康保护阈值（ProtectThreshold）：0~1的浮点数。当域名健康实例数占总服务实例数的比例小于该值时，无论实例是否健康，都会将这个实例返回给客户端。这样做虽然损失了一部分流量，但是保证了集群中剩余健康实例能正常工作。
- 实例选择器（Selector）：用于在获取服务下的实例列表时，过滤和筛选实例。
- 拓展数据(extendData)：用于用户在注册实例时自定义扩展的元数据内容，形式为 K-V 。

Nacos 提供两种类型的服务：持久化服务和非持久化服务。

为了标示该服务是哪种类型的服务，需要在创建服务时选择服务的持久化属性。

考虑到目前大多数使用动态服务发现的场景 为非持久化服务的类型（如 Spring Cloud，Dubbo，Service Mesh 等），Nacos 将缺醒值设置为了非持久化服务。

### 2.1.2 实例
Nacos 在设计实例的定义时，主要需要存储该实例的 一些网络相关的基础信息：
- 网络 IP 地址
- 网络端口
- 健康状态（Healthy）：用于表示该实例是否为健康状态，会在 Nacos 中通过健康检查的手段进行维护。
- 集群（Cluster）：用于标示该实例归属于哪个逻辑集群。
- 拓展数据(extendData)：用于用户自定义扩展的元数据内容，形式为 K-V。

实例的元数据主要作用于实例运维相关的数据信息：
- 权重（Weight）：实例级别的配置。权重为浮点数，范围为 0-10000。权重越大，分配给该实例 的流量越大。
- 上线状态（Enabled）：标记该实例是否接受流量，优先级大于权重和健康状态。
- 拓展数据(extendData)：在不变动实例本身的情况下，快速地修改和新增实例的扩展数据，从而达到运维实例的作用。

### 2.1.3 集群
![[img-56.jpg]]
集群是 Nacos 中一组服务实例的一个逻辑抽象的概念，它介于服务和实例之间，是一部分服务属性的下沉和实例属性的抽象。

在 Nacos 中，集群中主要保存了有关健康检查的一些信息和数据：
- 健康检查类型（HealthCheckType）：使用哪种类型的健康检查方式，目前支持：TCP，HTTP， MySQL；设置为 NONE 可以关闭健康检查。
- 健康检查端口（HealthCheckPort）：设置用于健康检查的端口。
- 是否使用实例端口进行健康检查（UseInstancePort）：如果使用实例端口进行健康检查，将会使用实例定义中的网络端口进行健康检查，而不再使用上述设置的健康检查端口进行。
- 拓展数据(extendData)：用于用户自定义扩展的元数据内容，形式为 K-V 。

Nacos自动会提供实例集群功能，只要我们启动多个同一服务名的实例，它们就会注册到同一个集群中。

## 2.2 健康检查机制
Nacos 提供了两种服务类型供用户注册实例时选择，分为临时实例和永久实例。
![[img-49.jpg]]

### 2.2.1 客户端心跳上报（临时实例）
临时实例只是临时存在于注册中心中，会在服务下线或不可用时被注册中心剔除，临时实例会与注册中心保持心跳，注册中心会在一段时间没有收到来自客户端的心跳后会将实例设置为不健康，然后在一段时间后进行剔除。

用户可以通过两种方式进行临时实例的注册：`OpenAPI`和`SDK`。

OpenAPI 的注册方式实际是用户根据自身需求调用 Http 接口对服务进行注册，然后通过 Http 接口发送心跳到注册中心。

SDK 的注册方式实际是通过 RPC 与注册中心保持连接（Nacos 2.x 版本中，旧版的还是仍然通过 OpenAPI 的方式），客户端会定时的通过 RPC 连接向 Nacos 注册中心发送心跳，保持连接的存活。

![[img-60.jpg]]

### 2.2.2 服务端主动探测（永久实例）
永久实例在被删除之前会永久的存在于注册中心，且有可能并不知道注册中心存在，不会主动向注册中心上报心跳，那么这个时候就需要注册中心主动进行探活。

对于永久实例的的监看检查，Nacos 采用的是注册中心探测机制，注册中心会在永久服务初始化时根据客户端选择的协议类型注册探活的定时任务。

Nacos 现在内置提供了三种探测的协议，即`Http`、`TCP`以及`MySQL`。

![[img-61.jpg]]

Http和TCP探测协议都很容易理解，就是发送网络请求询问实例的健康状况。

MySQL探测协议主要用于特殊的业务场景，例如数据库的主备需要通过服务名对外提供访问，需要确定当前访问数据库是否为主库时，那么我们此时的健康检查接口，是一个检查数据库是否为主库的 MySQL 命令。

有些时候会有这样的场景，有些服务不希望去校验其健康状态，Nacos 也是提供了对应的白名单配置，用户可以将服务配置到该白名单，那么 Nacos 会放弃对其进行健康检查，实例的健康状态也始终为用户传入的健康状态。

## 2.3 集群模式下的健康检查机制
在Nacos集群模式下，一个服务只会被 Nacos 集群中的一个注册中心所负责，其余节点的服务信息只是集群副本，用于订阅者在查询服务列表时，始终可以获取到全部的服务列表。

Nacos集群间会通过网络通信同步所有服务的健康状况。

![[img-62.jpg]]

## 2.4 数据一致性

## 2.5 负载均衡

# 3 源码

