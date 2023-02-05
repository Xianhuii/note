# 1 基本使用
`@Configuration`用来标注配置类，它自身的元注解是`@Component`，所以能够被Spring容器管理。

它可以和其他注解联合使用，完成配置功能：
- `@Bean`：标注方法，可以注册bean。
- `@ComponentScan`和`@ComponentScans`：通过`ClassPathBeanDefinitionScan`扫描指定路径，进行注册bean。
- `@Import`：通过引入其他配置类进行联合注册bean。
- `@ImportResource`：引入`.groovy`或`.xml`配置文件，进行注册bean。
- `@PropertySource`和`@PropertySources`：加载`.properties`文件，添加到`environmen`。

一个完整的`@Configuration`配置类使用方式如下：
```java
@Configuration  
@Order(1)  
@ComponentScan  
@ComponentScans(value = {@ComponentScan})  
@Import(AppConfig2.class)  
@ImportResource  
@PropertySource("")  
@PropertySources(@PropertySource(""))
public class AppConfig {
	@Bean
	public MyBean myBean() {
		return new MyBean();
	}
}
```

通过`ApplicationContext`实现类，通过注册/扫描方式将该配置类添加到容器`beanDefinitionMap`缓存中，`@Configuration`注解就可以起作用：
```java
ApplicationContext context = new AnnotationConfigApplicationContext(AppConfig.class);  
Object myBean = context.getBean("myBean");  
System.out.println(myBean);
```

# 2 工作原理
@Configuration的工作原理十分简单，它基于`ApplicationContext`的`BeanFactoryPostProcessor`机制，具体是在`AbstractApplicationContext#refresh()`方法`invokeBeanFactoryPostProcessors()`阶段，使用`ConfigurationClassPostProcessor`遍历容器中所有标注@Configuration的BeanDefinition缓存进行处理，包括：
1. 处理注解：`@PropertySource`、`@ComponentScan`、`@ComponentScans`、`@Import`、`@ImportResource`、`@Bean`。
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
      // 如果扫描出新的配置类，进行注册，同时注册@Bean方法对应的bean，以及Import导入的bean
      this.reader.loadBeanDefinitions(configClasses);  
      alreadyParsed.addAll(configClasses);  
      processConfig.tag("classCount", () -> String.valueOf(configClasses.size())).end();  
  
      candidates.clear();  
      // 遍历获取新的&未处理的配置类
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

通过`ConfigurationClassParser#parse()`处理所有标注`@Configuration`的`BeanDefinition`：
```java
public void parse(Set<BeanDefinitionHolder> configCandidates) {  
   for (BeanDefinitionHolder holder : configCandidates) {  
      BeanDefinition bd = holder.getBeanDefinition();  
      try {  
         if (bd instanceof AnnotatedBeanDefinition) {  
            parse(((AnnotatedBeanDefinition) bd).getMetadata(), holder.getBeanName());  
         }  
         else if (bd instanceof AbstractBeanDefinition && ((AbstractBeanDefinition) bd).hasBeanClass()) {  
            parse(((AbstractBeanDefinition) bd).getBeanClass(), holder.getBeanName());  
         }  
         else {  
            parse(bd.getBeanClassName(), holder.getBeanName());  
         }  
      }  
      catch (BeanDefinitionStoreException ex) {  
         throw ex;  
      }  
      catch (Throwable ex) {  
         throw new BeanDefinitionStoreException(  
               "Failed to parse configuration class [" + bd.getBeanClassName() + "]", ex);  
      }  
   }  
  
   this.deferredImportSelectorHandler.process();  
}
```

底层会调用`ConfigurationClassParser#processConfigurationClass()`进行处理每个配置类：
```java
protected void processConfigurationClass(ConfigurationClass configClass, Predicate<String> filter) throws IOException {  
   // 根据@Conditional注解判断当前配置类是否需要注册
   if (this.conditionEvaluator.shouldSkip(configClass.getMetadata(), ConfigurationPhase.PARSE_CONFIGURATION)) {  
      return;  
   }  
  
   ConfigurationClass existingClass = this.configurationClasses.get(configClass);  
   if (existingClass != null) {  
      if (configClass.isImported()) {  
         if (existingClass.isImported()) {  
            existingClass.mergeImportedBy(configClass);  
         }  
         // Otherwise ignore new imported config class; existing non-imported class overrides it.  
         return;  
      }  
      else {  
         // Explicit bean definition found, probably replacing an import.  
         // Let's remove the old one and go with the new one.         
         this.configurationClasses.remove(configClass);  
         this.knownSuperclasses.values().removeIf(configClass::equals);  
      }  
   }  
  
   // Recursively process the configuration class and its superclass hierarchy.  
   SourceClass sourceClass = asSourceClass(configClass, filter);  
   do {  
      // 处理@PropertySource、@ComponentScan、@Import、@ImportResource、和@Bean等注解
      sourceClass = doProcessConfigurationClass(configClass, sourceClass, filter);  
   }  
   while (sourceClass != null);  
  
   this.configurationClasses.put(configClass, configClass);  
}
```

通过`ConfigurationClassParser#doProcessConfigurationClass()`方法处理`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、和`@Bean`等注解：
```java
protected final SourceClass doProcessConfigurationClass(  
      ConfigurationClass configClass, SourceClass sourceClass, Predicate<String> filter)  
      throws IOException {  
  
   if (configClass.getMetadata().isAnnotated(Component.class.getName())) {  
      // Recursively process any member (nested) classes first  
      processMemberClasses(configClass, sourceClass, filter);  
   }  
  
   // Process any @PropertySource annotations  
   for (AnnotationAttributes propertySource : AnnotationConfigUtils.attributesForRepeatable(  
         sourceClass.getMetadata(), PropertySources.class,  
         org.springframework.context.annotation.PropertySource.class)) {  
      if (this.environment instanceof ConfigurableEnvironment) {  
         processPropertySource(propertySource);  
      }  
      else {  
         logger.info("Ignoring @PropertySource annotation on [" + sourceClass.getMetadata().getClassName() +  
               "]. Reason: Environment must implement ConfigurableEnvironment");  
      }  
   }  
  
   // Process any @ComponentScan annotations  
   Set<AnnotationAttributes> componentScans = AnnotationConfigUtils.attributesForRepeatable(  
         sourceClass.getMetadata(), ComponentScans.class, ComponentScan.class);  
   if (!componentScans.isEmpty() &&  
         !this.conditionEvaluator.shouldSkip(sourceClass.getMetadata(), ConfigurationPhase.REGISTER_BEAN)) {  
      for (AnnotationAttributes componentScan : componentScans) {  
         // The config class is annotated with @ComponentScan -> perform the scan immediately  
         Set<BeanDefinitionHolder> scannedBeanDefinitions =  
               this.componentScanParser.parse(componentScan, sourceClass.getMetadata().getClassName());  
         // Check the set of scanned definitions for any further config classes and parse recursively if needed  
         for (BeanDefinitionHolder holder : scannedBeanDefinitions) {  
            BeanDefinition bdCand = holder.getBeanDefinition().getOriginatingBeanDefinition();  
            if (bdCand == null) {  
               bdCand = holder.getBeanDefinition();  
            }  
            if (ConfigurationClassUtils.checkConfigurationClassCandidate(bdCand, this.metadataReaderFactory)) {  
               parse(bdCand.getBeanClassName(), holder.getBeanName());  
            }  
         }  
      }  
   }  
  
   // Process any @Import annotations  
   processImports(configClass, sourceClass, getImports(sourceClass), filter, true);  
  
   // Process any @ImportResource annotations  
   AnnotationAttributes importResource =  
         AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);  
   if (importResource != null) {  
      String[] resources = importResource.getStringArray("locations");  
      Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");  
      for (String resource : resources) {  
         String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);  
         configClass.addImportedResource(resolvedResource, readerClass);  
      }  
   }  
  
   // Process individual @Bean methods  
   Set<MethodMetadata> beanMethods = retrieveBeanMethodMetadata(sourceClass);  
   for (MethodMetadata methodMetadata : beanMethods) {  
      configClass.addBeanMethod(new BeanMethod(methodMetadata, configClass));  
   }
   // Process default methods on interfaces  
   processInterfaces(configClass, sourceClass);  
  
   // 递归对配置类父类进行处理
   if (sourceClass.getMetadata().hasSuperClass()) {  
      String superclass = sourceClass.getMetadata().getSuperClassName();  
      if (superclass != null && !superclass.startsWith("java") &&  
            !this.knownSuperclasses.containsKey(superclass)) {  
         this.knownSuperclasses.put(superclass, configClass);  
         // Superclass found, return its annotation metadata and recurse  
         return sourceClass.getSuperClass();  
      }  
   }  
  
   // No superclass -> processing is complete  
   return null;  
}
```

对于`@PropertySource`注解，会根据注解属性找到对应文件，并添加/替换到容器的`environment`中。`ConfigurationClassParser#processPropertySource()`：
```java
private void processPropertySource(AnnotationAttributes propertySource) throws IOException {  
   // 获取文件地址
   String name = propertySource.getString("name");  
   if (!StringUtils.hasLength(name)) {  
      name = null;  
   }  
   String encoding = propertySource.getString("encoding");  
   if (!StringUtils.hasLength(encoding)) {  
      encoding = null;  
   }  
   String[] locations = propertySource.getStringArray("value");  
   Assert.isTrue(locations.length > 0, "At least one @PropertySource(value) location is required");  
   boolean ignoreResourceNotFound = propertySource.getBoolean("ignoreResourceNotFound");  
  
   Class<? extends PropertySourceFactory> factoryClass = propertySource.getClass("factory");  
   PropertySourceFactory factory = (factoryClass == PropertySourceFactory.class ?  
         DEFAULT_PROPERTY_SOURCE_FACTORY : BeanUtils.instantiateClass(factoryClass));  
   // 添加/替换文件
   for (String location : locations) {  
      try {  
         String resolvedLocation = this.environment.resolveRequiredPlaceholders(location);  
         Resource resource = this.resourceLoader.getResource(resolvedLocation);  
         addPropertySource(factory.createPropertySource(name, new EncodedResource(resource, encoding)));  
      }  
      catch (IllegalArgumentException | FileNotFoundException | UnknownHostException | SocketException ex) {  
         // Placeholders not resolvable or resource not found when trying to open it  
         if (ignoreResourceNotFound) {  
            if (logger.isInfoEnabled()) {  
               logger.info("Properties location [" + location + "] not resolvable: " + ex.getMessage());  
            }  
         }  
         else {  
            throw ex;  
         }  
      }  
   }  
}
```

对于`@ComponentScan`和`@ComponentScan`注解，会获取指定的扫描路径，使用`ClassPathBeanDefinitionScanner`进行扫描。`ComponentScanAnnotationParser#parse()`：
```java
public Set<BeanDefinitionHolder> parse(AnnotationAttributes componentScan, String declaringClass) {  
   // 创建&配置ClassPathBeanDefinitionScanner
   ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(this.registry,  
         componentScan.getBoolean("useDefaultFilters"), this.environment, this.resourceLoader);  
  
   Class<? extends BeanNameGenerator> generatorClass = componentScan.getClass("nameGenerator");  
   boolean useInheritedGenerator = (BeanNameGenerator.class == generatorClass);  
   scanner.setBeanNameGenerator(useInheritedGenerator ? this.beanNameGenerator :  
         BeanUtils.instantiateClass(generatorClass));  
  
   ScopedProxyMode scopedProxyMode = componentScan.getEnum("scopedProxy");  
   if (scopedProxyMode != ScopedProxyMode.DEFAULT) {  
      scanner.setScopedProxyMode(scopedProxyMode);  
   }  
   else {  
      Class<? extends ScopeMetadataResolver> resolverClass = componentScan.getClass("scopeResolver");  
      scanner.setScopeMetadataResolver(BeanUtils.instantiateClass(resolverClass));  
   }  
  
   scanner.setResourcePattern(componentScan.getString("resourcePattern"));  
  
   for (AnnotationAttributes includeFilterAttributes : componentScan.getAnnotationArray("includeFilters")) {  
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(includeFilterAttributes, this.environment,  
            this.resourceLoader, this.registry);  
      for (TypeFilter typeFilter : typeFilters) {  
         scanner.addIncludeFilter(typeFilter);  
      }  
   }  
   for (AnnotationAttributes excludeFilterAttributes : componentScan.getAnnotationArray("excludeFilters")) {  
      List<TypeFilter> typeFilters = TypeFilterUtils.createTypeFiltersFor(excludeFilterAttributes, this.environment,  
         this.resourceLoader, this.registry);  
      for (TypeFilter typeFilter : typeFilters) {  
         scanner.addExcludeFilter(typeFilter);  
      }  
   }  
  
   boolean lazyInit = componentScan.getBoolean("lazyInit");  
   if (lazyInit) {  
      scanner.getBeanDefinitionDefaults().setLazyInit(true);  
   }  
  
   // 获取指定包路径
   Set<String> basePackages = new LinkedHashSet<>();  
   String[] basePackagesArray = componentScan.getStringArray("basePackages");  
   for (String pkg : basePackagesArray) {  
      String[] tokenized = StringUtils.tokenizeToStringArray(this.environment.resolvePlaceholders(pkg),  
            ConfigurableApplicationContext.CONFIG_LOCATION_DELIMITERS);  
      Collections.addAll(basePackages, tokenized);  
   }  
   for (Class<?> clazz : componentScan.getClassArray("basePackageClasses")) {  
      basePackages.add(ClassUtils.getPackageName(clazz));  
   }  
   if (basePackages.isEmpty()) {  
      basePackages.add(ClassUtils.getPackageName(declaringClass));  
   }  
  
   scanner.addExcludeFilter(new AbstractTypeHierarchyTraversingFilter(false, false) {  
      @Override  
      protected boolean matchClassName(String className) {  
         return declaringClass.equals(className);  
      }  
   });  
   // 扫描指定路径
   return scanner.doScan(StringUtils.toStringArray(basePackages));  
}
```

对于`@Import`注解，会通过`ImportSelector`实现类、`ImportBeanDefinitionRegistrar`实现类或`@Configuration`配置类三种方式分别处理。`ConfigurationClassParser#processImports()`：
```java
private void processImports(ConfigurationClass configClass, SourceClass currentSourceClass,  
      Collection<SourceClass> importCandidates, Predicate<String> exclusionFilter,  
      boolean checkForCircularImports) {  
  
   if (importCandidates.isEmpty()) {  
      return;  
   }  
  
   if (checkForCircularImports && isChainedImportOnStack(configClass)) {  
      this.problemReporter.error(new CircularImportProblem(configClass, this.importStack));  
   }  
   else {  
      this.importStack.push(configClass);  
      try {  
         // 遍历@Import注解属性值：根据不同接口策略进行分别注册BeanDefinition
         for (SourceClass candidate : importCandidates) {  
            if (candidate.isAssignable(ImportSelector.class)) {  
               // ImportSelector实现类 -> delegate to it to determine imports  
               Class<?> candidateClass = candidate.loadClass();  
               ImportSelector selector = ParserStrategyUtils.instantiateClass(candidateClass, ImportSelector.class,  
                     this.environment, this.resourceLoader, this.registry);  
               Predicate<String> selectorFilter = selector.getExclusionFilter();  
               if (selectorFilter != null) {  
                  exclusionFilter = exclusionFilter.or(selectorFilter);  
               }  
               if (selector instanceof DeferredImportSelector) {  
                  this.deferredImportSelectorHandler.handle(configClass, (DeferredImportSelector) selector);  
               }  
               else {  
                  String[] importClassNames = selector.selectImports(currentSourceClass.getMetadata());  
                  Collection<SourceClass> importSourceClasses = asSourceClasses(importClassNames, exclusionFilter);  
                  processImports(configClass, currentSourceClass, importSourceClasses, exclusionFilter, false);  
               }  
            }  
            else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {  
               //ImportBeanDefinitionRegistrar实现类 -> delegate to it to register additional bean definitions               
               Class<?> candidateClass = candidate.loadClass();  
               ImportBeanDefinitionRegistrar registrar =  
                     ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,  
                           this.environment, this.resourceLoader, this.registry);  
               configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());  
            }  
            else {  
               // 不是ImportSelector/ImportBeanDefinitionRegistrar实现类 -> 作为@Configuration配置类去处理
               this.importStack.registerImport(  
                     currentSourceClass.getMetadata(), candidate.getMetadata().getClassName());  
               processConfigurationClass(candidate.asConfigClass(configClass), exclusionFilter);  
            }  
         }  
      }  
      catch (BeanDefinitionStoreException ex) {  
         throw ex;  
      }  
      catch (Throwable ex) {  
         throw new BeanDefinitionStoreException(  
               "Failed to process import candidates for configuration class [" +  
               configClass.getMetadata().getClassName() + "]", ex);  
      }  
      finally {  
         this.importStack.pop();  
      }  
   }  
}
```

对于`@ImportResource`注解，会获取对应的配置文件地址（`.groovy`或`.xml`），通过对应的`BeanDefinitionReader`实现类注册：
```java
AnnotationAttributes importResource =  
      AnnotationConfigUtils.attributesFor(sourceClass.getMetadata(), ImportResource.class);  
if (importResource != null) {  
   // 获取配置文件地址
   String[] resources = importResource.getStringArray("locations");  
   // 获取该配置文件对应的BeanDefinitionReader实现类：XmlBeanDefinitionReader或GroovyBeanDefinitionReader
   Class<? extends BeanDefinitionReader> readerClass = importResource.getClass("reader");  
   // 加入配置文件缓存
   for (String resource : resources) {  
      String resolvedResource = this.environment.resolveRequiredPlaceholders(resource);  
      configClass.addImportedResource(resolvedResource, readerClass);  
   }  
}
```

对于`@Bean`注解（包括接口中声明的默认方法），会通过`ASM`按声明顺序获取标注的所有方法，然后添加到配置类的`beanMethods`缓存中。`ConfigurationClassParser#retrieveBeanMethodMetadata()`：
```java
private Set<MethodMetadata> retrieveBeanMethodMetadata(SourceClass sourceClass) {  
   AnnotationMetadata original = sourceClass.getMetadata();  
   Set<MethodMetadata> beanMethods = original.getAnnotatedMethods(Bean.class.getName());  
   if (beanMethods.size() > 1 && original instanceof StandardAnnotationMetadata) {  
      // Try reading the class file via ASM for deterministic declaration order...  
      // Unfortunately, the JVM's standard reflection returns methods in arbitrary      
      // order, even between different runs of the same application on the same JVM.      
      try {  
         AnnotationMetadata asm =  
               this.metadataReaderFactory.getMetadataReader(original.getClassName()).getAnnotationMetadata();  
         Set<MethodMetadata> asmMethods = asm.getAnnotatedMethods(Bean.class.getName());  
         if (asmMethods.size() >= beanMethods.size()) {  
            Set<MethodMetadata> selectedMethods = new LinkedHashSet<>(asmMethods.size());  
            for (MethodMetadata asmMethod : asmMethods) {  
               for (MethodMetadata beanMethod : beanMethods) {  
                  if (beanMethod.getMethodName().equals(asmMethod.getMethodName())) {  
                     selectedMethods.add(beanMethod);  
                     break;  
                  }  
               }  
            }  
            if (selectedMethods.size() == beanMethods.size()) {  
               // All reflection-detected methods found in ASM method set -> proceed  
               beanMethods = selectedMethods;  
            }  
         }  
      }  
      catch (IOException ex) {  
         logger.debug("Failed to read class file via ASM for determining @Bean method order", ex);  
         // No worries, let's continue with the reflection metadata we started with...  
      }  
   }  
   return beanMethods;  
}
```

### 2.2.2 增强配置类
`ConfigurationClassPostProcessor#enhanceConfigurationClasses()`方法会对所有配置类进行增强

`ConfigurationClassPostProcessor#enhanceConfigurationClasses()`：
```java
public void enhanceConfigurationClasses(ConfigurableListableBeanFactory beanFactory) {  
   StartupStep enhanceConfigClasses = this.applicationStartup.start("spring.context.config-classes.enhance");  
   Map<String, AbstractBeanDefinition> configBeanDefs = new LinkedHashMap<>();  
   // 遍历所有BeanDefinition
   for (String beanName : beanFactory.getBeanDefinitionNames()) {  
      BeanDefinition beanDef = beanFactory.getBeanDefinition(beanName);  
      Object configClassAttr = beanDef.getAttribute(ConfigurationClassUtils.CONFIGURATION_CLASS_ATTRIBUTE);  
      AnnotationMetadata annotationMetadata = null;  
      MethodMetadata methodMetadata = null;  
      if (beanDef instanceof AnnotatedBeanDefinition) {  
         AnnotatedBeanDefinition annotatedBeanDefinition = (AnnotatedBeanDefinition) beanDef;  
         annotationMetadata = annotatedBeanDefinition.getMetadata();  
         methodMetadata = annotatedBeanDefinition.getFactoryMethodMetadata();  
      }  
      if ((configClassAttr != null || methodMetadata != null) && beanDef instanceof AbstractBeanDefinition) {  
         // Configuration class (full or lite) or a configuration-derived @Bean method  
         // -> eagerly resolve bean class at this point, unless it's a 'lite' configuration         
         // or component class without @Bean methods.         
         AbstractBeanDefinition abd = (AbstractBeanDefinition) beanDef;  
         if (!abd.hasBeanClass()) {  
            boolean liteConfigurationCandidateWithoutBeanMethods =  
                  (ConfigurationClassUtils.CONFIGURATION_CLASS_LITE.equals(configClassAttr) &&  
                     annotationMetadata != null && !ConfigurationClassUtils.hasBeanMethods(annotationMetadata));  
            if (!liteConfigurationCandidateWithoutBeanMethods) {  
               try {  
                  abd.resolveBeanClass(this.beanClassLoader);  
               }  
               catch (Throwable ex) {  
                  throw new IllegalStateException(  
                        "Cannot load configuration class: " + beanDef.getBeanClassName(), ex);  
               }  
            }  
         }  
      }  
      if (ConfigurationClassUtils.CONFIGURATION_CLASS_FULL.equals(configClassAttr)) {  
         if (!(beanDef instanceof AbstractBeanDefinition)) {  
            throw new BeanDefinitionStoreException("Cannot enhance @Configuration bean definition '" +  
                  beanName + "' since it is not stored in an AbstractBeanDefinition subclass");  
         }  
         else if (logger.isInfoEnabled() && beanFactory.containsSingleton(beanName)) {  
            logger.info("Cannot enhance @Configuration bean definition '" + beanName +  
                  "' since its singleton instance has been created too early. The typical cause " +  
                  "is a non-static @Bean method with a BeanDefinitionRegistryPostProcessor " +  
                  "return type: Consider declaring such methods as 'static'.");  
         }  
         configBeanDefs.put(beanName, (AbstractBeanDefinition) beanDef);  
      }  
   }  
   if (configBeanDefs.isEmpty() || NativeDetector.inNativeImage()) {  
      // nothing to enhance -> return immediately  
      enhanceConfigClasses.end();  
      return;  
   }  
  
   // 使用CGLIB动态代理增强配置类
   ConfigurationClassEnhancer enhancer = new ConfigurationClassEnhancer();  
   for (Map.Entry<String, AbstractBeanDefinition> entry : configBeanDefs.entrySet()) {  
      AbstractBeanDefinition beanDef = entry.getValue();  
      // If a @Configuration class gets proxied, always proxy the target class  
      beanDef.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);  
      // Set enhanced subclass of the user-specified bean class  
      Class<?> configClass = beanDef.getBeanClass();  
      // 创建代理配置类
      Class<?> enhancedClass = enhancer.enhance(configClass, this.beanClassLoader);  
      if (configClass != enhancedClass) {  
         if (logger.isTraceEnabled()) {  
            logger.trace(String.format("Replacing bean definition '%s' existing class '%s' with " +  
                  "enhanced class '%s'", entry.getKey(), configClass.getName(), enhancedClass.getName()));  
         }  
         // 将代理配置类设为beanClass
         beanDef.setBeanClass(enhancedClass);  
      }  
   }  
   enhanceConfigClasses.tag("classCount", () -> String.valueOf(configBeanDefs.keySet().size())).end();  
}
```

在创建代理配置类时，会注册`BeanMethodInterceptor`和`BeanFactoryAwareMethodInterceptor`回调。

`BeanMethodInterceptor`会对`@Bean`方法进行拦截，从容器中获取`bean`。`ConfigurationClassEnhancer.BeanMethodInterceptor#intercept()`：
```java
public Object intercept(Object enhancedConfigInstance, Method beanMethod, Object[] beanMethodArgs,  
         MethodProxy cglibMethodProxy) throws Throwable {  
  
   ConfigurableBeanFactory beanFactory = getBeanFactory(enhancedConfigInstance);  
   // 获取beanName
   String beanName = BeanAnnotationHelper.determineBeanNameFor(beanMethod);  
  
   // Determine whether this bean is a scoped-proxy  
   if (BeanAnnotationHelper.isScopedProxy(beanMethod)) {  
      String scopedBeanName = ScopedProxyCreator.getTargetBeanName(beanName);  
      if (beanFactory.isCurrentlyInCreation(scopedBeanName)) {  
         beanName = scopedBeanName;  
      }  
   }  
  
   // FactoryBean处理流程：
   // To handle the case of an inter-bean method reference, we must explicitly check the  
   // container for already cached instances.  
   // First, check to see if the requested bean is a FactoryBean. If so, create a subclass   
   // proxy that intercepts calls to getObject() and returns any cached bean instance.   
   // This ensures that the semantics of calling a FactoryBean from within @Bean methods   
   // is the same as that of referring to a FactoryBean within XML. See SPR-6602.   
   if (factoryContainsBean(beanFactory, BeanFactory.FACTORY_BEAN_PREFIX + beanName) &&  
         factoryContainsBean(beanFactory, beanName)) {  
      Object factoryBean = beanFactory.getBean(BeanFactory.FACTORY_BEAN_PREFIX + beanName);  
      if (factoryBean instanceof ScopedProxyFactoryBean) {  
         // Scoped proxy factory beans are a special case and should not be further proxied  
      }  
      else {  
         // It is a candidate FactoryBean - go ahead with enhancement  
         return enhanceFactoryBean(factoryBean, beanMethod.getReturnType(), beanFactory, beanName);  
      }  
   }  
  
   if (isCurrentlyInvokedFactoryMethod(beanMethod)) {  
      // The factory is calling the bean method in order to instantiate and register the bean  
      // (i.e. via a getBean() call) -> invoke the super implementation of the method to actually      
      // create the bean instance.      
      if (logger.isInfoEnabled() &&  
            BeanFactoryPostProcessor.class.isAssignableFrom(beanMethod.getReturnType())) {  
         logger.info(String.format("@Bean method %s.%s is non-static and returns an object " +  
                     "assignable to Spring's BeanFactoryPostProcessor interface. This will " +  
                     "result in a failure to process annotations such as @Autowired, " +  
                     "@Resource and @PostConstruct within the method's declaring " +  
                     "@Configuration class. Add the 'static' modifier to this method to avoid " +  
                     "these container lifecycle issues; see @Bean javadoc for complete details.",  
               beanMethod.getDeclaringClass().getSimpleName(), beanMethod.getName()));  
      }  
      return cglibMethodProxy.invokeSuper(enhancedConfigInstance, beanMethodArgs);  
   }  
  
   // 处理bean引用：从容器中获取对应bean
   return resolveBeanReference(beanMethod, beanMethodArgs, beanFactory, beanName);  
}
```

`BeanFactoryAwareMethodInterceptor`则会对`BeanFactoryAware#setBeanFactory()`方法进行拦截：
```java
public Object intercept(Object obj, Method method, Object[] args, MethodProxy proxy) throws Throwable {  
   Field field = ReflectionUtils.findField(obj.getClass(), BEAN_FACTORY_FIELD);  
   Assert.state(field != null, "Unable to find generated BeanFactory field");  
   field.set(obj, args[0]);  
  
   // Does the actual (non-CGLIB) superclass implement BeanFactoryAware?  
   // If so, call its setBeanFactory() method. If not, just exit.   
   if (BeanFactoryAware.class.isAssignableFrom(ClassUtils.getUserClass(obj.getClass().getSuperclass()))) {  
      return proxy.invokeSuper(obj, args);  
   }  
   return null;  
}
```

### 2.2.3 注册ImportAwareBeanPostProcessor
`ConfigurationClassPostProcessor`还会注册`ImportAwareBeanPostProcessor`，它是一个`BeanPostProcessor`，会在`bean`实例化过程中进行功能增强。

`ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor#postProcessProperties()`会为实现`EnchancedConfiguration`接口的bean设置`beanFactory`：
```java
public PropertyValues postProcessProperties(@Nullable PropertyValues pvs, Object bean, String beanName) {  
   // Inject the BeanFactory before AutowiredAnnotationBeanPostProcessor's  
   // postProcessProperties method attempts to autowire other configuration beans.   
   if (bean instanceof EnhancedConfiguration) {  
      ((EnhancedConfiguration) bean).setBeanFactory(this.beanFactory);  
   }  
   return pvs;  
}
```

`ConfigurationClassPostProcessor.ImportAwareBeanPostProcessor#postProcessBeforeInitialization()`会为实现`ImportAware`接口的bean设置`@Import`引入的注解元信息：
```java
public Object postProcessBeforeInitialization(Object bean, String beanName) {  
   if (bean instanceof ImportAware) {  
      ImportRegistry ir = this.beanFactory.getBean(IMPORT_REGISTRY_BEAN_NAME, ImportRegistry.class);  
      AnnotationMetadata importingClass = ir.getImportingClassFor(ClassUtils.getUserClass(bean).getName());  
      if (importingClass != null) {  
         ((ImportAware) bean).setImportMetadata(importingClass);  
      }  
   }  
   return bean;  
}
```