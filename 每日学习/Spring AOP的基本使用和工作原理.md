# 1 基本使用
## 1.1 依赖
Spring的AOP功能是在IoC的基础上建立起来的，因此需要引入最基础的依赖如下：
```xml
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-context</artifactId>  
</dependency>  
<dependency>  
    <groupId>org.springframework</groupId>  
    <artifactId>spring-aspects</artifactId>  
</dependency>
```

## 1.2 开启AOP功能
默认情况下，Spring并不会开启AOP功能，需要我们在配置类中使用`@EnableAspectJAutoProxy`手动开启：
```java
@EnableAspectJAutoProxy  
@Configuration  
public class AopConfig {  
}
```

## 1.3 定义切面逻辑
在正常业务逻辑中，我们首先会有一个服务类，其中定义着业务逻辑：
```java
@Component  
public class ComponentA {  
    public void test() {  
        System.out.println("test");  
    }  
}
```

然后，我们会定义切面类，在其中定义拦截的规则，以及`Before`、`After`或`Around`等各种拦截方法：
```java
@Aspect  
@Component  
public class AspectA {  
    @Pointcut("execution(* *(..))")  
    public void pointcut() {}  
  
    @Before("pointcut()")  
    public void before() {  
         System.out.println("before");  
     }  
}
```

在上面的例子中，在调用`ComponentA#test()`方法之前，总是会先执行`AspectA#before()`方法。

## 1.4 启动
需要注意的是，最基础的`BeanFactory`（例如`DefaultListableBeanFactory`）并没有集成AOP功能。为了使用Spring AOP功能，我们必须使用`ApplicationContext`实现类。

例如：
```java
ApplicationContext context = new AnnotationConfigApplicationContext(AopConfig.class);  
ComponentA componentA = context.getBean(ComponentA.class);  
componentA.test();
```

会打印如下结果：
```
before
test
```

在Spring Boot项目中，默认会使用`ApplicationContext`实现类作为容器，所以也可以使用AOP功能。

# 2 工作原理
## 2.1 注册ConfigurationClassPostProcessor
`ApplicationContext`实现类读取配置类时，可以使用`AnnotatedBeanDefinitionReader`或`ClassPathBeanDefinitionScanner`。

`AnnotatedBeanDefinitionReader`在在初始化时，就会注册`ConfigurationClassPostProcessor`：
```java
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {  
   Assert.notNull(registry, "BeanDefinitionRegistry must not be null");  
   Assert.notNull(environment, "Environment must not be null");  
   this.registry = registry;  
   this.conditionEvaluator = new ConditionEvaluator(registry, environment, null);  
   AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);  
}
```

`ClassPathBeanDefinitionScanner`在扫描路径时，默认也会注册`ConfigurationClassPostProcessor`：
```java
public int scan(String... basePackages) {  
   int beanCountAtScanStart = this.registry.getBeanDefinitionCount();  
  
   doScan(basePackages);  
  
   // Register annotation config processors, if necessary.  
   if (this.includeAnnotationConfig) {  
      AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);  
   }  
  
   return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);  
}
```

注册的实际逻辑位于`AnnotationConfigUtils#registerAnnotationConfigProcessors()`：`
```java
public static Set<BeanDefinitionHolder> registerAnnotationConfigProcessors(  
      BeanDefinitionRegistry registry, @Nullable Object source) {  
   // ……省略
   Set<BeanDefinitionHolder> beanDefs = new LinkedHashSet<>(8);  
  
   if (!registry.containsBeanDefinition(CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME)) {  
      RootBeanDefinition def = new RootBeanDefinition(ConfigurationClassPostProcessor.class);  
      def.setSource(source);  
      beanDefs.add(registerPostProcessor(registry, def, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME));  
   }    
   // ……省略
   return beanDefs;  
}
```

## 2.2 执行ConfigurationClassPostProcessor
`ConfigurationClassPostProcessor`是`BeanDefinitionRegistryPostProcessor`实现类。

因此，在`AbstractApplicationContext#refresh()`的`invokeBeanFactoryPostProcessors`阶段，会执行它的`postProcessBeanDefinitionRegistry()`方法。该方法会遍历所有已经注册的`beanDefinition`，对其中标注`@Configuration`的使用`ConfigurationClassParser`进行处理。

其中与AOP功能有关的是，它会解析`@Import`注解，注册相关配置类。

对于`@EnableAspectJAutoProxy`注解，它会引入`AspectJAutoProxyRegistrar`配置类：
```java
@Target(ElementType.TYPE)  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
@Import(AspectJAutoProxyRegistrar.class)  
public @interface EnableAspectJAutoProxy {  
   boolean proxyTargetClass() default false;  
   boolean exposeProxy() default false;  
}
```

`AspectJAutoProxyRegistrar`是`ImportBeanDefinitionRegistrar`实现类，`ConfigurationClassParser`会调用它的`registerBeanDefinitions()`方法。该方法会注册`AnnotationAwareAspectJAutoProxyCreator`，并且根据`@EnableAspectJAutoProxy`的属性设置代理类型：
```java
public void registerBeanDefinitions(  
      AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {  
   // 注册AnnotationAwareAspectJAutoProxyCreator
   AopConfigUtils.registerAspectJAnnotationAutoProxyCreatorIfNecessary(registry);  
  
   // 根据@EnableAspectJAutoProxy的属性设置配置
   AnnotationAttributes enableAspectJAutoProxy =  
         AnnotationConfigUtils.attributesFor(importingClassMetadata, EnableAspectJAutoProxy.class);  
   if (enableAspectJAutoProxy != null) {  
      if (enableAspectJAutoProxy.getBoolean("proxyTargetClass")) {  
         AopConfigUtils.forceAutoProxyCreatorToUseClassProxying(registry);  
      }  
      if (enableAspectJAutoProxy.getBoolean("exposeProxy")) {  
         AopConfigUtils.forceAutoProxyCreatorToExposeProxy(registry);  
      }  
   }  
}
```

`AnnotationAwareAspectJAutoProxyCreator`是Spring AOP功能的核心类，它是`BeanPostProcessor`实现类，一方面会缓存切面信息，另一方面会将匹配的`bean`封装成对应代理对象。



ConfigurationClassPostProcessor：解析`@Configuration`注解
ConfigurationClassParser：解析`@Import`注解，对于Spring AOP来说，它会引入`AspectJAutoProxyRegistrar`
AspectJAutoProxyRegistrar：注册`AnnotationAwareAspectJAutoProxyCreator`
AnnotationAwareAspectJAutoProxyCreator：是一个`BeanPostProcessor`，会根据`@Aspect`缓存切面信息，并且对特定`bean`创建代理对象进行封装
BeanFactoryAspectJAdvisorsBuilder：会根据`@Aspect`创建并缓存切面信息
InstantiationModelAwarePointcutAdvisorImpl：默认的切面信息缓存
ProxyFactory：根据切面信息创建代理对象进行封装`bean`
