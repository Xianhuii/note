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

默认`BootstrapRegistryInitializer`为空。

默认`ApplicationContextInitializer`实现类包括：
- org.springframework.boot.context.ConfigurationWarningsApplicationContextInitializer
- org.springframework.boot.context.ContextIdApplicationContextInitializer
- org.springframework.boot.context.config.DelegatingApplicationContextInitializer
- org.springframework.boot.rsocket.context.RSocketPortInfoApplicationContextInitializer
- org.springframework.boot.web.context.ServerPortInfoApplicationContextInitializer

默认`ApplicationListener`实现类包括：
- org.springframework.boot.ClearCachesApplicationListener
- org.springframework.boot.builder.ParentContextCloserApplicationListener
- org.springframework.boot.context.FileEncodingApplicationListener
- org.springframework.boot.context.config.AnsiOutputApplicationListener
- org.springframework.boot.context.config.DelegatingApplicationListener
- org.springframework.boot.context.logging.LoggingApplicationListener
- org.springframework.boot.env.EnvironmentPostProcessorApplicationListener

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
`SpringApplication#prepareEnvironment()`方法会创建`environment`，并且添加各种类型的配置源：
```java
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,  
      DefaultBootstrapContext bootstrapContext, ApplicationArguments applicationArguments) {  
   // 根据webApplicationType创建：ApplicationServletEnvironment、ApplicationReactiveWebEnvironment或ApplicationEnvironment
   ConfigurableEnvironment environment = getOrCreateEnvironment();  
   // 从命令行中添加propertySource
   configureEnvironment(environment, applicationArguments.getSourceArgs());  
   // 添加configurationProperties
   ConfigurationPropertySources.attach(environment);  
   // 发布ApplicationEnvironmentPreparedEvent事件，由ApplicationListener监听并处理
   listeners.environmentPrepared(bootstrapContext, environment);  
   DefaultPropertiesPropertySource.moveToEnd(environment);  
   Assert.state(!environment.containsProperty("spring.main.environment-prefix"),  
         "Environment prefix cannot be set via properties.");  
   // 将environment绑定到springApplication
   bindToSpringApplication(environment);  
   if (!this.isCustomEnvironment) {  
      EnvironmentConverter environmentConverter = new EnvironmentConverter(getClassLoader());  
      environment = environmentConverter.convertEnvironmentIfNecessary(environment, deduceEnvironmentClass());  
   }  
   ConfigurationPropertySources.attach(environment);  
   return environment;  
}
```

在发布`ApplicationEnvironmentPreparedEvent`事件时，使用了观察者模式。`SimpleApplicationEventMulticaster#multicastEvent()`：
```java
public void multicastEvent(final ApplicationEvent event, @Nullable ResolvableType eventType) {  
   ResolvableType type = (eventType != null ? eventType : resolveDefaultEventType(event));  
   Executor executor = getTaskExecutor();  
   for (ApplicationListener<?> listener : getApplicationListeners(event, type)) {  
      if (executor != null) {  
         executor.execute(() -> invokeListener(listener, event));  
      }  
      else {  
         invokeListener(listener, event);  
      }  
   }  
}
```

以下ApplicationListener实现类会监听ApplicationEnvironmentPreparedEvent事件，并调用`ApplicationListener#onApplicationEvent()`方法进行处理：
- org.springframework.boot.ClearCachesApplicationListener
- org.springframework.boot.context.FileEncodingApplicationListener
- org.springframework.boot.context.config.AnsiOutputApplicationListener
- org.springframework.boot.context.config.DelegatingApplicationListener
- org.springframework.boot.context.logging.LoggingApplicationListener
- org.springframework.boot.env.EnvironmentPostProcessorApplicationListener
- org.springframework.boot.autoconfigure.BackgroundPreinitializer

其中与`environment`有关的是`EnvironmentPostProcessorApplicationListener#onApplicationEnvironmentPreparedEvent()`：
```java
private void onApplicationEnvironmentPreparedEvent(ApplicationEnvironmentPreparedEvent event) {  
   ConfigurableEnvironment environment = event.getEnvironment();  
   SpringApplication application = event.getSpringApplication();  
   for (EnvironmentPostProcessor postProcessor : getEnvironmentPostProcessors(application.getResourceLoader(),  
         event.getBootstrapContext())) {  
      postProcessor.postProcessEnvironment(environment, application);  
   }  
}
```

默认`EnvironmentPostProcessor`实现类包括：
- org.springframework.boot.env.RandomValuePropertySourceEnvironmentPostProcessor：添加`random`配置源。
- org.springframework.boot.env.SystemEnvironmentPropertySourceEnvironmentPostProcessor：用OriginAwareSystemEnvironmentPropertySource替换SystemEnvironmentPropertySource。
- org.springframework.boot.env.SpringApplicationJsonEnvironmentPostProcessor：解析`spring.application.json`配置。
- org.springframework.boot.cloud.CloudFoundryVcapEnvironmentPostProcessor：解析`VCAP`元数据。
- org.springframework.boot.context.config.ConfigDataEnvironmentPostProcessor：添加`application.property`、`application.yml`等配置源。
- org.springframework.boot.reactor.DebugAgentEnvironmentPostProcessor：开启`reactor.tools.agent.ReactorDebugAgent`功能。
- org.springframework.boot.autoconfigure.integration.IntegrationPropertiesEnvironmentPostProcessor：添加`META-INF/spring.integration.properties`配置源。

其中，我们首先需要了解的是`ConfigDataEnvironmentPostProcessor#postProcessEnvironment()`，它会加载常用的`application.properties`等配置源：
```java
void postProcessEnvironment(ConfigurableEnvironment environment, ResourceLoader resourceLoader,  
      Collection<String> additionalProfiles) {  
   try {  
      this.logger.trace("Post-processing environment to add config data");  
      resourceLoader = (resourceLoader != null) ? resourceLoader : new DefaultResourceLoader();  
      // 创建ConfigDataEnvironment对象，加载application.properties等配置源
      getConfigDataEnvironment(environment, resourceLoader, additionalProfiles).processAndApply();  
   }  
   catch (UseLegacyConfigProcessingException ex) {  
      this.logger.debug(LogMessage.format("Switching to legacy config file processing [%s]",  
            ex.getConfigurationProperty()));  
      configureAdditionalProfiles(environment, additionalProfiles);  
      postProcessUsingLegacyApplicationListener(environment, resourceLoader);  
   }  
}
```

实际加载逻辑位于`ConfigDataEnvironment#processAndApply()`：
```java
void processAndApply() {  
   // 创建importer
   ConfigDataImporter importer = new ConfigDataImporter(this.logFactory, this.notFoundAction, this.resolvers,  
         this.loaders);  
   registerBootstrapBinder(this.contributors, null, DENY_INACTIVE_BINDING);  
   // 初步导入
   ConfigDataEnvironmentContributors contributors = processInitial(this.contributors, importer);  
   // 创建activationContext，用于校验配置是否符合激活条件
   ConfigDataActivationContext activationContext = createActivationContext(  
         contributors.getBinder(null, BinderOption.FAIL_ON_BIND_TO_INACTIVE_SOURCE));  
   // 加载配置源
   contributors = processWithoutProfiles(contributors, importer, activationContext);  
   // 根据profile过滤配置源
   activationContext = withProfiles(contributors, activationContext);  
   contributors = processWithProfiles(contributors, importer, activationContext);  
   // 添加配置源到environment
   applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(),  
         importer.getOptionalLocations());  
}
```



## 2.2 printBanner

## 2.3 createApplicationContext

## 2.4 prepareContext

## 2.5 refreshContext

## 2.6 callRunners
