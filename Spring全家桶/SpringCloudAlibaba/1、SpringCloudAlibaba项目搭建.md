Spring Cloud Alibaba作为一个微服务架构，往往会创建一个父工程管理整个项目的依赖关系。每个子项目代表一个微服务，可以各自选择所需的组件进行使用。

因此，搭建Spring Cloud Alibaba项目总的来说包括两个步骤：
1. 创建父工程，统一管理全局微服务依赖。
2. 创建子服务，引入所需的特定组件进行业务开发。

# 1 父工程
在IDEA中，我们首先需要创建空的Maven父工程。

Spring Cloud Alibaba依赖于Spring Boot和Spring Cloud，我们需要在父工程的`pom.xml`中添加全局的微服务依赖管理：
```java
<parent>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-parent</artifactId>  
    <version>2.3.12.RELEASE</version>  
    <relativePath/>  
</parent>
  
<dependencyManagement>  
    <dependencies>  
        <dependency>  
            <groupId>org.springframework.cloud</groupId>  
            <artifactId>spring-cloud-dependencies</artifactId>  
            <version>Hoxton.SR12</version>  
            <type>pom</type>  
            <scope>import</scope>  
        </dependency>  
        <dependency>  
            <groupId>com.alibaba.cloud</groupId>  
            <artifactId>spring-cloud-alibaba-dependencies</artifactId>  
            <version>2.2.9.RELEASE</version>  
            <type>pom</type>  
            <scope>import</scope>  
        </dependency>  
    </dependencies>  
</dependencyManagement>
```

`spring-cloud-dependencies`管理着Spring Cloud各个组件的对应版本，`spring-cloud-alibaba-denpendencies`管理着Spring Cloud Alibaba各个组件的对应版本。

在子服务中，引入组件依赖时不必指定版本，会从上述依赖管理中获取对应的版本信息，避免依赖冲突。

需要注意的是，Spring Boot、Spring Cloud和Spring Cloud Alibaba这三者之间也存在着适配版本，需要我们手动进行指定。
![[Pasted image 20230211125653.png]]

最新的适配版本可以查看官网（[版本说明 · alibaba/spring-cloud-alibaba Wiki (github.com)](https://github.com/alibaba/spring-cloud-alibaba/wiki/%E7%89%88%E6%9C%AC%E8%AF%B4%E6%98%8E)）。

# 2 子服务
