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
   // 根据profile加载配置源
   activationContext = withProfiles(contributors, activationContext);  
   contributors = processWithProfiles(contributors, importer, activationContext);  
   // 添加配置源到environment
   applyToEnvironment(contributors, activationContext, importer.getLoadedLocations(),  
         importer.getOptionalLocations());  
}
```

导入逻辑大概如下：
1. 遍历`congributors`，从中获取配置文件路径：
	1. `optional:file:./;optional:file:./config/;optional:file:./config/*/`
	2. `optional:classpath:/;optional:classpath:/config/`
2. 遍历上述配置文件路径，使用`ConfigDataLocationResolver`实现类导入其中的配置文件。

在创建`ConfigDataEnvironment`时，会使用SPI机制从`spring.factories`中加载`ConfigDataLocationResolver`实现类。`ConfigDataLocationResolvers#ConfigDataLocationResolvers()`：
```java
ConfigDataLocationResolvers(DeferredLogFactory logFactory, ConfigurableBootstrapContext bootstrapContext,  
      Binder binder, ResourceLoader resourceLoader) {  
   this(logFactory, bootstrapContext, binder, resourceLoader, SpringFactoriesLoader  
         .loadFactoryNames(ConfigDataLocationResolver.class, resourceLoader.getClassLoader()));  
}
```

默认添加的`ConfigDataLocationResolver`实现类有：
- org.springframework.boot.context.config.ConfigTreeConfigDataLocationResolver：加载`configtree:`前缀的配置文件。
- org.springframework.boot.context.config.StandardConfigDataLocationResolver：加载标准路径下的配置文件。

`StandardConfigDataLocationResolver#resolve()`是加载标准路径下配置文件的入口：
```java
public List<StandardConfigDataResource> resolve(ConfigDataLocationResolverContext context,  
      ConfigDataLocation location) throws ConfigDataNotFoundException {  
   return resolve(getReferences(context, location.split()));  
}
```

`StandardConfigDataLocationResolver#getReferences()`会对配置文件路径进行拼接。例如，对于`file:./`，它会为其添加`application`文件名，并根据`propertySourceLoaders`成员变量添加`properties`/`xml`/`yml`/`yaml`等文件后缀。如果有指定`profile`，在`processWithProfiles()`阶段还会生成再生成一份file:./application-`profile`.yml形式的路径进行添加。

`StandardConfigDataLocationResolver`的`propertySourceLoaders`成员变量也是通过SPI机制，从`spring.factories`中加载`PropertySourceLoader`实现类。默认加载：
- org.springframework.boot.env.PropertiesPropertySourceLoader：加载`properties`和`xml`配置文件。
- org.springframework.boot.env.YamlPropertySourceLoader：加载`yml`和`yaml`配置文件。

随后，`StandardConfigDataLocationResolver#resolve()`会根据解析出的具体配置文件路径，使用`resourceLoader`加载。`DefaultResourceLoader#getResource()`：
```java
public Resource getResource(String location) {  
   Assert.notNull(location, "Location must not be null");  
  
   for (ProtocolResolver protocolResolver : getProtocolResolvers()) {  
      Resource resource = protocolResolver.resolve(location, this);  
      if (resource != null) {  
         return resource;  
      }  
   }  
  
   if (location.startsWith("/")) {  
      return getResourceByPath(location);  
   }  
   else if (location.startsWith(CLASSPATH_URL_PREFIX)) {  
      return new ClassPathResource(location.substring(CLASSPATH_URL_PREFIX.length()), getClassLoader());  
   }  
   else {  
      try {  
         // Try to parse the location as a URL...  
         URL url = new URL(location);  
         return (ResourceUtils.isFileURL(url) ? new FileUrlResource(url) : new UrlResource(url));  
      }  
      catch (MalformedURLException ex) {  
         // No URL -> resolve as resource path.  
         return getResourceByPath(location);  
      }  
   }  
}
```

## 2.2 printBanner
`SpringApplication#printBanner()`方法可以在启动时打印Logo：
```java
private Banner printBanner(ConfigurableEnvironment environment) {  
   // 默认为Banner.Mode.CONSOLE
   if (this.bannerMode == Banner.Mode.OFF) {  
      return null;  
   }  
   ResourceLoader resourceLoader = (this.resourceLoader != null) ? this.resourceLoader  
         : new DefaultResourceLoader(null);  
   SpringApplicationBannerPrinter bannerPrinter = new SpringApplicationBannerPrinter(resourceLoader, this.banner);  
   if (this.bannerMode == Mode.LOG) {  
      return bannerPrinter.print(environment, this.mainApplicationClass, logger);  
   }  
   return bannerPrinter.print(environment, this.mainApplicationClass, System.out);  
}
```

核心代码位于`SpringApplicationBannerPrinter#print()`：
```java
Banner print(Environment environment, Class<?> sourceClass, PrintStream out) {  
   // 根据environment创建banner
   Banner banner = getBanner(environment);  
   // 
   banner.printBanner(environment, sourceClass, out);  
   return new PrintedBanner(banner, sourceClass);  
}
```

`SpringApplicationBannerPrinter#getBanner()`可以根据配置创建对应的banner，只要我们指定相关配置文件，或者添加指定路径的文件，就可以自动进行打印：
```java
private Banner getBanner(Environment environment) {  
   Banners banners = new Banners();  
   // 添加spring.banner.image.location配置/banner.gif/banner.jpg/banner.png的banner
   banners.addIfNotNull(getImageBanner(environment));  
   // 添加spring.banner.location配置/banner.txt的banner
   banners.addIfNotNull(getTextBanner(environment));  
   if (banners.hasAtLeastOneBanner()) {  
      return banners;  
   }  
   if (this.fallbackBanner != null) {  
      return this.fallbackBanner;  
   }  
   // 默认SpringBoot官方的banner
   return DEFAULT_BANNER;  
}
```

如果使用默认SpringBoot官方的banner，会调用`SpringBootBanner#printBanner()`方法进行打印。

如果添加图片或文本的banner，会调用`Banners#printBanner()`方法，会遍历添加的banner，调用各自实现去打印：
```java
public void printBanner(Environment environment, Class<?> sourceClass, PrintStream out) {  
   for (Banner banner : this.banners) {  
      banner.printBanner(environment, sourceClass, out);  
   }  
}
```

## 2.3 createApplicationContext
`SpringApplication#createApplicationContext()`会创建applicationContext：
```java
protected ConfigurableApplicationContext createApplicationContext() {  
   return this.applicationContextFactory.create(this.webApplicationType);  
}
```

webApplicationType在前面的步骤中会根据是否导入相关依赖进行判断：Serlvet、Reactive或None。

默认的applicationContextFactory是`DefaultApplicationContextFactory`。因此，会调用`DefaultApplicationContextFactory#create()`方法：
```java
public ConfigurableApplicationContext create(WebApplicationType webApplicationType) {  
   try {  
      return getFromSpringFactories(webApplicationType, ApplicationContextFactory::create,  
            AnnotationConfigApplicationContext::new);  
   }  
   catch (Exception ex) {  
      throw new IllegalStateException("Unable create a default ApplicationContext instance, "  
            + "you may need a custom ApplicationContextFactory", ex);  
   }  
}
```

接着会调用`DefaultApplicationContextFactory#getFromSpringFactories()`：
```java
private <T> T getFromSpringFactories(WebApplicationType webApplicationType,  
      BiFunction<ApplicationContextFactory, WebApplicationType, T> action, Supplier<T> defaultResult) {  
   // 遍历spring.factory中的ApplicationContextFactory实现类
   for (ApplicationContextFactory candidate : SpringFactoriesLoader.loadFactories(ApplicationContextFactory.class,  
         getClass().getClassLoader())) {  
      // 调用实现类的ApplicationContextFactory::create方法
      T result = action.apply(candidate, webApplicationType);  
      if (result != null) {  
         return result;  
      }  
   }  
   // 实现类不顶用，调用AnnotationConfigApplicationContext::new
   return (defaultResult != null) ? defaultResult.get() : null;  
}
```

默认会从spring.factory中加载的ApplicationContextFactory实现类包括：
- org.springframework.boot.web.reactive.context.AnnotationConfigReactiveWebServerApplicationContext.Factory：创建AnnotationConfigReactiveWebServerApplicationContext。
- org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext.Factory：创建AnnotationConfigServletWebServerApplicationContext。

简单来说，会根据`webApplicationType`创建不同的容器：
- NONE：AnnotationConfigApplicationContext
- SERVLET：AnnotationConfigServletWebServerApplicationContext
- REACTIVE：AnnotationConfigReactiveWebServerApplicationContext

## 2.4 prepareContext
`SpringApplication#prepareContext()`方法会对容器进行预处理：
```java
private void prepareContext(DefaultBootstrapContext bootstrapContext, ConfigurableApplicationContext context,  
      ConfigurableEnvironment environment, SpringApplicationRunListeners listeners,  
      ApplicationArguments applicationArguments, Banner printedBanner) {  
   // 关联environment
   context.setEnvironment(environment);  
   // applicationContext的相关后处理：beanNameGenerator、resourceLoader、addConversionService
   postProcessApplicationContext(context);  
   // 触发ApplicationContextInitializer#initialize()回调（来自spring.factories）
   applyInitializers(context);  
   // 发布ApplicationContextInitializedEvent事件，ApplicationListener监听
   listeners.contextPrepared(context);  
   // 发布BootstrapContextClosedEvent事件，ApplicationListener监听
   bootstrapContext.close(context);  
   // 打印启动日志
   if (this.logStartupInfo) {  
      logStartupInfo(context.getParent() == null);  
      logStartupProfileInfo(context);  
   }  
   // 添加SpringBoot指定的单例bean
   ConfigurableListableBeanFactory beanFactory = context.getBeanFactory();  
   beanFactory.registerSingleton("springApplicationArguments", applicationArguments);  
   if (printedBanner != null) {  
      beanFactory.registerSingleton("springBootBanner", printedBanner);  
   }  
   // 设置容器参数
   if (beanFactory instanceof AbstractAutowireCapableBeanFactory) {  
      ((AbstractAutowireCapableBeanFactory) beanFactory).setAllowCircularReferences(this.allowCircularReferences);  
      if (beanFactory instanceof DefaultListableBeanFactory) {  
         ((DefaultListableBeanFactory) beanFactory)  
               .setAllowBeanDefinitionOverriding(this.allowBeanDefinitionOverriding);  
      }  
   }  
   if (this.lazyInitialization) {  
      context.addBeanFactoryPostProcessor(new LazyInitializationBeanFactoryPostProcessor());  
   }  
   context.addBeanFactoryPostProcessor(new PropertySourceOrderingBeanFactoryPostProcessor(context));  
   // 添加resources作为BeanDefinition到容器：primarySources（启动类）、sources
   Set<Object> sources = getAllSources();  
   Assert.notEmpty(sources, "Sources must not be empty");  
   load(context, sources.toArray(new Object[0]));  
   // 发布ApplicationPreparedEvent事件，ApplicationListener监听
   listeners.contextLoaded(context);  
}
```

## 2.5 refreshContext
`SpringApplication#refreshContext()`方法会调用刷新applicationContest：
```java
private void refreshContext(ConfigurableApplicationContext context) {  
   // 注册shutdownHook
   if (this.registerShutdownHook) {  
      shutdownHook.registerApplicationContext(context);  
   }  
   // 刷新applicationContest
   refresh(context);  
}
```

它实际上会调用容器自身的刷新方法，对容器进行初始化操作，具体流程可以参看相关文章（[ApplicationContext体系](https://www.cnblogs.com/Xianhuii/p/17060707.html#13-refresh)）。`SpringApplication#refresh()`：
```java
protected void refresh(ConfigurableApplicationContext applicationContext) {  
   applicationContext.refresh();  
}
```

## 2.6 afterRefresh
`SpringApplication#afterRefresh()`方法可以对刷新后的applicationContext进行一个回调操作，默认是个空方法，可以由子类具体去实现：
```java
protected void afterRefresh(ConfigurableApplicationContext context, ApplicationArguments args) {  
}
```

## 2.7 callRunners
`SpringApplication#callRunners()`方法会执行容器中`ApplicationRunner`和`CommandLineRunner`的回调，只要我们添加这两个bean，就会在这个阶段进行执行回调：
```java
private void callRunners(ApplicationContext context, ApplicationArguments args) {  
   List<Object> runners = new ArrayList<>();  
   runners.addAll(context.getBeansOfType(ApplicationRunner.class).values());  
   runners.addAll(context.getBeansOfType(CommandLineRunner.class).values());  
   AnnotationAwareOrderComparator.sort(runners);  
   for (Object runner : new LinkedHashSet<>(runners)) {  
      if (runner instanceof ApplicationRunner) {  
         callRunner((ApplicationRunner) runner, args);  
      }  
      if (runner instanceof CommandLineRunner) {  
         callRunner((CommandLineRunner) runner, args);  
      }  
   }  
}
```