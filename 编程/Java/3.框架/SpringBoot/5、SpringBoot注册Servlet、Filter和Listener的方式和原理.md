# 1 实战
在Spring Boot项目中，如果使用内嵌Web服务器，可以很方便地注册`Servlet`、`Filter`和`Listener`等组件。

总的来说，包括以下方式：
- 创建实现`ServletContextInitializer`接口的bean，自定义注册逻辑。
- 开启`@ServletCompnentScan`功能，扫描标注`@WebServlet`、`@WebFilter`或`WebListener`的bean。
- 注册实现`Servlet`、`Filter`或`Listener`接口的bean。

## 1.1 实现`ServletContextInitializer`接口
Spring提供了`ServletRegistrationBean`、`FilterRegistrationBean`和`ServletListenerRegistrationBean`，只需要创建对应bean即可：
```java
@Bean  
public ServletRegistrationBean servletRegistrationBean() {  
    ServletRegistrationBean servletRegistrationBean = new ServletRegistrationBean();  
    // servletRegistrationBean.setServlet(); 设置Servlet  
    // servletRegistrationBean.addUrlMappings(""); 设置映射地址  
    return servletRegistrationBean;  
}  
  
@Bean  
public FilterRegistrationBean filterRegistrationBean() {  
    FilterRegistrationBean filterRegistrationBean = new FilterRegistrationBean();  
    // filterRegistrationBean.setFilter(); 设置Filter  
    return filterRegistrationBean;  
}  
  
@Bean  
public ServletListenerRegistrationBean servletListenerRegistrationBean() {  
    ServletListenerRegistrationBean servletListenerRegistrationBean = new ServletListenerRegistrationBean();  
    // servletListenerRegistrationBean.setListener(); 设置Listener  
    return servletListenerRegistrationBean;  
}
```

## 1.2 开启`@ServletCompnentScan`功能
在配置类中开启功能：
```java
@Configuration  
@ServletComponentScan  
public class ServletContextConfig {
}
```

在对应包下添加对应bean：
```java
@Component  
@WebServlet  
public class MyServlet implements Servlet {  
}

@Component  
@WebFilter  
public class MyFilter implements Filter {  
}

@Component  
@WebListener  
public class MyListener implements EventListener {  
}
```

## 1.3 实现`Servlet`、`Filter`或`Listener`接口
实现`Servlet`、`Filter`或`Listener`接口:
```java
@Component  
public class MyServlet implements Servlet {  
}

@Component  
public class MyFilter implements Filter {  
}

@Component  
public class MyListener implements EventListener {  
}
```

# 2 源码
![[ServletContextInitializer.png]]
## 2.1 ServletContextInitializer
> web服务器在初始化时，会调用ServletContextInitializer#onStartup()方法进行自定义注册。

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

`ServletWebServerApplicationContext#getServletContextInitializerBeans()`方法会返回`ServletContextInitializerBeans`对象：
```java
protected Collection<ServletContextInitializer> getServletContextInitializerBeans() {  
   return new ServletContextInitializerBeans(getBeanFactory());  
}
```

`ServletContextInitializerBeans#ServletContextInitializerBeans()`方法中定义了获取ServletContextInitializer的逻辑：
```java
public ServletContextInitializerBeans(ListableBeanFactory beanFactory,  
      Class<? extends ServletContextInitializer>... initializerTypes) {  
   this.initializers = new LinkedMultiValueMap<>();  
   this.initializerTypes = (initializerTypes.length != 0) ? Arrays.asList(initializerTypes)  
         : Collections.singletonList(ServletContextInitializer.class);  
   // 添加ServletContextInitializer
   addServletContextInitializerBeans(beanFactory);  
   addAdaptableBeans(beanFactory);  
   List<ServletContextInitializer> sortedInitializers = this.initializers.values().stream()  
         .flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE))  
         .collect(Collectors.toList());  
   this.sortedList = Collections.unmodifiableList(sortedInitializers);  
   logMappings(this.initializers);  
}
```

`ServletContextInitializerBeans#addServletContextInitializerBeans()`方法会从容器中获取ServletContextInitializer实现类，
```java
private void addServletContextInitializerBeans(ListableBeanFactory beanFactory) {  
   for (Class<? extends ServletContextInitializer> initializerType : this.initializerTypes) {  
      // 从容器中获取ServletContextInitializer实现类
      for (Entry<String, ? extends ServletContextInitializer> initializerBean : getOrderedBeansOfType(beanFactory,  
            initializerType)) {  
         addServletContextInitializerBean(initializerBean.getKey(), initializerBean.getValue(), beanFactory);  
      }  
   }  
}
```

`ServletContextInitializerBeans#addServletContextInitializerBean()`会注册ServletContextInitializer实现类：
```java
private void addServletContextInitializerBean(String beanName, ServletContextInitializer initializer,  
      ListableBeanFactory beanFactory) {  
   if (initializer instanceof ServletRegistrationBean) {  
      Servlet source = ((ServletRegistrationBean<?>) initializer).getServlet();  
      addServletContextInitializerBean(Servlet.class, beanName, initializer, beanFactory, source);  
   }  
   else if (initializer instanceof FilterRegistrationBean) {  
      Filter source = ((FilterRegistrationBean<?>) initializer).getFilter();  
      addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);  
   }  
   else if (initializer instanceof DelegatingFilterProxyRegistrationBean) {  
      String source = ((DelegatingFilterProxyRegistrationBean) initializer).getTargetBeanName();  
      addServletContextInitializerBean(Filter.class, beanName, initializer, beanFactory, source);  
   }  
   else if (initializer instanceof ServletListenerRegistrationBean) {  
      EventListener source = ((ServletListenerRegistrationBean<?>) initializer).getListener();  
      addServletContextInitializerBean(EventListener.class, beanName, initializer, beanFactory, source);  
   }  
   else {  
      addServletContextInitializerBean(ServletContextInitializer.class, beanName, initializer, beanFactory,  
            initializer);  
   }  
}
```

三种内嵌容器的初始化流程都很相似。对于内嵌Tomcat，它的初始化流程如下：
1. `TomcatServletWebServerFactory#getWebServer()`和`TomcatWebServer#TomcatWebServer()`方法创建服务器。
2. 在创建服务器过程中，将`selfInitialize`添加到`TomcatStarter`中。
3. `TomcatWebServer#initialize()`方法初始化服务器。
4. `Tomcat#start()`和`LifecycleBase#start()`启动服务器。
5. `StandardContext#startInternal()`启动上下文。
6. `javax.servlet.ServletContainerInitializer#onStartup()`初始化容器。

对于内嵌Jetty，它的初始化流程如下：
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
> `@ServletComponentScan`会将指定路径下标注@WebServlet、@WebFilter或@WebListener的bean注册成三大组件。

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

`ServletWebServerFactoryCustomizer`会被Spring Boot自动配置机制注入，并添加上述WebListenerRegistrar实现类。同时，还会注册`ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar`：
```java
// 注册ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar
@Import({ ServletWebServerFactoryAutoConfiguration.BeanPostProcessorsRegistrar.class})
public class ServletWebServerFactoryAutoConfiguration {  
   // 注册ServletWebServerFactoryCustomizer
   @Bean  
   public ServletWebServerFactoryCustomizer servletWebServerFactoryCustomizer(ServerProperties serverProperties,  
         ObjectProvider<WebListenerRegistrar> webListenerRegistrars,  
         ObjectProvider<CookieSameSiteSupplier> cookieSameSiteSuppliers) {  
      return new ServletWebServerFactoryCustomizer(serverProperties,  
            webListenerRegistrars.orderedStream().collect(Collectors.toList()),  
            cookieSameSiteSuppliers.orderedStream().collect(Collectors.toList()));  
   }
}
```

`WebServerFactoryCustomizerBeanPostProcessor#postProcessBeforeInitialization()`方法会触发`ServletWebServerFactoryCustomizer#customize()`的执行：
```java
private void postProcessBeforeInitialization(WebServerFactory webServerFactory) {  
   LambdaSafe.callbacks(WebServerFactoryCustomizer.class, getCustomizers(), webServerFactory)  
         .withLogger(WebServerFactoryCustomizerBeanPostProcessor.class)  
         .invoke((customizer) -> customizer.customize(webServerFactory));  
}
```

## 2.3 ServletContextInitializerBeans#addAdaptableBeans
> `ServletContextInitializerBeans#addAdaptableBeans()`方法会注册实现Servlet、Filter或Listener接口的bean作为三大组件。

之前说过，会在服务器初始化调用`ServletContextInitializer#onStartup()`方法时，触发`ServletWebServerApplicationContext#selfInitialize()`方法：
```java
private void selfInitialize(ServletContext servletContext) throws ServletException {  
   // 遍历调用org.springframework.boot.web.servlet.ServletContextInitializer#onStartup()
   for (ServletContextInitializer beans : getServletContextInitializerBeans()) {  
      beans.onStartup(servletContext);  
   }  
}
```

`getServletContextInitializerBeans()`方法会创建`ServletContextInitializerBeans`对象。

`ServletContextInitializerBeans#ServletContextInitializerBeans()`方法中定义了获取ServletContextInitializer的逻辑：
```java
public ServletContextInitializerBeans(ListableBeanFactory beanFactory,  
      Class<? extends ServletContextInitializer>... initializerTypes) {  
   this.initializers = new LinkedMultiValueMap<>();  
   this.initializerTypes = (initializerTypes.length != 0) ? Arrays.asList(initializerTypes)  
         : Collections.singletonList(ServletContextInitializer.class);  
   addServletContextInitializerBeans(beanFactory);  
   // 扫描实现Servlet、Filter或EventListener的bean
   addAdaptableBeans(beanFactory);  
   List<ServletContextInitializer> sortedInitializers = this.initializers.values().stream()  
         .flatMap((value) -> value.stream().sorted(AnnotationAwareOrderComparator.INSTANCE))  
         .collect(Collectors.toList());  
   this.sortedList = Collections.unmodifiableList(sortedInitializers);  
   logMappings(this.initializers);  
}
```

`ServletContextInitializerBeans#addAdaptableBeans()`方法会从容器获取所有实现`javax.servlet.Servlet`、`javax.servlet.Filter`或`java.util.EventListener`的bean，为它们创建对应的`RegistrationBean`实现类：
```java
protected void addAdaptableBeans(ListableBeanFactory beanFactory) {  
   MultipartConfigElement multipartConfig = getMultipartConfig(beanFactory);  
   // 扫描&注册Servlet实现类
   addAsRegistrationBean(beanFactory, Servlet.class, new ServletRegistrationBeanAdapter(multipartConfig));  
   // 扫描&注册Filter实现类
   addAsRegistrationBean(beanFactory, Filter.class, new FilterRegistrationBeanAdapter());  
   // 扫描&注册EventListener实现类
   for (Class<?> listenerType : ServletListenerRegistrationBean.getSupportedTypes()) {  
      addAsRegistrationBean(beanFactory, EventListener.class, (Class<EventListener>) listenerType,  
            new ServletListenerRegistrationBeanAdapter());  
   }  
}
```

`ServletContextInitializerBeans#addAsRegistrationBean()`定义了具体扫描&注册逻辑：
```java
private <T, B extends T> void addAsRegistrationBean(ListableBeanFactory beanFactory, Class<T> type,  
      Class<B> beanType, RegistrationBeanAdapter<T> adapter) {  
   // 从容器中获取指定类型的bean
   List<Map.Entry<String, B>> entries = getOrderedBeansOfType(beanFactory, beanType, this.seen);  
   for (Entry<String, B> entry : entries) {  
      String beanName = entry.getKey();  
      B bean = entry.getValue();  
      // 添加缓存
      if (this.seen.add(bean)) {  
         // 创建对应RegistrationBean实现类
         RegistrationBean registration = adapter.createRegistrationBean(beanName, bean, entries.size());  
         // 根据@Order注解设置执行顺序
         int order = getOrder(bean);  
         registration.setOrder(order);  
         this.initializers.add(type, registration);
      }  
   }  
}
```

创建RegistrationBean实现类的逻辑位于对应的`RegistrationBeanAdapter`实现类中。

`ServletContextInitializerBeans.ServletRegistrationBeanAdapter#createRegistrationBean()`方法创建ServletRegistrationBean：
```java
public RegistrationBean createRegistrationBean(String name, Servlet source, int totalNumberOfSourceBeans) {  
   String url = (totalNumberOfSourceBeans != 1) ? "/" + name + "/" : "/";  
   if (name.equals(DISPATCHER_SERVLET_NAME)) {  
      url = "/"; // always map the main dispatcherServlet to "/"  
   }  
   ServletRegistrationBean<Servlet> bean = new ServletRegistrationBean<>(source, url);  
   bean.setName(name);  
   bean.setMultipartConfig(this.multipartConfig);  
   return bean;  
}
```

`ServletContextInitializerBeans.FilterRegistrationBeanAdapter#createRegistrationBean()`方法创建FilterRegistrationBean：
```java
public RegistrationBean createRegistrationBean(String name, Filter source, int totalNumberOfSourceBeans) {  
   FilterRegistrationBean<Filter> bean = new FilterRegistrationBean<>(source);  
   bean.setName(name);  
   return bean;  
}
```

`ServletContextInitializerBeans.ServletListenerRegistrationBeanAdapter#createRegistrationBean()`方法创建ServletListenerRegistrationBean：
```java
public RegistrationBean createRegistrationBean(String name, EventListener source,  
      int totalNumberOfSourceBeans) {  
   return new ServletListenerRegistrationBean<>(source);  
}
```