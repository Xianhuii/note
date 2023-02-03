# 1 基本使用

# 2 工作原理
@Configuration的工作原理十分简单，它基于`ApplicationContext`的`BeanFactoryPostProcessor`机制，具体是在`AbstractApplicationContext#refresh()`方法`invokeBeanFactoryPostProcessors()`阶段，使用`ConfigurationClassPostProcessor`遍历容器中所有标注@Configuration的BeanDefinition缓存进行处理，包括：
1. 处理注解：`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、`@Bean`。
2. 使用`CGLIB`动态代理方式增强配置类功能。
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
   // 处理@PropertySource、@ComponentScan、@Import、@ImportResource和@Bean等注解
   processConfigBeanDefinitions(registry);  
}
```

### 2.1.3 执行postProcessBeanFactory方法
`ConfigurationClassPostProcessor#postProcessBeanFactory()`会使用`CGLIB`动态代理方式增强配置类功能，注册`ImportAwareBeanPostProcessor`。

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
      // 处理@PropertySource、@ComponentScan、@Import、@ImportResource和@Bean等注解
      processConfigBeanDefinitions((BeanDefinitionRegistry) beanFactory);  
   }  
   // 使用CGLIB动态代理方式增强配置类功能
   enhanceConfigurationClasses(beanFactory);  
   // 注册ImportAwareBeanPostProcessor
   beanFactory.addBeanPostProcessor(new ImportAwareBeanPostProcessor(beanFactory));  
}
```

## 2.2 核心方法
### 2.2.1 注解处理
`ConfigurationClassPostProcessor#processConfigBeanDefinitions()`方法的工作流程：
1. 遍历容器的`beanDefinitionMap`，获取所有标注`@Configuration`注解的`BeanDefinition`。
2. 按照`@Order`进行排序。
3. 解析配置类，处理`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、和`@Bean`等注解。

`ConfigurationClassPostProcessor#processConfigBeanDefinitions()`：
```java
public void processConfigBeanDefinitions(BeanDefinitionRegistry registry) {  
   // 1、遍历容器的beanDefinitionMap，获取所有标注@Configuration注解的BeanDefinition
   List<BeanDefinitionHolder> configCandidates = new ArrayList<>();  
   String[] candidateNames = registry.getBeanDefinitionNames();  
   for (String beanName : candidateNames) {  
      BeanDefinition beanDef = registry.getBeanDefinition(beanName);  
      if (beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE) != null) {  
         if (logger.isDebugEnabled()) {  
            logger.debug("Bean definition has already been processed as a configuration class: " + beanDef);  
         }  
      }  
      // 校验@Configuration配置类，设置CONFIGURATION_CLASS_ATTRIBUTE（来自@Configuration注解的proxyBeanMethods属性）和ORDER_ATTRIBUTE（来自@Order注解）属性
      else if (ConfigurationClassUtils.checkConfigurationClassCandidate(beanDef, this.metadataReaderFactory)) {  
         configCandidates.add(new BeanDefinitionHolder(beanDef, beanName));  
      }  
   }  
   // Return immediately if no @Configuration classes were found  
   if (configCandidates.isEmpty()) {  
      return;  
   }  
  
   // 2、按照@Order从小到大排序
   configCandidates.sort((bd1, bd2) -> {  
      int i1 = ConfigurationClassUtils.getOrder(bd1.getBeanDefinition());  
      int i2 = ConfigurationClassUtils.getOrder(bd2.getBeanDefinition());  
      return Integer.compare(i1, i2);  
   });  
  
   // 设置beanName生成策略
   SingletonBeanRegistry sbr = null;  
   if (registry instanceof SingletonBeanRegistry) {  
      sbr = (SingletonBeanRegistry) registry;  
      if (!this.localBeanNameGeneratorSet) {  
         BeanNameGenerator generator = (BeanNameGenerator) sbr.getSingleton(  
               AnnotationConfigUtils.CONFIGURATION_BEAN_NAME_GENERATOR);  
         if (generator != null) {  
            this.componentScanBeanNameGenerator = generator;  
            this.importBeanNameGenerator = generator;  
         }  
      }  
   }  
   if (this.environment == null) {  
      this.environment = new StandardEnvironment();  
   }  
  
   // 3、解析配置类，处理@PropertySource、@ComponentScan、@Import、@ImportResource、和@Bean等注解
   ConfigurationClassParser parser = new ConfigurationClassParser(  
         this.metadataReaderFactory, this.problemReporter, this.environment,  
         this.resourceLoader, this.componentScanBeanNameGenerator, registry);  
   Set<BeanDefinitionHolder> candidates = new LinkedHashSet<>(configCandidates);  
   Set<ConfigurationClass> alreadyParsed = new HashSet<>(configCandidates.size());  
   do {  
      StartupStep processConfig = this.applicationStartup.start("spring.context.config-classes.parse");  
      // 实际解析
      parser.parse(candidates);  
      parser.validate();  
  
      Set<ConfigurationClass> configClasses = new LinkedHashSet<>(parser.getConfigurationClasses());  
      configClasses.removeAll(alreadyParsed);  
  
      // Read the model and create bean definitions based on its content  
      if (this.reader == null) {  
         this.reader = new ConfigurationClassBeanDefinitionReader(  
               registry, this.sourceExtractor, this.resourceLoader, this.environment,  
               this.importBeanNameGenerator, parser.getImportRegistry());  
      }  
      // 如果扫描出新的配置类，进行注册
      this.reader.loadBeanDefinitions(configClasses);  
      alreadyParsed.addAll(configClasses);  
      processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();  
  
      candidates.clear();  
      // 遍历获取新&未处理的配置类
      if (registry.getBeanDefinitionCount() > candidateNames.length) {  
         String[] newCandidateNames = registry.getBeanDefinitionNames();  
         Set<String> oldCandidateNames = new HashSet<>(Arrays.asList(candidateNames));  
         Set<String> alreadyParsedClasses = new HashSet<>();  
         for (ConfigurationClass configurationClass : alreadyParsed) {  
            alreadyParsedClasses.add(configurationClass.getMetadata().getClassName());  
         }  
         for (String candidateName : newCandidateNames) {  
            if (!oldCandidateNames.contains(candidateName)) {  
               BeanDefinition bd = registry.getBeanDefinition(candidateName);  
               if (ConfigurationClassUtils.checkConfigurationClassCandidate(bd, this.metadataReaderFactory) &&  
                     !alreadyParsedClasses.contains(bd.getBeanClassName())) {  
                  candidates.add(new BeanDefinitionHolder(bd, candidateName));  
               }  
            }  
         }  
         candidateNames = newCandidateNames;  
      }  
   }  
   // 由于可能通过扫描/引入新的配置类，需要循环对新的配置类进行解析，直到没有新的配置类
   while (!candidates.isEmpty());  
  
   // Register the ImportRegistry as a bean in order to support ImportAware @Configuration classes  
   if (sbr != null && !sbr.containsSingleton(IMPORT_REGISTRY_BEAN_NAME)) {  
      sbr.registerSingleton(IMPORT_REGISTRY_BEAN_NAME, parser.getImportRegistry());  
   }  
  
   if (this.metadataReaderFactory instanceof CachingMetadataReaderFactory) {  
      // Clear cache in externally provided MetadataReaderFactory; this is a no-op  
      // for a shared cache since it'll be cleared by the ApplicationContext.      
      ((CachingMetadataReaderFactory) this.metadataReaderFactory).clearCache();  
   }  
}
```



### 2.2.2 增强配置类

### 2.2.3 注册ImportAwareBeanPostProcessor
