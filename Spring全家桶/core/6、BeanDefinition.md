# 1 介绍
`BeanDefinition`是Spring中非常重要的一个概念。

在Spring启动时，会读取项目中依赖关系的配置（`xml`文件、`groovy`文件或注解），将这些依赖关系通过`BeanDefinition`进行缓存。

在实例化`bean`时，Spring容器会根据`BeanDefinition`中的信息进行创建对象、设置属性，也就是我们常说的控制反转（或者依赖注入）。

在缓存`BeanDefinition`后，实例化`bean`前，还可以通过`BeanFactoryPostProcessor`等方式对`BeanDefinition`进行修改，以此来增强功能（AOP就是通过类似方式实现的）。

因此，我们也可以从这个角度来理解Spring容器：配置文件→`BeanDefinition`→`bean`。

# 2 体系
![[BeanDefinition 1.png]]
`BeanDefinition`定义了基础的`getter/setter`方法，用来获取和设置`bean`的信息。

`AbstractBeanDefinition`提取了`bean`的通用信息：
![[AbstractBeanDefinition.png]]

`RootBeanDefinition`保存了`bean`的完整信息以及实例化状态，Spring容器会根据`RootBeanDefinition`进行创建对象和设置属性：
![[RootBeanDefinition.png]]

Spring容器在读取`xml`文件、`groovy`文件和注解等配置信息时，会首先通过`GenericBeanDefinition`来保存，它主要在`AbstractBeanDefinition`的基础上定义了`bean`配置的父子关系：
![[GenericBeanDefinition.png]]

在Spring容器实例化`bean`时，会对`GenericBeanDefinition`进行一个`merge`操作，将其完整信息保存到`RootBeanDefinition`中进行后续实例化。

对于不同形式的配置文件，可能会使用对应的`GenericBeanDefinition`子类进行保存额外信息。例如，`AnnotatedBeanDefinitionReader`用`AnnotatedGenericBeanDefinition`保存，`ClassPathBeanDefinitionScanner`使用`ScannedGenericBeanDefinition`保存。

# 3 典型使用
## 3.1 AnnotatedBeanDefinitionReader
`AnnotatedBeanDefinitionReader`在注册类对象时，会创建`AnnotatedGenericBeanDefinition`对象保存依赖关系。

`AnnotatedBeanDefinitionReader#doRegisterBean()`：
```java
private <T> void doRegisterBean(Class<T> beanClass, 
								@Nullable String name,  
								@Nullable Class<? extends Annotation>[] qualifiers, 
								@Nullable Supplier<T> supplier,  
								@Nullable BeanDefinitionCustomizer[] customizers) {  
  
   AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);  
   if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {  
      return;  
   }  
  
   abd.setInstanceSupplier(supplier);  
   ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);  
   abd.setScope(scopeMetadata.getScopeName());  
   String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));  
  
   AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);  
   if (qualifiers != null) {  
      for (Class<? extends Annotation> qualifier : qualifiers) {  
         if (Primary.class == qualifier) {  
            abd.setPrimary(true);  
         }  
         else if (Lazy.class == qualifier) {  
            abd.setLazyInit(true);  
         }  
         else {  
            abd.addQualifier(new AutowireCandidateQualifier(qualifier));  
         }  
      }  
   }  
   if (customizers != null) {  
      for (BeanDefinitionCustomizer customizer : customizers) {  
         customizer.customize(abd);  
      }  
   }  
  
   BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);  
   definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);  
   BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);  
}
```

## 3.2 ClassPathBeanDefinitionScanner
`ClassPathBeanDefinitionScanner`在注册类对象时，会创建`ScannedGenericBeanDefinition`对象保存依赖关系。

`ClassPathScanningCandidateComponentProvider#scanCandidateComponents()`：
```java
private Set<BeanDefinition> scanCandidateComponents(String basePackage) {  
   Set<BeanDefinition> candidates = new LinkedHashSet<>();  
   try {  
      String packageSearchPath = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX +  
            resolveBasePackage(basePackage) + '/' + this.resourcePattern;  
      Resource[] resources = getResourcePatternResolver().getResources(packageSearchPath);  
      boolean traceEnabled = logger.isTraceEnabled();  
      boolean debugEnabled = logger.isDebugEnabled();  
      for (Resource resource : resources) {  
         if (traceEnabled) {  
            logger.trace("Scanning " + resource);  
         }  
         try {  
            MetadataReader metadataReader = getMetadataReaderFactory().getMetadataReader(resource);  
            if (isCandidateComponent(metadataReader)) {  
               ScannedGenericBeanDefinition sbd = new ScannedGenericBeanDefinition(metadataReader);  
               sbd.setSource(resource);  
               if (isCandidateComponent(sbd)) {  
                  if (debugEnabled) {  
                     logger.debug("Identified candidate component class: " + resource);  
                  }  
                  candidates.add(sbd);  
               }  
               else {  
                  if (debugEnabled) {  
                     logger.debug("Ignored because not a concrete top-level class: " + resource);  
                  }  
               }  
            }  
            else {  
               if (traceEnabled) {  
                  logger.trace("Ignored because not matching any filter: " + resource);  
               }  
            }  
         }  
         catch (FileNotFoundException ex) {  
            if (traceEnabled) {  
               logger.trace("Ignored non-readable " + resource + ": " + ex.getMessage());  
            }  
         }  
         catch (Throwable ex) {  
            throw new BeanDefinitionStoreException(  
                  "Failed to read candidate component class: " + resource, ex);  
         }  
      }  
   }  
   catch (IOException ex) {  
      throw new BeanDefinitionStoreException("I/O failure during classpath scanning", ex);  
   }  
   return candidates;  
}
```

## 3.3 AbstractBeanFactory
`AbstractBeanFactory`在实例化`bean`时，首先会将`GenericBeanDefinition`合并成`RootBeanDefinition`。

`AbstractBeanFactory#getMergedBeanDefinition()`：
```java
protected RootBeanDefinition getMergedBeanDefinition(  
      String beanName, BeanDefinition bd, @Nullable BeanDefinition containingBd)  
      throws BeanDefinitionStoreException {  
  
   synchronized (this.mergedBeanDefinitions) {  
      RootBeanDefinition mbd = null;  
      RootBeanDefinition previous = null;  
  
      // Check with full lock now in order to enforce the same merged instance.  
      if (containingBd == null) {  
         mbd = this.mergedBeanDefinitions.get(beanName);  
      }  
  
      if (mbd == null || mbd.stale) {  
         previous = mbd;  
         if (bd.getParentName() == null) {  
            // Use copy of given root bean definition.  
            if (bd instanceof RootBeanDefinition) {  
               mbd = ((RootBeanDefinition) bd).cloneBeanDefinition();  
            }  
            else {  
               mbd = new RootBeanDefinition(bd);  
            }  
         }  
         else {  
            // Child bean definition: needs to be merged with parent.  
            BeanDefinition pbd;  
            try {  
               String parentBeanName = transformedBeanName(bd.getParentName());  
               if (!beanName.equals(parentBeanName)) {  
                  pbd = getMergedBeanDefinition(parentBeanName);  
               }  
               else {  
                  BeanFactory parent = getParentBeanFactory();  
                  if (parent instanceof ConfigurableBeanFactory) {  
                     pbd = ((ConfigurableBeanFactory) parent).getMergedBeanDefinition(parentBeanName);  
                  }  
                  else {  
                     throw new NoSuchBeanDefinitionException(parentBeanName,  
                           "Parent name '" + parentBeanName + "' is equal to bean name '" + beanName +  
                                 "': cannot be resolved without a ConfigurableBeanFactory parent");  
                  }  
               }  
            }  
            catch (NoSuchBeanDefinitionException ex) {  
               throw new BeanDefinitionStoreException(bd.getResourceDescription(), beanName,  
                     "Could not resolve parent bean definition '" + bd.getParentName() + "'", ex);  
            }  
            // Deep copy with overridden values.  
            mbd = new RootBeanDefinition(pbd);  
            mbd.overrideFrom(bd);  
         }  
  
         // Set default singleton scope, if not configured before.  
         if (!StringUtils.hasLength(mbd.getScope())) {  
            mbd.setScope(SCOPE_SINGLETON);  
         }  
  
         // A bean contained in a non-singleton bean cannot be a singleton itself.  
         // Let's correct this on the fly here, since this might be the result of         // parent-child merging for the outer bean, in which case the original inner bean         // definition will not have inherited the merged outer bean's singleton status.         if (containingBd != null && !containingBd.isSingleton() && mbd.isSingleton()) {  
            mbd.setScope(containingBd.getScope());  
         }  
  
         // Cache the merged bean definition for the time being  
         // (it might still get re-merged later on in order to pick up metadata changes)         if (containingBd == null && isCacheBeanMetadata()) {  
            this.mergedBeanDefinitions.put(beanName, mbd);  
         }  
      }  
      if (previous != null) {  
         copyRelevantMergedBeanDefinitionCaches(previous, mbd);  
      }  
      return mbd;  
   }  
}
```