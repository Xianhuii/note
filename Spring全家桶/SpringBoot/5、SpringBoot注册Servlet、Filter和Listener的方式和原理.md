# 1 实战
在Spring Boot项目中，如果使用内嵌Web服务器，可以很方便地注册`Servlet`、`Filter`和`Listener`等组件。

总的来说，包括以下方式：
- 开启`@ServletCompnentScan`功能，扫描标注`@WebServlet`、`@WebFilter`或`WebListener`的bean。
- 创建继承`RegistrationBean`的bean，自定义注册的组件。
- 

# 2 源码
## 2.1 ServletContextInitializer
在`ServletWebServerApplicationContext`的`onRefresh()`阶段，会创建Web服务器：
```java
protected void onRefresh() {  
   // 调用父类的onRefresh()方法
   super.onRefresh();  
   try {  
      // 创建web服务器
      createWebServer();  
   }  
   catch (Throwable ex) {  
      // 抛异常
   }  
}
```

在`ServletWebServerApplicationContext#createWebServer()`方法中，会创建并初始化web服务器：
```java
private void createWebServer() {  
   WebServer webServer = this.webServer;  
   ServletContext servletContext = getServletContext();  
   if (webServer == null && servletContext == null) {  
      // 获取factory
      ServletWebServerFactory factory = getWebServerFactory();  
      // 传入getSelfInitializer()，创建web服务器
      this.webServer = factory.getWebServer(getSelfInitializer());  
   }  
   else if (servletContext != null) {  
      // 初始化servletContext
      getSelfInitializer().onStartup(servletContext);  
   }  
}
```

`ServletWebServerApplicationContext#getSelfInitializer()`是一个方法引用（匿名对象）：
```java
private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {  
   return this::selfInitialize;  
}
```

会在后续服务器初始化调用`ServletContextInitializer#onStartup()`方法时，触发`ServletWebServerApplicationContext#selfInitialize()`方法：
```java
private void selfInitialize(ServletContext servletContext) throws ServletException {  
   // 遍历调用org.springframework.boot.web.servlet.ServletContextInitializer#onStartup()
   for (ServletContextInitializer beans : getServletContextInitializerBeans()) {  
      beans.onStartup(servletContext);  
   }  
}
```

对于内嵌Tomcat，它的初始化流程如下：
1. `TomcatServletWebServerFactory#getWebServer()`和`TomcatWebServer#TomcatWebServer()`方法创建服务器。
2. 在创建服务器过程中，将`selfInitialize`添加到`TomcatStarter`中。
3. `TomcatWebServer#initialize()`方法初始化服务器。
4. `Tomcat#start()`和`LifecycleBase#start()`启动服务器。
5. `StandardContext#startInternal()`启动上下文。
6. `javax.servlet.ServletContainerInitializer#onStartup()`初始化容器。

`org.springframework.boot.web.embedded.tomcat.TomcatStarter`实现了`javax.servlet.ServletContainerInitializer`，所以会直接触发前者的`onStartup()`方法。需要注意全限定类名的不同，`TomcatStarter`在这里起着适配器的作用：
```java
public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {  
   try {  
      // 遍历所有org.springframework.boot.web.servlet.ServletContextInitializer
      for (ServletContextInitializer initializer : this.initializers) {  
         initializer.onStartup(servletContext);  
      }  
   }  
   catch (Exception ex) {   
   }  
}
```

理论上，我们只需要注册实现`org.springframework.boot.web.servlet.ServletContextInitializer`的bean，就可以在服务器初始化时进行额外操作，例如注册`Servlet`、`Filter`和`Listener`。

实际上，Spring提供了专门用来注册`Servlet`、`Filter`和`Listener`的实现类，它们定义好了注册三大组件的模板方法，更加方便开发人员使用：
- `org.springframework.boot.web.servlet.ServletRegistrationBean`：注册Servlet。
- `org.springframework.boot.web.servlet.FilterRegistrationBean`：注册Filter。
- `org.springframework.boot.web.servlet.ServletListenerRegistrationBean`：注册Listener。

这些实现的使用方式都十分简单，只需要设置对应成员变量即可。

org.springframework.boot.web.servlet.ServletRegistrationBean：
![[ServletRegistrationBean.png]]

org.springframework.boot.web.servlet.FilterRegistrationBean：
![[FilterRegistrationBean.png]]

org.springframework.boot.web.servlet.ServletListenerRegistrationBean
![[ServletListenerRegistrationBean.png]]

## 2.2 @ServletComponentScan
将`@ServletComponentScan`标注到配置类上，会将指定路径下标注`@WebServlet`、`@WebFilter`或`@WebListener`的bean注册到web服务器中。

`@ServletComponentScan`源码如下，它会导入`ServletComponentScanRegistrar`：
```java
@Import(ServletComponentScanRegistrar.class)  
public @interface ServletComponentScan {  
   @AliasFor("basePackages")  
   String[] value() default {};  
  
   @AliasFor("value")  
   String[] basePackages() default {};  
   
   Class<?>[] basePackageClasses() default {};  
}
```

`ServletComponentScanRegistrar`会注册`servletComponentRegisteringPostProcessor`作为BeanFactoryPostProcessor。`ServletComponentScanRegistrar#registerBeanDefinitions()`：
```java
public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {  
   Set<String> packagesToScan = getPackagesToScan(importingClassMetadata);  
   if (registry.containsBeanDefinition(BEAN_NAME)) {  
      updatePostProcessor(registry, packagesToScan);  
   }  
   else {  
      addPostProcessor(registry, packagesToScan);  
   }  
}
```

`ServletComponentRegisteringPostProcessor#postProcessBeanFactory()`方法会从容器中获取指定路径下、标注`@WebServlet`、`@WebFilter`或`@WebListener`的BeanDefinition，然后使用ServletComponentHandler进行处理：
```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {  
   if (isRunningInEmbeddedWebServer()) {  
      ClassPathScanningCandidateComponentProvider componentProvider = createComponentProvider();  
      for (String packageToScan : this.packagesToScan) {  
         // 扫描指定路径
         scanPackage(componentProvider, packageToScan);  
      }  
   }  
}
```

`ServletComponentRegisteringPostProcessor#scanPackage()`方法中定义了实际扫描逻辑：
```java
private void scanPackage(ClassPathScanningCandidateComponentProvider componentProvider, String packageToScan) {  
   // 获取指定路径下的BeanDefinition
   for (BeanDefinition candidate : componentProvider.findCandidateComponents(packageToScan)) {  
      // 过滤注解标注的BeanDefinition
      if (candidate instanceof AnnotatedBeanDefinition) {  
         // 使用ServletComponentHandler进行分别处理
         for (ServletComponentHandler handler : HANDLERS) {  
            handler.handle(((AnnotatedBeanDefinition) candidate),  
                  (BeanDefinitionRegistry) this.applicationContext);  
         }  
      }  
   }  
}
```

`org.springframework.boot.web.servlet.ServletComponentHandler#handle()`方法会对`@WebServlet`、`@WebFilter`或`@WebListener`进行过滤：
```java
void handle(AnnotatedBeanDefinition beanDefinition, BeanDefinitionRegistry registry) {  
   Map<String, Object> attributes = beanDefinition.getMetadata()  
         .getAnnotationAttributes(this.annotationType.getName());  
   if (attributes != null) {  
      doHandle(attributes, beanDefinition, registry);  
   }  
}
```

`WebServletHandler#doHandle()`方法会注册一个`ServletRegistrationBean`：
```java
public void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,  
      BeanDefinitionRegistry registry) {  
   BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(ServletRegistrationBean.class);  
   builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));  
   builder.addPropertyValue("initParameters", extractInitParameters(attributes));  
   builder.addPropertyValue("loadOnStartup", attributes.get("loadOnStartup"));  
   String name = determineName(attributes, beanDefinition);  
   builder.addPropertyValue("name", name);  
   // 设置servlet
   builder.addPropertyValue("servlet", beanDefinition);  
   builder.addPropertyValue("urlMappings", extractUrlPatterns(attributes));  
   builder.addPropertyValue("multipartConfig", determineMultipartConfig(beanDefinition));  
   registry.registerBeanDefinition(name, builder.getBeanDefinition());  
}
```

`WebFilterHandler#doHandle()`方法会注册一个`FilterRegistrationBean`：
```java
public void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,  
      BeanDefinitionRegistry registry) {  
   BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FilterRegistrationBean.class);  
   builder.addPropertyValue("asyncSupported", attributes.get("asyncSupported"));  
   builder.addPropertyValue("dispatcherTypes", extractDispatcherTypes(attributes));  
   builder.addPropertyValue("filter", beanDefinition);  
   builder.addPropertyValue("initParameters", extractInitParameters(attributes));  
   String name = determineName(attributes, beanDefinition);  
   builder.addPropertyValue("name", name);  
   builder.addPropertyValue("servletNames", attributes.get("servletNames"));  
   builder.addPropertyValue("urlPatterns", extractUrlPatterns(attributes));  
   registry.registerBeanDefinition(name, builder.getBeanDefinition());  
}
```

`WebListenerHandler#doHandle()`方法会注册一个`ServletComponentWebListenerRegistrar`：
```java
protected void doHandle(Map<String, Object> attributes, AnnotatedBeanDefinition beanDefinition,  
      BeanDefinitionRegistry registry) {  
   BeanDefinitionBuilder builder = BeanDefinitionBuilder  
         .rootBeanDefinition(ServletComponentWebListenerRegistrar.class);  
   builder.addConstructorArgValue(beanDefinition.getBeanClassName());  
   registry.registerBeanDefinition(beanDefinition.getBeanClassName() + "Registrar", builder.getBeanDefinition());  
}
```

不同于其他两个，ServletComponentWebListenerRegistrar实现了`WebListenerRegistrar`接口，会在`org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryCustomizer#customize()`方法被触发：
```java
public void customize(ConfigurableServletWebServerFactory factory) {  
   for (WebListenerRegistrar registrar : this.webListenerRegistrars) {  
      registrar.register(factory);  
   }  
}
```

`ServletWebServerFactoryCustomizer`会在