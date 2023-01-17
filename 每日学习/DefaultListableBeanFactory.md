# BeanFactory体系
![[BeanFactory.png]]
`BeanFactory`是Spring最核心的功能，它就是我们常说的Spring IoC容器。

`BeanFactory`体系下包含许多接口，它们分别代表Spring IoC容器的不同功能：
- `BeanFactory`：提供最基础的获取`bean`信息的方法，如`getBean()`。
- `HierarchicalBeanFactory`：提供父子层级Spring容器的基础方法，如`getParentBeanFactory()`。
- `AutowireCapableBeanFactory`：提供实例化、自动装配等基础功能，如`createBean()`。
- `ListableBeanFactory`：提供枚举所有`bean`的功能，如`getBeansOfType()`。
- `ConfigurableBeanFactory`：提供配置Spring容器的基础功能，是`BeanFactory`接口的补充。如`addBeanPostProcessor()`。
- `ConfigurableListableBeanFactory`：提供获取和修改`BeanDefinition`、预实例化单例对象的功能。如`getBeanDefinition()`。
- `ApplicationContext`：应用层Spring容器的顶级接口。
- `BeanDefinitionRegistry`：提供注册`BeanDefinition`的功能，如`registerBeanDefinition()`。不是`BeanFactory`的子接口，但它是`BeanFactory`体系的核心组成部分。

虽然`BeanFactory`体系的接口众多，但是它们的核心实现类只有`DefaultListableBeanFactory`。

我们只需要按照`DefaultListableBeanFactory`的基本使用流程，掌握其中的关键性方法的源码，就能够很好的理解Spring IoC容器。

在对Spring IoC容器有了整体的认识后，再去针对性研究它提供的特性功能，就能够完全掌握Spring IoC容器。

# 2 DefaultListableBeanFactory

`DefaultListableBeanFactory`的成员变量很多，这里介绍其中最核心的：
- `beanDefinitionMap`：`BeanDefinition`的缓存，`key`是`beanName`。

`DefaultListableBeanFactory`中最核心的流程（方法）包括：
1. 注册`BeanDefinition`
2. 创建`bean`
3. 获取`bean`

## 2.1 registerBeanDefinition
注册`BeanDefinition`是`BeanDefinitionRegistry`接口提供的方法，`DefaultListableBeanFactory`对其进行了实现。

注册`BeanDefinition`的过程主要是将其保存到`beanDefinitionMap`缓存中，其中`beanName`作为`key`：
```java
private final Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>(256);
```

考虑到`beanName`可能被重复注册多次的情况。一方面，要对此做出限制，主要是通过`allowBeanDefinitionOverriding`属性；另一方面，在覆盖时需要将旧的`beanDefinition`衍生出的各种缓存清除，保证数据的一致性。

与`beanDefinition`相关的缓存包括（没有全部列出）：
- `mergedBeanDefinitions`、`mergedBeanDefinitionHolders`
- `singletonObjects`、`singletonFactories`、`earlySingletonObjects`、`registeredSingletons`、`disposableBeans`、`dependentBeanMap`

`DefaultListableBeanFactory#registerBeanDefinition()`：
```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)  
      throws BeanDefinitionStoreException {  
   // 校验beanDefinition信息的完整性
   if (beanDefinition instanceof AbstractBeanDefinition) {  
      try {  
         ((AbstractBeanDefinition) beanDefinition).validate();  
      }  
      catch (BeanDefinitionValidationException ex) {  
         throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, "Validation of bean definition failed", ex);  
      }  
   }  
  
   // 获取缓存
   BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);  
   // 如果该beanDefinition已经注册
   if (existingDefinition != null) {  
      // 如果beanFactory不允许beanDefinition重载，抛出异常
      if (!isAllowBeanDefinitionOverriding()) {  
         throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);  
      }  
      // 如果beanFactory允许beanDefinition重载，进行覆盖（默认会进行覆盖）
      this.beanDefinitionMap.put(beanName, beanDefinition);  
   }  
   // 如果该beanDefinition未注册
   else {  
      // 如果beanFactory已经创建bean
      if (hasBeanCreationStarted()) {  
         // 加锁，更新beanDefinitionMap缓存
         synchronized (this.beanDefinitionMap) {  
            this.beanDefinitionMap.put(beanName, beanDefinition);  
            List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);  
            updatedDefinitions.addAll(this.beanDefinitionNames);  
            updatedDefinitions.add(beanName);  
            this.beanDefinitionNames = updatedDefinitions;  
            removeManualSingletonName(beanName);  
         }  
      }  
      // 如果beanFactory还没有创建bean，仍处于注册bean阶段
      else {  
         // 直接更新beanDefinitionMap缓存
         this.beanDefinitionMap.put(beanName, beanDefinition);  
         this.beanDefinitionNames.add(beanName);  
         removeManualSingletonName(beanName);  
      }  
      this.frozenBeanDefinitionNames = null;  
   }  
   // 如果beanDefinition已存在，或单例bean已存在，需要清除相关缓存信息：mergedBeanDefinitions、singletonObjects等
   if (existingDefinition != null || containsSingleton(beanName)) {  
      resetBeanDefinition(beanName);  
   }  
   // 如果beanDefinition不存在，并且单例bean不存在，并且beanFactory会缓存所有beanDefinition的元数据
   else if (isConfigurationFrozen()) {  
      // 清除allBeanNamesByType和singletonBeanNamesByType
      clearByTypeCache();  
   }  
}
```

## 2.2 createBean
创建`bean`的底层方法位于`AbstractAutowireCapableBeanFactory#createBean()`。
```java
protected Object createBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {  
   RootBeanDefinition mbdToUse = mbd;  
   // 解析bean对应的beanClass对象，用于后面的实例化
   Class<?> resolvedClass = resolveBeanClass(mbd, beanName);  
   if (resolvedClass != null && !mbd.hasBeanClass() && mbd.getBeanClassName() != null) {  
      mbdToUse = new RootBeanDefinition(mbd);  
      mbdToUse.setBeanClass(resolvedClass);  
   }  
  
   // Prepare method overrides：校验lookup方法是否存在
   try {  
      mbdToUse.prepareMethodOverrides();  
   }  
   catch (BeanDefinitionValidationException ex) {  
      throw new BeanDefinitionStoreException(mbdToUse.getResourceDescription(),  
            beanName, "Validation of method overrides failed", ex);  
   }  
  
   try {  
      // 触发InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation()和BeanPostProcessor#postProcessAfterInitialization()方法
      Object bean = resolveBeforeInstantiation(beanName, mbdToUse);  
      // 如果通过InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation()方法实例化对象，就直接返回了
      if (bean != null) {  
         return bean;  
      }  
   }  
   catch (Throwable ex) {  
      throw new BeanCreationException(mbdToUse.getResourceDescription(), beanName,  
            "BeanPostProcessor before instantiation of bean failed", ex);  
   }  
  
   try {  
      // 使用BeanFactory默认策略实例化对象：使用instanceSupplier、factoryMethodName或构造函数
      Object beanInstance = doCreateBean(beanName, mbdToUse, args);
      return beanInstance;  
   }  
   catch (BeanCreationException | ImplicitlyAppearedSingletonException ex) {
      throw ex;  
   }  
   catch (Throwable ex) {  
      throw new BeanCreationException(  
            mbdToUse.getResourceDescription(), beanName, "Unexpected exception during bean creation", ex);  
   }  
}
```

### 2.2.1 解析beanClass
为了创建`bean`，首先要知道它的`beanClass`是什么。

解析常规`beanClass`的底层方法位于`AbstractBeanDefinition#resolveBeanClass()`。简单来说，它会根据`beanClassName`加载对应的类对象，并且缓存起来：
```java
public Class<?> resolveBeanClass(@Nullable ClassLoader classLoader) throws ClassNotFoundException {  
   String className = getBeanClassName();  
   if (className == null) {  
      return null;  
   }  
   Class<?> resolvedClass = ClassUtils.forName(className, classLoader);  
   this.beanClass = resolvedClass;  
   return resolvedClass;  
}
```

### 2.2.2 实例化前的回调
在创建`bean`之前，会先触发`InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation()`和`BeanPostProcessor#postProcessAfterInitialization()`方法回调：
```java
protected Object resolveBeforeInstantiation(String beanName, RootBeanDefinition mbd) {  
   Object bean = null;  
   if (!Boolean.FALSE.equals(mbd.beforeInstantiationResolved)) {  
      // Make sure bean class is actually resolved at this point.  
      if (!mbd.isSynthetic() && hasInstantiationAwareBeanPostProcessors()) {  
         Class<?> targetType = determineTargetType(beanName, mbd);  
         if (targetType != null) {  
            // InstantiationAwareBeanPostProcessor#postProcessBeforeInstantiation()回调
            bean = applyBeanPostProcessorsBeforeInstantiation(targetType, beanName);  
            if (bean != null) {  
               // BeanPostProcessor#postProcessAfterInitialization()
               bean = applyBeanPostProcessorsAfterInitialization(bean, beanName);  
            }  
         }  
      }  
      mbd.beforeInstantiationResolved = (bean != null);  
   }  
   return bean;  
}
```

在这个阶段，会触发常见的`InstantiationAwareBeanPostProcessor`实现类如下：
- `AbstractAutoProxyCreator`：将符合条件的`bean`用AOP代理封装起来，并指定代理的拦截器。

如果在这个阶段创建了`bean`，那么会直接返回，不会继续执行后续创建`bean`操作。

### 2.2.3 创建bean
`AbstractAutowireCapableBeanFactory#doCreateBean()`会根据`RootBeanDefinition`的信息进行创建对象。

简单来说，包括以下步骤：
1. 通过`instanceSupplier`、工厂方法和构造函数等方式创建对象。‘
2. 触发`MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition()`回调。
3. `InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation()`回调。
4. 依赖注入。
5. 触发`BeanNameAware`、`BeanClassLoaderAware`和`BeanFactoryAware`回调。
6. 触发`BeanPostProcessor#postProcessBeforeInitialization()`回调。
7. 触发`InitializingBean#afterPropertiesSet()`回调。
8. 触发`initMethod`回调。
9. 触发`BeanPostProcessor#postProcessAfterInitialization()`回调。

需要注意的是，由于存在循环依赖的情况：
1. 在创建`bean`后，依赖注入前，会将未完全实例化的`bean`信息缓存到`singletonFactories`和`registeredSingletons`。

`AbstractAutowireCapableBeanFactory#doCreateBean()`：
```java
protected Object doCreateBean(String beanName, RootBeanDefinition mbd, @Nullable Object[] args) throws BeanCreationException {  
   BeanWrapper instanceWrapper = null;  
   if (mbd.isSingleton()) {  
      // 如果factoryBeanInstanceCache有，直接取
      instanceWrapper = this.factoryBeanInstanceCache.remove(beanName);  
   }  
   if (instanceWrapper == null) {  
      /* 创建bean：
         1、instanceSupplier#get()方法创建
         2、factoryMethod工厂方法创建
         3、有参构造函数创建
         4、无参构造函数创建
      */
      instanceWrapper = createBeanInstance(beanName, mbd, args);  
   }  
   
   Object bean = instanceWrapper.getWrappedInstance();  
   Class<?> beanType = instanceWrapper.getWrappedClass();  
   if (beanType != NullBean.class) {  
      mbd.resolvedTargetType = beanType;  
   }  
  
   // 触发MergedBeanDefinitionPostProcessor#postProcessMergedBeanDefinition()回调
   synchronized (mbd.postProcessingLock) {  
      if (!mbd.postProcessed) {  
         try {  
            applyMergedBeanDefinitionPostProcessors(mbd, beanType, beanName);  
         }  
         catch (Throwable ex) {  
            throw new BeanCreationException(mbd.getResourceDescription(), beanName,  
                  "Post-processing of merged bean definition failed", ex);  
         }  
         mbd.postProcessed = true;  
      }  
   }  
  
   // Eagerly cache singletons to be able to resolve circular references  
   // even when triggered by lifecycle interfaces like BeanFactoryAware.   
   boolean earlySingletonExposure = (mbd.isSingleton() && this.allowCircularReferences && isSingletonCurrentlyInCreation(beanName));  
   if (earlySingletonExposure) {  
      // 如果是提前暴露的单例bean：缓存单例对象：singletonFactories和registeredSingletons
      addSingletonFactory(beanName, () -> getEarlyBeanReference(beanName, mbd, bean));  
   }  
  
   // 实例化bean
   Object exposedObject = bean;  
   try {  
      /* 
         1、InstantiationAwareBeanPostProcessor#postProcessAfterInstantiation()回调
         2、依赖注入
      */ 
      populateBean(beanName, mbd, instanceWrapper);  
      /*
	     1、触发BeanNameAware、BeanClassLoaderAware和BeanFactoryAware回调
	     2、触发BeanPostProcessor#postProcessBeforeInitialization()回调
	     3、触发InitializingBean#afterPropertiesSet()回调
	     4、触发initMethod回调
	     5、触发BeanPostProcessor#postProcessAfterInitialization()回调
      */
      exposedObject = initializeBean(beanName, exposedObject, mbd);  
   }  
   catch (Throwable ex) {  
      
   }  
   // 如果是提前暴露的单例bean
   if (earlySingletonExposure) {  
      // 从缓存中获取：singletonObjects、earlySingletonObjects、singletonFactories
      Object earlySingletonReference = getSingleton(beanName, false);  
      if (earlySingletonReference != null) {  
         if (exposedObject == bean) {  
            exposedObject = earlySingletonReference;  
         }  
         else if (!this.allowRawInjectionDespiteWrapping && hasDependentBean(beanName)) {  
            // 清除依赖bean的缓存
            String[] dependentBeans = getDependentBeans(beanName);  
            Set<String> actualDependentBeans = new LinkedHashSet<>(dependentBeans.length);  
            for (String dependentBean : dependentBeans) {  
               if (!removeSingletonIfCreatedForTypeCheckOnly(dependentBean)) {  
                  actualDependentBeans.add(dependentBean);  
               }  
            }  
            if (!actualDependentBeans.isEmpty()) {  
               throw new BeanCurrentlyInCreationException();  
            }  
         }  
      }  
   }  
  
   // Register bean as disposable.  
   try {  
      registerDisposableBeanIfNecessary(beanName, bean, mbd);  
   }  
   catch (BeanDefinitionValidationException ex) {  
      throw new BeanCreationException(  
            mbd.getResourceDescription(), beanName, "Invalid destruction signature", ex);  
   }  
  
   return exposedObject;  
}
```
# getBean
