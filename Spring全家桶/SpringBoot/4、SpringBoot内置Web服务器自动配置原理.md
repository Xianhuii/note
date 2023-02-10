SpringBoot为Web应用提供了内置Web服务器，我们不用再额外下载`Tomcat`、`Jetty`、`Undertow`等服务器。

`spring-boot-autoconfigure`中提供了自动配置内置Web服务器的功能，只要添加了相关依赖，就会配置对应的Web服务器。

对于`spring-boot-starter-web`：
- `spring-boot-starter-tomcat`（默认）：内置Tomcat服务器。
- `spring-boot-starter-jetty`：内置Jetty服务器。
- `spring-boot-starter-undertow`：内置Undertow服务器。

对于`spring-boot-starter-webflux`：
- `spring-boot-starter-reactor-netty`（默认）：使用Netty监听网络请求。
- `spring-boot-starter-tomcat`：内置Tomcat服务器。
- `spring-boot-starter-jetty`：内置Jetty服务器。
- `spring-boot-starter-undertow`：内置Undertow服务器。

如果我们不想使用默认内置Web服务器，需要先移除默认值，然后导入需要的：
```xml
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-web</artifactId>  
    <exclusions>  
        <exclusion>  
            <groupId>org.springframework.boot</groupId>  
            <artifactId>spring-boot-starter-tomcat</artifactId>  
        </exclusion>  
    </exclusions>  
</dependency>  
<dependency>  
    <groupId>org.springframework.boot</groupId>  
    <artifactId>spring-boot-starter-jetty</artifactId>  
    <version>2.7.8</version>  
</dependency>
```

内置Web服务器的自动配置基于SpringBoot自动配置SPI机制和BeanPostProcessor机制。简单来说包括以下步骤：
1. 在`org.springframework.boot.autoconfigure.AutoConfiguration.imports`中定义自动配置类：
	- EmbeddedWebServerFactoryCustomizerAutoConfiguration
	- ServletWebServerFactoryAutoConfiguration
	- ReactiveWebServerFactoryAutoConfiguration
2. 在`EmbeddedWebServerFactoryCustomizerAutoConfiguration`中注册`WebServerFactoryCustomizer`实现类。
3. 在`ServletWebServerFactoryAutoConfiguration`或`ReactiveWebServerFactoryAutoConfiguration`中注册`WebServerFactoryCustomizerBeanPostProcessor`和`XxxWebServerFactory`。
4. 在`WebServerFactoryCustomizerBeanPostProcessor`中，使用`WebServerFactory`对`WebServerFactory`的bean对象进行自定义配置。
5. 在`XxxWebServerApplicationContext`的`onfresh()`阶段，使用`XxxWebServerFactory`创建`WebServer`，并监听指定端口。

# 1 内置Tomcat自动配置原理
![[TomcatWebServer 1.png]]

上图展示了内置Tomcat自动配置的相关类图，包括五个核心模块：
- EmbeddedWebServerFactoryCustomizerAutoConfiguration：注册TomcatWebServerFactoryCustomizer。
- ServletWebServerFactoryAutoConfiguration：注册WebServerFactoryCustomizerBeanPostProcessor和TomcatServletWebServerFactory
- 