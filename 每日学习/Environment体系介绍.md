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
![[Environment 1.png]]
`Environment`所具备的对`profiles`和`properties`环境的基本操作，在`AbstractEnvironment`中都已经实现了，通过`customizePropertySources()`方法使子类可以添加额外自定义配置文件。

`StandardEnvironment`、`StandardServletEnvironment`和`StandardReactiveWebEnvironment`是Spring为`standard`应用、`Servlet Web`应用和`Reactive Web`应用提供的运行时环境，我们可以根据项目实际情况进行选择。

`ApplicationEnvironment`、`ApplicationServletEnvironment`和`ApplicationReactiveWebEnvironment`则是Spring Boot内置的实现类，专门用于`SpringApplication`。

# 2 AbstractEnvironment

# 3 StandardEnvironment

# 4 ApplicationEnvironment

# 5 StandardServletEnvironment

# 6 StandardReactiveWebEnvironment

