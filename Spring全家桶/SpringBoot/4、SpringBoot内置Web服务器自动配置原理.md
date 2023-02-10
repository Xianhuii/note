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
4. 在`XxxWebServerApplicationContext`的`onfresh()`阶段，使用`XxxWebServerFactory`创建`WebServer`，并监听指定端口。
5. 在`WebServerFactoryCustomizerBeanPostProcessor`中，使用`WebServerFactory`对`WebServerFactory`的bean对象进行自定义配置。

# 1 内置Web服务器自动配置原理（以Tomcat为例）
![[TomcatWebServer 1.png]]

上图展示了内置Tomcat自动配置的相关类图，包括五个核心模块：
- EmbeddedWebServerFactoryCustomizerAutoConfiguration：注册TomcatWebServerFactoryCustomizer。
- ServletWebServerFactoryAutoConfiguration/ReactiveWebServerFactoryAutoConfiguration：注册WebServerFactoryCustomizerBeanPostProcessor和TomcatServletWebServerFactory
- WebServerFactoryCustomizerBeanPostProcessor：在TomcatServletWebServerFactory初始化前，使用TomcatWebServerFactoryCustomizer对其进行配置。
- TomcatServletWebServerFactory：创建TomcatWebServer。
- ServletWebServerApplicationContext：使用TomcatServletWebServerFactory创建TomcatWebServer。

## 1.1 EmbeddedWebServerFactoryCustomizerAutoConfiguration
`EmbeddedWebServerFactoryCustomizerAutoConfiguration`会根据是否添加依赖，来注册对应的`WebServerFactoryCustomizer`实现类：
- TomcatWebServerFactoryCustomizer
- JettyWebServerFactoryCustomizer
- UndertowWebServerFactoryCustomizer
- NettyWebServerFactoryCustomizer

`EmbeddedWebServerFactoryCustomizerAutoConfiguration`源码如下：
```java
@AutoConfiguration  
@ConditionalOnWebApplication  
@EnableConfigurationProperties(ServerProperties.class)  // 注册ServerProperties，读取配置信息
public class EmbeddedWebServerFactoryCustomizerAutoConfiguration {  
  
   /**  
    * Nested configuration if Tomcat is being used.    
    */   
   @Configuration(proxyBeanMethods = false)  
   @ConditionalOnClass({ Tomcat.class, UpgradeProtocol.class })  
   public static class TomcatWebServerFactoryCustomizerConfiguration {  
  
      @Bean  
      public TomcatWebServerFactoryCustomizer tomcatWebServerFactoryCustomizer(Environment environment,  
            ServerProperties serverProperties) {  
         return new TomcatWebServerFactoryCustomizer(environment, serverProperties);  
      }  
  
   }  
  
   /**  
    * Nested configuration if Jetty is being used.    
    */   
   @Configuration(proxyBeanMethods = false)  
   @ConditionalOnClass({ Server.class, Loader.class, WebAppContext.class })  
   public static class JettyWebServerFactoryCustomizerConfiguration {  
  
      @Bean  
      public JettyWebServerFactoryCustomizer jettyWebServerFactoryCustomizer(Environment environment,  
            ServerProperties serverProperties) {  
         return new JettyWebServerFactoryCustomizer(environment, serverProperties);  
      }  
  
   }  
  
   /**  
    * Nested configuration if Undertow is being used.    
    */   
   @Configuration(proxyBeanMethods = false)  
   @ConditionalOnClass({ Undertow.class, SslClientAuthMode.class })  
   public static class UndertowWebServerFactoryCustomizerConfiguration {  
  
      @Bean  
      public UndertowWebServerFactoryCustomizer undertowWebServerFactoryCustomizer(Environment environment,  
            ServerProperties serverProperties) {  
         return new UndertowWebServerFactoryCustomizer(environment, serverProperties);  
      }  
  
   }  
  
   /**  
    * Nested configuration if Netty is being used.    
    */   
   @Configuration(proxyBeanMethods = false)  
   @ConditionalOnClass(HttpServer.class)  
   public static class NettyWebServerFactoryCustomizerConfiguration {  
  
      @Bean  
      public NettyWebServerFactoryCustomizer nettyWebServerFactoryCustomizer(Environment environment,  
            ServerProperties serverProperties) {  
         return new NettyWebServerFactoryCustomizer(environment, serverProperties);  
      }  
  
   }  
  
}
```

## 1.2 ServletWebServerFactoryAutoConfiguration
`ServletWebServerFactoryAutoConfiguration`会注册以下bean：
- ServletWebServerFactoryCustomizer、TomcatServletWebServerFactoryCustomizer（Tomcat内置服务器时）
- WebServerFactoryCustomizerBeanPostProcessor
- TomcatServletWebServerFactory/JettyServletWebServerFactory/UndertowServletWebServerFactory

`ServletWebServerFactoryAutoConfiguration`部分源码如下：
```java
@AutoConfiguration  
@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)  
@ConditionalOnClass(ServletRequest.class)  
@ConditionalOnWebApplication(type = Type.SERVLET)  
@EnableConfigurationProperties(ServerProperties.class)  
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class,  
      ServletWebServerFactoryConfiguration.EmbeddedTomcat.class,  
      ServletWebServerFactoryConfiguration.EmbeddedJetty.class,  
      ServletWebServerFactoryConfiguration.EmbeddedUndertow.class })  
public class ServletWebServerFactoryAutoConfiguration {  
  
   @Bean  
   public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,  
         ObjectProvider<WebListenerRegistrar> webListenerRegistrars,  
         ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers) {  
      return new ServletWebServerFactoryCustomizer(serverProperties,  
            webListenerRegistrars.orderedStream().collect(Collectors.toList()),  
            cookieSameSiteSuppliers.orderedStream().collect(Collectors.toList()));  
   }  
  
   @Bean  
   @ConditionalOnClass(name = "org.apache.catalina.startup.Tomcat")  
   public TomcatServletWebServerFactoryCustomizer tomcatServletWebServerFactoryCustomizer(  
         ServerProperties serverProperties) {  
      return new TomcatServletWebServerFactoryCustomizer(serverProperties);  
   }    
}
```

通过`@Import(ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class)`会注册WebServerFactoryCustomizerBeanPostProcessor：
```java
public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata,  
      BeanDefinitionRegistry registry) {  
   if (this.beanFactory == null) {  
      return;  
   }  
   registerSyntheticBeanIfMissing(registry, "webServerFactoryCustomizerBeanPostProcessor",  
         WebServerFactoryCustomizerBeanPostProcessor.class,  
         WebServerFactoryCustomizerBeanPostProcessor::new);  
   registerSyntheticBeanIfMissing(registry, "errorPageRegistrarBeanPostProcessor",  
         ErrorPageRegistrarBeanPostProcessor.class, ErrorPageRegistrarBeanPostProcessor::new);  
}
```

通过`@Import(ServletWebServerFactoryConfiguration.EmbeddedXxx.class)`会注册对应的XxxServletWebServerFactory，例如：
```java
@Configuration(proxyBeanMethods = false)  
@ConditionalOnClass({ Servlet.class, Tomcat.class, UpgradeProtocol.class })  
@ConditionalOnMissingBean(value = ServletWebServerFactory.class, search = SearchStrategy.CURRENT)  
static class EmbeddedTomcat {  
  
   @Bean  
   TomcatServletWebServerFactory tomcatServletWebServerFactory(  
         ObjectProvider<TomcatConnectorCustomizer> connectorCustomizers,  
         ObjectProvider<TomcatContextCustomizer> contextCustomizers,  
         ObjectProvider<TomcatProtocolHandlerCustomizer<?>> protocolHandlerCustomizers) {  
      TomcatServletWebServerFactory factory = new TomcatServletWebServerFactory();  
      factory.getTomcatConnectorCustomizers()  
            .addAll(connectorCustomizers.orderedStream().collect(Collectors.toList()));  
      factory.getTomcatContextCustomizers()  
            .addAll(contextCustomizers.orderedStream().collect(Collectors.toList()));  
      factory.getTomcatProtocolHandlerCustomizers()  
            .addAll(protocolHandlerCustomizers.orderedStream().collect(Collectors.toList()));  
      return factory;  
   }  
  
}
```

## 1.3 XxxWebServerApplicationContext
在`XxxWebServerApplicationContext#onfresh()`中，会创建webServer。需要注意的是，此时单例bean还没有初始化。

例如，`ServletWebServerApplicationContext#onRefresh()`：
```java
protected void onRefresh() {  
   super.onRefresh();  
   try {  
      createWebServer();  
   }  
   catch (Throwable ex) {  
      throw new ApplicationContextException("Unable to start web server", ex);  
   }  
}
```

在`ServletWebServerApplicationContext#createWebServer()`方法中，会创建ServletWebServerFactory的bean对象，此时会执行WebServerFactoryCustomizerBeanPostProcessor回调。然后再创建webServer，监听指定端口：
```java
private void createWebServer() {  
   WebServer webServer = this.webServer;  
   ServletContext servletContext = getServletContext();  
   if (webServer == null && servletContext == null) {  
      StartupStep createWebServer = this.getApplicationStartup().start("spring.boot.webserver.create");  
      // 从容器中获取ServletWebServerFactory
      ServletWebServerFactory factory = getWebServerFactory();  
      createWebServer.tag("factory", factory.getClass().toString());  
      // 创建webServer
      this.webServer = factory.getWebServer(getSelfInitializer());  
      createWebServer.end();  
      getBeanFactory().registerSingleton("webServerGracefulShutdown",  
            new WebServerGracefulShutdownLifecycle(this.webServer));  
      getBeanFactory().registerSingleton("webServerStartStop",  
            new WebServerStartStopLifecycle(this, this.webServer));  
   }  
   else if (servletContext != null) {  
      try {  
         getSelfInitializer().onStartup(servletContext);  
      }  
      catch (ServletException ex) {  
         throw new ApplicationContextException("Cannot initialize servlet context", ex);  
      }  
   }  
   // 初始化servletContext环境配置
   initPropertySources();  
}
```

`ServletWebServerApplicationContext#getWebServerFactory()`方法会从容器中获取ServletWebServerFactory。由于此时还没有初始化单例对象，因此会执行创建bean的流程：
```java
protected ServletWebServerFactory getWebServerFactory() {  
   // Use bean names so that we don't consider the hierarchy  
   String[] beanNames = getBeanFactory().getBeanNamesForType(ServletWebServerFactory.class);  
   if (beanNames.length == 0) {  
      throw new MissingWebServerFactoryBeanException(getClass(), ServletWebServerFactory.class,  
            WebApplicationType.SERVLET);  
   }  
   if (beanNames.length > 1) {  
      throw new ApplicationContextException("Unable to start ServletWebServerApplicationContext due to multiple "  
            + "ServletWebServerFactory beans : " + StringUtils.arrayToCommaDelimitedString(beanNames));  
   }  
   return getBeanFactory().getBean(beanNames[0], ServletWebServerFactory.class);  
}
```

根据之前注册的不同`XxxWebServerFactory`，此时会调用对应实现类创建webServer。例如，`TomcatServletWebServerFactory#getWebServer()`：
```java
public WebServer getWebServer(ServletContextInitializer... initializers) {  
   if (this.disableMBeanRegistry) {  
      Registry.disableRegistry();  
   }  
   // 创建Tomcat容器
   Tomcat tomcat = new Tomcat();  
   File baseDir = (this.baseDirectory != null) ? this.baseDirectory : createTempDir("tomcat");  
   tomcat.setBaseDir(baseDir.getAbsolutePath());  
   for (LifecycleListener listener : this.serverLifecycleListeners) {  
      tomcat.getServer().addLifecycleListener(listener);  
   }  
   // 创建连接器
   Connector connector = new Connector(this.protocol);  
   connector.setThrowOnFailure(true);  
   tomcat.getService().addConnector(connector);  
   customizeConnector(connector);  
   tomcat.setConnector(connector);  
   tomcat.getHost().setAutoDeploy(false);  
   // 配置引擎
   configureEngine(tomcat.getEngine());  
   for (Connector additionalConnector : this.additionalTomcatConnectors) {  
      tomcat.getService().addConnector(additionalConnector);  
   }  
   prepareContext(tomcat.getHost(), initializers);  
   // 创建Tomcat服务器，并启动
   return getTomcatWebServer(tomcat);  
}
```

## 1.4 WebServerFactoryCustomizerBeanPostProcessor
在ServletWebServerFactory的创建过程中，会触发`WebServerFactoryCustomizerBeanPostProcessor#postProcessBeforeInitialization()`方法：
```java
public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {  
   if (bean instanceof WebServerFactory) {  
      postProcessBeforeInitialization((WebServerFactory) bean);  
   }  
   return bean;  
}
```

它会调用之前注册的所有WebServerFactoryCustomizer实现类，对WebServerFactory进行自定义配置：
```java
private void postProcessBeforeInitialization(WebServerFactory webServerFactory) {  
   LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory)  
         .withLogger(WebServerFactoryCustomizerBeanPostProcessor.class)  
         .invoke((customizer) -> customizer.customize(webServerFactory));  
}
```
