# 1 介绍
`Environment`接口表示当前应用的运行时环境，包括`profiles`环境和`properties`环境。

`profiles`是对`BeanDefinition`逻辑上的分组，每个`bean`都可以通过`@Provile`注解指定它所属的`profile`。在Spring容器注册`BeanDefinition`时，只会注册当前运行时环境激活的`profiles`的`bean`。

`properties`环境包括`propertis`文件、JVM变量、系统环境变量、JNDI和servlet上下文参数等。

`PropertyResolver`提供了对`properties`环境的操作，`Environment`接口提供了对`profiles`环境的操作。
![[Environment.png]]

---

在`ApplicationContext`实现类作为容器时，可以实现`EnvironmentAware`接口获取运行时环境：
```java
@Component  
public class ComponentA implements EnvironmentAware {  
    private Environment environment;  
      
    @Override  
    public void setEnvironment(Environment environment) {  
        this.environment = environment;  
    }  
}
```

这是因为`AplicationContext`实现类在`postProcessBeforeInitialization()`阶段，会通过`ApplicationContextAwareProcessor#invokeAwareInterfaces()`方法执行`XxxAware#setXxx()`方法：
```java
private void invokeAwareInterfaces(Object bean) {  
   if (bean instanceof EnvironmentAware) {  
      ((EnvironmentAware) bean).setEnvironment(this.applicationContext.getEnvironment());  
   }  
   // ……省略
}
```

也可以通过`@Autowired`或`@Resource`直接从`AplicationContext`中获取`environment`：
```java
@Component  
public class ComponentA implements EnvironmentAware {  
    @Autowired
    private Environment environment;  
}
```

通常不建议直接在应用层使用`environment`，推荐使用`${...}`直接引入对应的值：
```java
@Component  
public class ComponentA {  
    @Value("${java.runtime.name}")  
    private String runtimeName;   
}
```

`${...}`需要`PropertySourcesPlaceholderConfigurer`的支持，在Spring Boot中通常会帮我们默认引入：
```java
@Configuration  
public class AppConfig {  
    @Bean  
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {  
        return new PropertySourcesPlaceholderConfigurer();  
    }  
}
```

---
![[Environment 2.png]]
`ConfigurableEnvironment`提供了管理`profiles`和`properties`环境的基本方法。

在`AbstractEnvironment`中实现了对`profiles`和`properties`环境的基本操作，并通过`customizePropertySources()`方法使子类可以添加额外自定义配置文件。

`StandardEnvironment`、`StandardServletEnvironment`和`StandardReactiveWebEnvironment`是Spring为`standard`应用、`Servlet Web`应用和`Reactive Web`应用提供的运行时环境，我们可以根据项目实际情况进行选择。

`ApplicationEnvironment`、`ApplicationServletEnvironment`和`ApplicationReactiveWebEnvironment`则是Spring Boot内置的实现类，专门用于`SpringApplication`。

# 2 AbstractEnvironment
![[AbstractEnvironment.png]]
`AbstractEnvironment`的`propetySources`保存了配置文件的信息，通过`propertyResolver`可以从配置文件中获取指定属性值。`activeProfiles`和`defaultProfiles`则起着缓存对应配置的作用。

`AbstractEnvironment`的核心在于它的构造函数，子类在初始化时都会调用这些构造函数进行默认处理。

无参构造函数：
```java
public AbstractEnvironment() {  
   this(new MutablePropertySources());  
}
```

有参构造函数：
```java
protected AbstractEnvironment(MutablePropertySources propertySources) {  
   this.propertySources = propertySources;  
   this.propertyResolver = createPropertyResolver(propertySources);  
   customizePropertySources(propertySources);  
}
```

子类可以通过`AbstractEnvironment#customizePropertySources()`方法用来添加配置文件：
```java
protected void customizePropertySources(MutablePropertySources propertySources) {  
}
```

在获取配置时，会使用`propertyResolver`读取配置文件，例如：
```java
public String getProperty(String key) {  
   return this.propertyResolver.getProperty(key);  
}
```

在获取`profiles`信息时，则会进行缓存处理，例如：
```java
protected Set<String> doGetActiveProfiles() {  
   synchronized (this.activeProfiles) {  
      if (this.activeProfiles.isEmpty()) {  
         String profiles = doGetActiveProfilesProperty();  
         if (StringUtils.hasText(profiles)) {  
            setActiveProfiles(StringUtils.commaDelimitedListToStringArray(  
                  StringUtils.trimAllWhitespace(profiles)));  
         }  
      }  
      return this.activeProfiles;  
   }  
}
```

`AbstractEnvironment#getSystemProperties()`方法可以获取系统配置信息：
```java
public Map<String, Object> getSystemProperties() {  
   try {  
      return (Map) System.getProperties();  
   }  
   catch (AccessControlException ex) {  
      return (Map) new ReadOnlySystemAttributesMap() {  
         @Override  
         @Nullable         
         protected String getSystemAttribute(String attributeName) {  
            try {  
               return System.getProperty(attributeName);  
            }  
            catch (AccessControlException ex) {    
               return null;  
            }  
         }  
      };  
   }  
}
```

`AbstractEnvironment#getSystemEnvironment()`方法可以获取系统环境信息：
```java
public Map<String, Object> getSystemEnvironment() {  
   if (suppressGetenvAccess()) {  
      return Collections.emptyMap();  
   }  
   try {  
      return (Map) System.getenv();  
   }  
   catch (AccessControlException ex) {  
      return (Map) new ReadOnlySystemAttributesMap() {  
         @Override  
         @Nullable         
         protected String getSystemAttribute(String attributeName) {  
            try {  
               return System.getenv(attributeName);  
            }  
            catch (AccessControlException ex) {  
               return null;  
            }  
         }  
      };  
   }  
}
```
# 3 StandardEnvironment
`StandardEnvironment`通过`customizePropertySources()`方法添加了`systemEnvironment`和`systemProperties`环境变量：
```java
protected void customizePropertySources(MutablePropertySources propertySources) {  
   propertySources.addLast(  
         new PropertiesPropertySource(SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME, getSystemProperties()));  
   propertySources.addLast(  
         new SystemEnvironmentPropertySource(SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME, getSystemEnvironment()));  
}
```

## 3.1 StandardServletEnvironment
`StandardServletEnvironment`通过`customizePropertySources()`方法添加了`servletConfigInitParams`、`servletContextInitParams`和`jndiProperties`配置文件：
```java
protected void customizePropertySources(MutablePropertySources propertySources) {  
   propertySources.addLast(new StubPropertySource(SERVLET_CONFIG_PROPERTY_SOURCE_NAME));  
   propertySources.addLast(new StubPropertySource(SERVLET_CONTEXT_PROPERTY_SOURCE_NAME));  
   if (jndiPresent && JndiLocatorDelegate.isDefaultJndiEnvironmentAvailable()) {  
      propertySources.addLast(new JndiPropertySource(JNDI_PROPERTY_SOURCE_NAME));  
   }  
   super.customizePropertySources(propertySources);  
}
```
## 3.2 StandardReactiveWebEnvironment
`StandardReactiveWebEnvironment`使用着`StandardEnvironment`原本的功能：
```java
public class StandardReactiveWebEnvironment extends StandardEnvironment implements ConfigurableReactiveWebEnvironment {  
  
   public StandardReactiveWebEnvironment() {  
      super();  
   }  
  
   protected StandardReactiveWebEnvironment(MutablePropertySources propertySources) {  
      super(propertySources);  
   }  
  
}
```

## 3.3 ApplicationXxxEnvironment
`ApplicationEnvironment`、`ApplicationServletEnvironment`和`ApplicationReactiveEnvironment`除了继承对应`StandardXxxEnvironment`的功能外，主要的变化是重写了`createPropertyResolver()`方法，使用`ConfigurationPropertySourcesPropertyResolver`对象作为`propertyResolver`：
```java
protected ConfigurablePropertyResolver createPropertyResolver(MutablePropertySources propertySources) {  
   return ConfigurationPropertySources.createPropertyResolver(propertySources);  
}
```

# 4 典型案例
## 4.1 ConditionEvaluator
在`AnnotatedBeanDefinitionReader`或`ClassPathBeanDefinitionScanner`中初始化`conditionEvaluator`时，都会创建`environment`对象：
```java
private static Environment getOrCreateEnvironment(BeanDefinitionRegistry registry) {  
   Assert.notNull(registry, "BeanDefinitionRegistry must not be null");  
   if (registry instanceof EnvironmentCapable) {  
      return ((EnvironmentCapable) registry).getEnvironment();  
   }  
   return new StandardEnvironment();  
}
```

## 4.2 ApplicationContext
`AbstractApplicationContext`提供了`createEnvironment()`方法，不同子类可能有不同的实现。

`AbstractApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardEnvironment();  
}
```

`AbstractRefreshableWebApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardServletEnvironment();  
}
```

`AnnotationConfigReactiveWebApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardReactiveWebEnvironment();  
}
```

`GenericReactiveWebApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardReactiveWebEnvironment();  
}
```

`GenericWebApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardServletEnvironment();  
}
```

`StaticWebApplicationContext#createEnvironment()`：
```java
protected ConfigurableEnvironment createEnvironment() {  
   return new StandardServletEnvironment();  
}
```

## 4.3 SpringApplication
`SpringApplication`是Spring Boot的启动类，它会使用`SpringApplication#prepareEnvironment()`方法创建`environment`：
```java
private ConfigurableEnvironment prepareEnvironment(SpringApplicationRunListeners listeners,  
      DefaultBootstrapContext bootstrapContext, ApplicationArguments applicationArguments) {  
   // 根据webApplicationType创建不同类型的environment
   ConfigurableEnvironment environment = getOrCreateEnvironment();  
   // 设置conversionService，根据args修改propertySources和profiles
   configureEnvironment(environment, applicationArguments.getSourceArgs());  
   // 添加configurationProperties配置
   ConfigurationPropertySources.attach(environment);  
   // 执行监听器：添加额外配置文件
   listeners.environmentPrepared(bootstrapContext, environment);  
   // 将defaultProperties配置移到末尾
   DefaultPropertiesPropertySource.moveToEnd(environment);  
   Assert.state(!environment.containsProperty("spring.main.environment-prefix"),  
         "Environment prefix cannot be set via properties.");  
   // 将environment绑定到SpringApplication中
   bindToSpringApplication(environment);  
   if (!this.isCustomEnvironment) {  
      environment = convertEnvironment(environment);  
   }  
   // 将configurationProperties配置移到首位
   ConfigurationPropertySources.attach(environment);  
   return environment;  
}
```

`SpringApplication#getOrCreateEnvironment()`会根据`webApplicationType`创建不同的`Environment`对象：
```java
private ConfigurableEnvironment getOrCreateEnvironment() {  
   if (this.environment != null) {  
      return this.environment;  
   }  
   switch (this.webApplicationType) {  
      case SERVLET:  
         return new ApplicationServletEnvironment();  
      case REACTIVE:  
         return new ApplicationReactiveWebEnvironment();  
      default:  
         return new ApplicationEnvironment();  
   }  
}
```

`listeners.environmentPrepared()`方法会调用到`EnvironmentPostProcessorApplicationListener#onApplicationEvent()`方法，进行不同阶段的处理：
```java
public void onApplicationEvent(ApplicationEvent event) {  
   // 准备阶段
   if (event instanceof ApplicationEnvironmentPreparedEvent) {  
      onApplicationEnvironmentPreparedEvent((ApplicationEnvironmentPreparedEvent) event);  
   }  
   // 准备完成
   if (event instanceof ApplicationPreparedEvent) {  
      onApplicationPreparedEvent();  
   }  
   // 失败
   if (event instanceof ApplicationFailedEvent) {  
      onApplicationFailedEvent();  
   }  
}
```

在准备阶段，会调用不同的`EnvironmentPostProcessor`的`postProcessEnvironment()`方法进行处理：
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

其中，`ConfigDataEnvironmentPostProcessor`会获取`application.properties`等的信息。