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

然后，在对应命名空间中创建配置文件，主要是要指定`Data Id`作为配置文件名，便于识别。
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
在本地配置必须指定配置中心的地址：
```properties
spring.cloud.nacos.config.server-addr=127.0.0.1:8848
```

如果指定了命名空间，也需要进行配置：
```properties
spring.cloud.nacos.config.namespace=b93fda8d-6585-4aa2-8678-b0dc37511029
```

# 2 理论

# 3 源码
