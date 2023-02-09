传统的Spring项目，需要我们对每个引入的组件进行手动配置。

这需要开发者对组件有深入的了解，否则很容易遗漏某些细节。对于业务开发人员/公司来说，他们只需要知道如何使用组件即可，不需要过多了解底层配置原理。如果有多个项目，则需要将配置进行多次拷贝，会增大无意义的工作量。

实际上，每个第三方组件的配置都是相对固定的，只有其中一些参数可能需要根据运行环境进行修改。例如`spring-mvc`有它自己的一套配置，`spring-mybatis`也有它自己的一套配置。我们可以将这些第三方组件的基础配置抽取出来，通过配置文件动态修改其中的运行参数。后续需要重复使用时，只需要将相同的基础依赖，可以大大减小重复开发的工作量。

另一方面，组件开发者肯定要比我们这些使用者更加熟悉底层细节。因此，最稳妥的方法是由组件开发者去抽象对应组件的基础配置。我们使用者只需要熟悉组件暴露的配置文件即可。这些由组件开发者抽象出的基础配置，在Spring Boot中就是`starter`。

Spring Boot就是为了解决上述问题而开发出来的。它提供自动配置的`SPI`机制，制定了从`starter`读取配置的规则。第三方组件根据规则编写基础配置信息。后续引入依赖时，我们只要开启`EnableAutoConfiguration`注解，Spring Boot会根据SPI规则读取配置。

![[EnableAutoConfiguration.png]]
上图Spring Boot自动配置`SPI`机制的相关类图，主要可以分为3个模块：
1. `ConfigurationClassPostProcessor`：基于`@Configuration`的`BeanFactoryPostProcessor`。
2. `AutoConfigurationImportSelector`：注册指定目录文件中的配置类。
3. `Registrar`：注册指定路径下的类。

Spring Boot自动配置的`SPI`机制原理基于`ConfigurationClassPostProcessor`，它是一个`BeanFactoryPostProcessor`，会在读取依赖配置后，对配置类`BeanDefinition`进行处理。自动配置机制使用了其中的`@Import`功能。

`@Import`注解可以注册指定依赖配置：
1. `ImportSelector`：根据选择规则注册。
2. `ImportBeanDefinitionRegistrar`：注册额外依赖配置。
3. 其他：作为配置类注册。

通过`org.springframework.boot.autoconfigure.AutoConfigurationPackages.Registrar`和`org.springframework.boot.autoconfigure.AutoConfigurationImportSelector`，自动配置`SPI`机制会自动注册以下依赖：
1. 标注`@EnableAutoConfiguration`注解的配置类所在包下的所有注解依赖。
2. 注册`META-INF/spring.factories`中键为`org.springframework.boot.autoconfigure.EnableAutoConfiguration`的类。
3. 注册`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`中的所有类。

对于`starter`来说，它只需要定义要相关配置，然后在`META-INF/spring.factories`或`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`中指定配置类就可以了。

# 1 @EnableAutoConfiguration详解
`@EnableAutoConfiguration`可以开启Spring Boot的自动注册功能，由于它基于`ConfigurationClassPostProcessor`，所以需要同时添加`@Configuration`注解：
```java
@Configuration
@EnableAutoConfiguration
public class AppConfig {
}
```

`@EnableAutoConfiguration`核心源码如下，它会使用`AutoConfigurationImportSelector`，注册`META-INF/spring.factories`和`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`中的类。：
```java
@AutoConfigurationPackage  
@Import(AutoConfigurationImportSelector.class)  
public @interface EnableAutoConfiguration {  
   // environment变量是否可以覆盖starter中的配置变量
   String ENABLED_OVERRIDE_PROPERTY = "spring.boot.enableautoconfiguration";  
   // 指定排除的类
   Class<?>[] exclude() default {};  
   // 指定排除的类名
   String[] excludeName() default {};  
}
```

元注解`@AutoConfigurationPackage`的核心源码如下，默认会扫描标注该注解的类所在的包路径：
```java
@Import(AutoConfigurationPackages.Registrar.class)  
public @interface AutoConfigurationPackage {  
   // 指定扫描包路径
   String[] basePackages() default {};  
   // 指定类，会扫描该类所在包路径
   Class<?>[] basePackageClasses() default {};  
}
```

需要注意的是，我们通常不会直接使用`@EnableAutoConfiguration`，而是使用集成自动配置功能的`@SpringBootApplication`，它与自动配置相关的核心源码如下：
```java
@SpringBootConfiguration  // 集成@Configuration功能
@EnableAutoConfiguration  
@ComponentScan(excludeFilters = { @Filter(type = FilterType.CUSTOM, classes = TypeExcludeFilter.class),  
      @Filter(type = FilterType.CUSTOM, classes = AutoConfigurationExcludeFilter.class) })  
public @interface SpringBootApplication {  
   // 指定自动配置的排除类
   @AliasFor(annotation = EnableAutoConfiguration.class)  
   Class<?>[] exclude() default {};  
   // 指定自动配置的排除类名
   @AliasFor(annotation = EnableAutoConfiguration.class)  
   String[] excludeName() default {};  
}
```

# 2 自动注册SPI机制原理
## 2.1 @Import功能
`@Import`是`ConfigurationClassPostProcessor`针对`@Configuration`配置类提供的增强功能，具体原理可以查看相关文章（[@Configuration的基本使用和工作原理](https://www.cnblogs.com/Xianhuii/p/17090626.html)），这里主要介绍@Import的相关原理。

`ConfigurationClassParser#processImports()`定义了具体导入流程：
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
         for (SourceClass candidate : importCandidates) {  
            // ImportSelector导入流程
            if (candidate.isAssignable(ImportSelector.class)) {  
               // Candidate class is an ImportSelector -> delegate to it to determine imports  
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
            // ImportBeanDefinitionRegistrar导入流程
            else if (candidate.isAssignable(ImportBeanDefinitionRegistrar.class)) {  
               // Candidate class is an ImportBeanDefinitionRegistrar ->  
               // delegate to it to register additional bean definitions               Class<?> candidateClass = candidate.loadClass();  
               ImportBeanDefinitionRegistrar registrar =  
                     ParserStrategyUtils.instantiateClass(candidateClass, ImportBeanDefinitionRegistrar.class,  
                           this.environment, this.resourceLoader, this.registry);  
               configClass.addImportBeanDefinitionRegistrar(registrar, currentSourceClass.getMetadata());  
            }  
            // 配置类导入流程
            else {  
               // Candidate class not an ImportSelector or ImportBeanDefinitionRegistrar ->  
               // process it as an @Configuration class               
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

### 2.1.1 DeferredImportSelector
对于`DeferredImportSelector`，导入入口位于`ConfigurationClassParser.DeferredImportSelectorHandler#handle()`：
```java
public void handle(ConfigurationClass configClass, DeferredImportSelector importSelector) {  
   DeferredImportSelectorHolder holder = new DeferredImportSelectorHolder(configClass, importSelector);  
   if (this.deferredImportSelectors == null) {  
      DeferredImportSelectorGroupingHandler handler = new DeferredImportSelectorGroupingHandler();  
      handler.register(holder);  
      // 实际导入逻辑
      handler.processGroupImports();  
   }  
   else {  
      this.deferredImportSelectors.add(holder);  
   }  
}
```

它会从DeferredImportSelector实现类中获取需要导入的类，然后继续按照按ImportSelector、ImportBeanDefinitionRegistrar和配置类递归进行导入。`ConfigurationClassParser.DeferredImportSelectorGroupingHandler#processGroupImports()`：
```java
public void processGroupImports() {  
   for (DeferredImportSelectorGrouping grouping : this.groupings.values()) {  
      Predicate<String> exclusionFilter = grouping.getCandidateFilter();  
      // 获取分组导入的类，递归进行导入（按ImportSelector、ImportBeanDefinitionRegistrar和配置类）
      grouping.getImports().forEach(entry -> {  
         ConfigurationClass configurationClass = this.configurationClasses.get(entry.getMetadata());  
         try {  
            processImports(configurationClass, asSourceClass(configurationClass, exclusionFilter),  
                  Collections.singleton(asSourceClass(entry.getImportClassName(), exclusionFilter)),  
                  exclusionFilter, false);  
         }  
         catch (BeanDefinitionStoreException ex) {  
            throw ex;  
         }  
         catch (Throwable ex) {  
            throw new BeanDefinitionStoreException(  
                  "Failed to process import candidates for configuration class [" +  
                        configurationClass.getMetadata().getClassName() + "]", ex);  
         }  
      });  
   }  
}
```

### 2.1.2 ImportBeanDefinitionRegistrar
对于`ImportBeanDefinitionRegistrar`，它会调用实现类的`ImportBeanDefinitionRegistrar#registerBeanDefinitions()`方法进行注册：
```java
default void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {  
}
```

## 2.2 AutoConfigurationImportSelector

