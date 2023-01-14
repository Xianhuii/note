# 1 介绍
`ClassPathBeanDefinitionScanner`可以扫描指定路径下的`@Component`类，将这些类解析成`BeanDefinition`，注册到Spring容器中。

在`AnnotationConfigApplicationContext`中，就会使用这种方式加载指定路径下的`bean`。例如：
```java
ApplicationContext context = new AnnotationConfigApplicationContext("com.example.component");
```

![[ClassPathBeanDefinitionScanner 2.png]]
`ClassPathScanningCandidateComponentProvider`成员变量：
- `resourcePattern`：资源文件的路径匹配模式，默认是`**/*.class`，表示会扫描所有类文件。
- `includeFilters`：过滤器，满足条件的类会被注册成`bean`。默认条件为标注`@Component`、`@ManagedBean`或`@Named`注解，后两个条件成立需要引入相关依赖。
- `excludeFilters`：过滤器，满足条件的类会被跳过，不会被注册成`bean`。默认为空。
- `environment`：运行时环境，可获取系统变量和配置文件信息。
- `conditionEvaluator`：会根据`@Conditional`注解判断是否需要注册成`bean`。
- `resourcePatternResolver`：根据路径匹配模式获取类文件的`Resource`对象数组。
- `metadataReaderFactory`：`MetadataReader`工厂，用来读取类文件的元数据。
- `componentsIndex`：

`ClassPathBeanDefinitionScanner`成员变量：
- `registry`：`BeanDefinition`注册器，实际上就是Spring容器。
- `beanDefinitionDefaults`：`BeanDefinition`默认属性的封装工具。
- `autowireCandidatePatterns`：
- `beanNameGenerator`：`beanName`生成器，会先获取`@Component`、`@ManagedBean`、`@Named`或`@Component`子注解的`value`属性，没有再按类名生成。
- `scopeMetadataResolver`：作用域解析器，会先获取`@Scope`注解的`value`和`proxyMode`属性，没有则使用默认值（`singleton`和`ScopedProxyMode.NO`）。
- `includeAnnotationConfig`：是否往Spring容器注册默认的`XxxProcessor`，默认为`true`。
# 2 基本使用
在使用`ClassPathBeanDefinitionScanner`时，首先需要为其设置`registry`属性，通常通过构造函数进行设置。

`DefaultListableBeanFactory`和`GenericApplicationContext`都实现了`BeandefinitionRegistry`接口，Spring容器实现类基本上都可以作为`registry`设置到`ClassPathBeanDefinitionScanner`中。

然后，通过`ClassPathBeanDefinitionScanner#scan()`就可以扫描指定包下的所有类文件，将符合条件的类作为`BeanDefinition`，注册到`registry`中。

以下是使用`ClassPathBeanDefinitionScanner`扫描指定包下所有`bean`的最基本使用：
```java
// 创建registry  
BeanDefinitionRegistry registry = new DefaultListableBeanFactory();  
// 创建scanner，设置registry  
ClassPathBeanDefinitionScanner scanner = new ClassPathBeanDefinitionScanner(registry);  
// 扫描指定包下的bean  
scanner.scan("com.example.component");  
// 获取bean  
BeanFactory beanFactory = (BeanFactory) registry;  
Object bean = beanFactory.getBean("xxx");
```

# 3 源码解读
## 3.1 构造函数初始化
我们通常会使用`ClassPathBeanDefinitionScanner(BeanDefinitionRegistry)`构造函数进行初始化，为其指定`registry`属性：
```java
public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry) {  
   this(registry, true);  
}
```

在这个过程中，除了`registry`，还会初始化`includeFilters`、`environment`、`resourcePatternResolver`、`metadataReaderFactory`和`componentsIndex`等属性：
```java
public ClassPathBeanDefinitionScanner(BeanDefinitionRegistry registry, boolean useDefaultFilters,  
      Environment environment, @Nullable ResourceLoader resourceLoader) {  
  
   Assert.notNull(registry, "BeanDefinitionRegistry must not be null");  
   // 初始化registry
   this.registry = registry;  
   // 初始化includeFilters：添加@Component、@ManagedBean和@Named条件
   if (useDefaultFilters) {  
      registerDefaultFilters();  
   }  
   // 初始化environment
   setEnvironment(environment);  
   // 初始化resourcePatternResolver、metadataReaderFactory和componentsIndex
   setResourceLoader(resourceLoader);  
}
```

在`ClassPathScanningCandidateComponentProvider#registerDefaultFilters()`方法中，会添加校验`@Component`、`@ManagedBean`和`@Named`三个条件的注解过滤器。需要注意的是，后两个过滤器只有在引入相关依赖的时候才会生效：
```java
protected void registerDefaultFilters() {  
   this.includeFilters.add(new AnnotationTypeFilter(Component.class));  
   ClassLoader cl = ClassPathScanningCandidateComponentProvider.class.getClassLoader();  
   try {  
      this.includeFilters.add(new AnnotationTypeFilter(  
            ((Class<? extends Annotation>) ClassUtils.forName("javax.annotation.ManagedBean", cl)), false));  
      logger.trace("JSR-250 'javax.annotation.ManagedBean' found and supported for component scanning");  
   }  
   catch (ClassNotFoundException ex) {  
      // JSR-250 1.1 API (as included in Java EE 6) not available - simply skip.  
   }  
   try {  
      this.includeFilters.add(new AnnotationTypeFilter(  
            ((Class<? extends Annotation>) ClassUtils.forName("javax.inject.Named", cl)), false));  
      logger.trace("JSR-330 'javax.inject.Named' annotation found and supported for component scanning");  
   }  
   catch (ClassNotFoundException ex) {  
      // JSR-330 API not available - simply skip.  
   }  
}
```

在`ClassPathScanningCandidateComponentProvider#setResourceLoader()`方法中，会初始化`resourcePatternResolver`、`metadataReaderFactory`和`componentsIndex`：
```java
public void setResourceLoader(@Nullable ResourceLoader resourceLoader) {  
   this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(resourceLoader);  
   this.metadataReaderFactory = new CachingMetadataReaderFactory(resourceLoader);  
   this.componentsIndex = CandidateComponentsIndexLoader.loadIndex(this.resourcePatternResolver.getClassLoader());  
}
```

在初始化`resourcePatternResolver`时，会判断`registry`是否是`ResourcePatternResolver`实现类（因为`ApplicationContext`继承了`ResourcePatternResolver`接口，而`DefaultListableBeanFactory`则没有）。如果是，直接将`registry`赋值给`resourcePatternResolver`；如果不是，则会新建一个`PathMatchingResourcePatternResolver`对象：
```java
public static ResourcePatternResolver getResourcePatternResolver(@Nullable ResourceLoader resourceLoader) {  
   if (resourceLoader instanceof ResourcePatternResolver) {  
      return (ResourcePatternResolver) resourceLoader;  
   }  
   else if (resourceLoader != null) {  
      return new PathMatchingResourcePatternResolver(resourceLoader);  
   }  
   else {  
      return new PathMatchingResourcePatternResolver();  
   }  
}
```

在初始化`componentsIndex`时，会尝试读取`META-INF/spring.components`文件中定义的配置信息，后续扫描时会从配置信息里获取符合条件的`bean`进行加载：
```java
private static CandidateComponentsIndex doLoadIndex(ClassLoader classLoader) {  
   // 读取spring.index.ignore配置，需要关闭spring.components功能时可以将设为false
   if (shouldIgnoreIndex) {  
      return null;  
   }  
  
   try {  
      // 读取META-INF/spring.components
      Enumeration<URL> urls = classLoader.getResources(COMPONENTS_RESOURCE_LOCATION);  
      if (!urls.hasMoreElements()) {  
         return null;  
      }  
      // 获取配置
      List<Properties> result = new ArrayList<>();  
      while (urls.hasMoreElements()) {  
         URL url = urls.nextElement();  
         Properties properties = PropertiesLoaderUtils.loadProperties(new UrlResource(url));  
         result.add(properties);  
      }  
      if (logger.isDebugEnabled()) {  
         logger.debug("Loaded " + result.size() + " index(es)");  
      }  
      int totalCount = result.stream().mapToInt(Properties::size).sum();  
      return (totalCount > 0 ? new CandidateComponentsIndex(result) : null);  
   }  
   catch (IOException ex) {  
      throw new IllegalStateException("Unable to load indexes from location [" +  
            COMPONENTS_RESOURCE_LOCATION + "]", ex);  
   }  
}
```

`META-INF/spring.components`文件格式如下：
```
全限定类名1=注解全限定类名1,注解全限定类名2
全限定类名2=注解全限定类名1
```

需要特别注意的是，开启`spring.components`还需要`includeFilters`中仅支持`@Indexed`及其子注解的条件过滤器，如果初始化了`componentsIndex`，后续扫描时只会在`META-INF/spring.components`文件中筛选路径匹配的类进行注册。如果遇到包下的`bean`扫描不到时，可以从这方面考虑。

## 3.2 扫描
通过`ClassPathBeanDefinitionScanner#scan()`方法可以扫描指定包，注册`XxxProcessor`，并且计算本次注册`bean`的数量：
```java
public int scan(String... basePackages) {  
   int beanCountAtScanStart = this.registry.getBeanDefinitionCount();  
   // 实际扫描方法
   doScan(basePackages);  
   // 注册XxxProcessor
   if (this.includeAnnotationConfig) {  
      AnnotationConfigUtils.registerAnnotationConfigProcessors(this.registry);  
   }  
   return (this.registry.getBeanDefinitionCount() - beanCountAtScanStart);  
}
```

`ClassPathBeanDefinitionScanner#doScan()`方法是扫描的核心方法，它会扫描指定包，设置`BeanDefinition`的基本属性，最后注册到`registry`中：
```java
protected Set<BeanDefinitionHolder> doScan(String... basePackages) {  
   Assert.notEmpty(basePackages, "At least one base package must be specified");  
   Set<BeanDefinitionHolder> beanDefinitions = new LinkedHashSet<>();  
   // 遍历basePackages
   for (String basePackage : basePackages) {  
      // 从指定basePackage中筛选满足条件的类，解析成BeanDefinition
      Set<BeanDefinition> candidates = findCandidateComponents(basePackage);  
      // 遍历candidates，设置基本属性
      for (BeanDefinition candidate : candidates) {  
         // 设置作用域：读取@Scope属性进行设置
         ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(candidate);  
         candidate.setScope(scopeMetadata.getScopeName());  
         // 设置beanName：读取@Component属性，或者根据类名生成
         String beanName = this.beanNameGenerator.generateBeanName(candidate, this.registry);  
         // 设置AbstractBeanDefinition相关基本属性
         if (candidate instanceof AbstractBeanDefinition) {  
            postProcessBeanDefinition((AbstractBeanDefinition) candidate, beanName);  
         }  
         // 设置AnnotatedBeanDefinition相关基本属性
         if (candidate instanceof AnnotatedBeanDefinition) {  
            AnnotationConfigUtils.processCommonDefinitionAnnotations((AnnotatedBeanDefinition) candidate);  
         }  
         // 校验是否重复注册&注册到Spring容器
         if (checkCandidate(beanName, candidate)) {  
            BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(candidate, beanName);  
            definitionHolder =  
                  AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);  
            beanDefinitions.add(definitionHolder);  
            registerBeanDefinition(definitionHolder, this.registry);  
         }  
      }  
   }  
   return beanDefinitions;  
}
```

`ClassPathScanningCandidateComponentProvider#findCandidateComponents()`包含扫描包的核心代码。

如果开启了`spring.components`配置，并且`includeFilters`中只有`@Indexed`及其子注解条件（或者注解全限定类名以`javax.`开头），就会从`spring.components`中扫描满足条件的类进行解析。

如果没有开启`spring.component`配置，或者`includeFilters`不满足条件，就会从类路径中扫描满足条件的类进行解析。

`ClassPathScanningCandidateComponentProvider#findCandidateComponents()`源码如下：
```java
public Set<BeanDefinition> findCandidateComponents(String basePackage) {  
   // 开启spring.components配置，并且includeFilters过滤器仅支持@Indexed及其子注解条件时：从配置中扫描
   if (this.componentsIndex != null && indexSupportsIncludeFilters()) {  
      return addCandidateComponentsFromIndex(this.componentsIndex, basePackage);  
   }  
   // 从指定包中扫描
   else {  
      return scanCandidateComponents(basePackage);  
   }  
}
```

我们先来看`ClassPathScanningCandidateComponentProvider#addCandidateComponentsFromIndex()`方法，
```java
private Set<BeanDefinition> addCandidateComponentsFromIndex(CandidateComponentsIndex index, String basePackage) {  
   Set<BeanDefinition> candidates = new LinkedHashSet<>();  
   try {  
      // 根据includeFilters中的条件和basePackage从spring.components中获取对应类
      Set<String> types = new HashSet<>();  
      for (TypeFilter filter : this.includeFilters) {  
         // stereotype是注解全限定类名，如org.springframework.stereotype.Component
         String stereotype = extractStereotype(filter);  
         if (stereotype == null) {  
            throw new IllegalArgumentException("Failed to extract stereotype from " + filter);  
         }  
         // 根据basePackage和stereotype，从spring.components中获取对应类
         types.addAll(index.getCandidateTypes(basePackage, stereotype));  
      }  
      // 遍历所有类
      for (String type : types) {  
         // 获取元数据
         MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(type);  
         // 校验是否满足excludeFilters和includeFilters条件（注意校验顺序）
         if (isCandidateComponent(metadataReader)) {  
            // 解析成BeanDefinition，设置基本属性
            ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);  
            sbd.setSource(metadataReader.getResource());  
            // 校验BeanDefinition是否满足条件：比如是否可以进行初始化
            if (isCandidateComponent(sbd)) {  
               candidates.add(sbd);  
            }  
         }
      }  
   }  
   catch (IOException ex) {  
      throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);  
   }  
   return candidates;  
}
```

`CandidateComponentsIndex#getCandidateTypes()`方法定义了从`spring.components`扫描的逻辑：
```java
public Set<String> getCandidateTypes(String basePackage, String stereotype) {  
   // 获取符合注解条件的所有候补类
   List<Entry> candidates = this.index.get(stereotype);  
   if (candidates != null) {  
      // 过滤出满足包条件的所有类
      return candidates.parallelStream()  
            .filter(t -> t.match(basePackage))  
            .map(t -> t.type)  
            .collect(Collectors.toSet());  
   }  
   return Collections.emptySet();  
}
```

`SimpleMetadataReaderFactory#getMetadataReader()`
# 4 典型案例
