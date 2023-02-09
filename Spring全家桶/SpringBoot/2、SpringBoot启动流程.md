
```java
SpringApplication.run(Application.class, args);
```

1. 初始化`SpringApplication`：
```java
new SpringApplication(primarySources).run(args);
```

```java
public SpringApplication(ResourceLoader resourceLoader, Class<?>... primarySources) {  
   this.resourceLoader = resourceLoader;  
   // 设置主配置源
   this.primarySources = new LinkedHashSet<>(Arrays.asList(primarySources));  
   // 根据依赖判断应用类型：Serlvet、Reactive或None
   this.webApplicationType = WebApplicationType.deduceFromClasspath();  
   // 利用SPI机制，从spring.factory中获取BootstrapRegistryInitializer实现类
   this.bootstrapRegistryInitializers = new ArrayList<>(  
         getSpringFactoriesInstances(BootstrapRegistryInitializer.class));  
   // 利用SPI机制，从spring.factory中获取ApplicationContextInitializer实现类
   setInitializers((Collection) getSpringFactoriesInstances(ApplicationContextInitializer.class));  
   // 利用SPI机制，从spring.factory中获取ApplicationListener实现类
   setListeners((Collection) getSpringFactoriesInstances(ApplicationListener.class));  
   // 设置主类
   this.mainApplicationClass = deduceMainApplicationClass();  
}
```

2. 启动`SpringApplication`：
```java
public ConfigurableApplicationContext run(String... args) {  
   long startTime = System.nanoTime();  
   DefaultBootstrapContext bootstrapContext = createBootstrapContext();  
   ConfigurableApplicationContext context = null;  
   configureHeadlessProperty();  
   SpringApplicationRunListeners listeners = getRunListeners(args);  
   listeners.starting(bootstrapContext, this.mainApplicationClass);  
   try {  
      ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);  
      ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);  
      configureIgnoreBeanInfo(environment);  
      Banner printedBanner = printBanner(environment);  
      context = createApplicationContext();  
      context.setApplicationStartup(this.applicationStartup);  
      prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);  
      refreshContext(context);  
      afterRefresh(context, applicationArguments);  
      Duration timeTakenToStartup = Duration.ofNanos(System.nanoTime() - startTime);  
      if (this.logStartupInfo) {  
         new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), timeTakenToStartup);  
      }  
      listeners.started(context, timeTakenToStartup);  
      callRunners(context, applicationArguments);  
   }  
   catch (Throwable ex) {  
      handleRunFailure(context, ex, listeners);  
      throw new IllegalStateException(ex);  
   }  
   try {  
      Duration timeTakenToReady = Duration.ofNanos(System.nanoTime() - startTime);  
      listeners.ready(context, timeTakenToReady);  
   }  
   catch (Throwable ex) {  
      handleRunFailure(context, ex, null);  
      throw new IllegalStateException(ex);  
   }  
   return context;  
}
```