Spring Boot项目都需要一个启动类。

在启动类上标注`@SpringBootApplication`，在main方法中调用`SpringApplication.run()`方法，就可以启动项目：
```java
@SpringBootApplication  
public class Application {  
   public static void main(String[] args) {  
      SpringApplication.run(Application.class, args);  
   }  
}
```

在项目启动过程中，需要经过两个流程：
1. 创建`SpringApplication`对象
2. 执行`SpringApplication#run()`方法

简单来说，会执行以下代码：
```java
new SpringApplication(primarySources).run(args);
```

# 1 创建SpringApplication
在创建SpringApplication对象时，会进行以下初始化：
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

# 2 启动SpringApplication
调用`SpringApplication#run()`方法启动项目，会创建并且刷新`applicationContext`：
```java
public ConfigurableApplicationContext run(String... args) {  
   long startTime = System.nanoTime();  
   // 创建bootstrapContext，执行BootstrapRegistryInitializer实现类初始化方法
   DefaultBootstrapContext bootstrapContext = createBootstrapContext();  
   ConfigurableApplicationContext context = null;  
   configureHeadlessProperty();  
   // 利用SPI机制，从spring.factory中获取SpringApplicationRunListener实现类，用于监听Spring#run()方法的运行状态
   SpringApplicationRunListeners listeners = getRunListeners(args);  
   // starting监听：触发SpringApplicationRunListener#starting()回调
   listeners.starting(bootstrapContext, this.mainApplicationClass);  
   try {  
      // 封装args参数
      ApplicationArguments applicationArguments = new DefaultApplicationArguments(args);  
      // 创建&配置environment
      ConfigurableEnvironment environment = prepareEnvironment(listeners, bootstrapContext, applicationArguments);  
      configureIgnoreBeanInfo(environment);  
      // 打印Banner
      Banner printedBanner = printBanner(environment);  
      // 创建applicationContext：根据webApplicationType创建
      context = createApplicationContext();  
      context.setApplicationStartup(this.applicationStartup);  
      // 预处理applicationContext：设置environment、触发ApplicationContextInitializer、注册BeanFactoryPostProcessor等
      prepareContext(bootstrapContext, context, environment, listeners, applicationArguments, printedBanner);  
      // 执行applicationContext#refresh()方法，启动容器
      refreshContext(context);  
      // afterRefresh回调：默认空方法
      afterRefresh(context, applicationArguments);  
      Duration timeTakenToStartup = Duration.ofNanos(System.nanoTime() - startTime);  
      if (this.logStartupInfo) {  
         new StartupInfoLogger(this.mainApplicationClass).logStarted(getApplicationLog(), timeTakenToStartup);  
      }  
      // started监听：触发SpringApplicationRunListener#started()回调
      listeners.started(context, timeTakenToStartup);  
      // ApplicationRunner/CommandLineRunner回调
      callRunners(context, applicationArguments);  
   }  
   catch (Throwable ex) {  
      // 启动失败的回调
      handleRunFailure(context, ex, listeners);  
      throw new IllegalStateException(ex);  
   }  
   try {  
      Duration timeTakenToReady = Duration.ofNanos(System.nanoTime() - startTime);  
      // ready监听：触发SpringApplicationRunListener#ready()回调
      listeners.ready(context, timeTakenToReady);  
   }  
   catch (Throwable ex) {  
      // 启动失败的回调
      handleRunFailure(context, ex, null);  
      throw new IllegalStateException(ex);  
   }  
   return context;  
}
```

## 2.1 prepareEnvironment

## 2.2 printBanner

## 2.3 createApplicationContext

## 2.4 prepareContext

## 2.5 refreshContext

## 2.6 callRunners
