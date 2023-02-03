# 1 基本使用

# 2 工作原理
@Configuration的工作原理十分简单，它基于`ApplicationContext`的`BeanFactoryPostProcessor`机制，具体是在`AbstractApplicationContext#refresh()`方法`invokeBeanFactoryPostProcessors()`阶段，使用`ConfigurationClassPostProcessor`遍历容器中所有标注@Configuration的BeanDefinition缓存进行处理，包括：
1. 处理注解：`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、`@Bean`。
2. 使用`CGLIB`代理方式增强配置类功能。
3. 注册`ImportAwareBeanPostProcessor`。

需要注意的是，`BeanFactory`并不提供BeanFactoryPostProcessor功能，如果使用底层的DefaultListableBeanFactory作为容器，不能对@Configuration进行处理。

## 2.1 工作流
`@Configuration`工作流的各个节点包括：
1. 注册`ConfigurationClassPostProcessor`
2. 执行`ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry`方法
3. 执行`ConfigurationClassPostProcessor#postProcessBeanFactory`方法

### 2.1.1 注册ConfigurationClassPostProcessor
`ApplicationContext`实现类可以使用`AnnotatedBeanDefinitionReader`或`ClassPathBeanDefinitionScanner`读取配置类。

`AnnotatedBeanDefinitionReader`在在初始化时，就会注册`ConfigurationClassPostProcessor`：
```java
public AnnotatedBeanDefinitionReader(BeanDefinitionRegistry registry, Environment environment) {  
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

### 2.1.2 执行postProcessBeanDefinitionRegistry方法
`ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry()`中会对`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`和`@Bean`等注解进行处理。

`ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry()`：
```java
public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) {  
   int registryId = System.identityHashCode(registry);  
   if (this.registriesPostProcessed.contains(registryId)) {  
      throw new IllegalStateException(  
            "postProcessBeanDefinitionRegistry already called on this post-processor against " + registry);  
   }  
   if (this.factoriesPostProcessed.contains(registryId)) {  
      throw new IllegalStateException(  
            "postProcessBeanFactory already called on this post-processor against " + registry);  
   }  
   this.registriesPostProcessed.add(registryId);  
  
   processConfigBeanDefinitions(registry);  
}
```

### 2.1.3 执行postProcessBeanFactory方法
`ConfigurationClassPostProcessor#postProcessBeanFactory()`会使用`CGLIB`代理方式增强配置类功能，注册`ImportAwareBeanPostProcessor`。

如果容器不支持BeanDefinitionRegistryPostProcessor回调，那么会在这个阶段完成对`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`和`@Bean`等注解进行处理。

`ConfigurationClassPostProcessor#postProcessBeanFactory()`：
```java
public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {  
   int factoryId = System.identityHashCode(beanFactory);  
   if (this.factoriesPostProcessed.contains(factoryId)) {  
      throw new IllegalStateException(  
            "postProcessBeanFactory already called on this post-processor against " + beanFactory);  
   }  
   this.factoriesPostProcessed.add(factoryId);  
   if (!this.registriesPostProcessed.contains(factoryId)) {  
      // BeanDefinitionRegistryPostProcessor hook apparently not supported...  
      // Simply call processConfigurationClasses lazily at this point then.      
      processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);  
   }  
  
   enhanceConfigurationClasses(beanFactory);  
   beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));  
}
```

## 2.2 核心方法
