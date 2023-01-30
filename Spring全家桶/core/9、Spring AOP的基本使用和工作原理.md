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
   // 注册AnnotationAwareAspectJAutoProxyCreator，并且设置优先级为最高
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

## 2.3 AnnotationAwareAspectJAutoProxyCreator
![[AnnotationAwareAspectJAutoProxyCreator.png]]

### 2.3.1 初始化
`AnnotationAwareAspectJAutoProxyCreator`在注册时，设置的权重为最大值。因此，它会先被容器创建。

`AnnotationAwareAspectJAutoProxyCreator`实现类`BeanFactoryAware`接口，容器创建时会触发其`setBeanFactory()`方法，其中调用的`initBeanFactory()`方法会对其成员变量进行初始化。：
```java
public void setBeanFactory(BeanFactory beanFactory) {  
   super.setBeanFactory(beanFactory);  
   if (!(beanFactory instanceof ConfigurableListableBeanFactory)) {  
      throw new IllegalArgumentException(  
            "AdvisorAutoProxyCreator requires a ConfigurableListableBeanFactory: " + beanFactory);  
   }  
   initBeanFactory((ConfigurableListableBeanFactory) beanFactory);  
}
```

`AbstractAdvisorAutoProxyCreator#initBeanFactory()`：
```java
protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {  
   this.advisorRetrievalHelper = new BeanFactoryAdvisorRetrievalHelperAdapter(beanFactory);  
}
```

`AnnotationAwareAspectJAutoProxyCreator#initBeanFactory()`：
```java
protected void initBeanFactory(ConfigurableListableBeanFactory beanFactory) {  
   super.initBeanFactory(beanFactory);  
   if (this.aspectJAdvisorFactory == null) {  
      this.aspectJAdvisorFactory = new ReflectiveAspectJAdvisorFactory(beanFactory);  
   }  
   this.aspectJAdvisorsBuilder =  
         new BeanFactoryAspectJAdvisorsBuilderAdapter(beanFactory, this.aspectJAdvisorFactory);  
}
```

`advisorRetrievalHelper`可以获取容器中所有`Advisor`实现类，获取其中的切面信息。`BeanFactoryAdvisorRetrievalHelper#findAdvisorBeans()`：
```java
public List<Advisor> findAdvisorBeans() {  
   // Determine list of advisor bean names, if not cached already.  
   String[] advisorNames = this.cachedAdvisorBeanNames;  
   if (advisorNames == null) {  
      // Do not initialize FactoryBeans here: We need to leave all regular beans  
      // uninitialized to let the auto-proxy creator apply to them!      
      advisorNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(  
            this.beanFactory, Advisor.class, true, false);  
      this.cachedAdvisorBeanNames = advisorNames;  
   }  
   if (advisorNames.length == 0) {  
      return new ArrayList<>();  
   }  
  
   List<Advisor> advisors = new ArrayList<>();  
   for (String name : advisorNames) {  
      if (isEligibleBean(name)) {  
         if (this.beanFactory.isCurrentlyInCreation(name)) {  
            if (logger.isTraceEnabled()) {  
               logger.trace("Skipping currently created advisor '" + name + "'");  
            }  
         }  
         else {  
            try {  
               advisors.add(this.beanFactory.getBean(name, Advisor.class));  
            }  
            catch (BeanCreationException ex) {  
               Throwable rootCause = ex.getMostSpecificCause();  
               if (rootCause instanceof BeanCurrentlyInCreationException) {  
                  BeanCreationException bce = (BeanCreationException) rootCause;  
                  String bceBeanName = bce.getBeanName();  
                  if (bceBeanName != null && this.beanFactory.isCurrentlyInCreation(bceBeanName)) {  
                     if (logger.isTraceEnabled()) {  
                        logger.trace("Skipping advisor '" + name +  
                              "' with dependency on currently created bean: " + ex.getMessage());  
                     }  
                     // Ignore: indicates a reference back to the bean we're trying to advise.  
                     // We want to find advisors other than the currently created bean itself.                     
                     continue;  
                  }  
               }  
               throw ex;  
            }  
         }  
      }  
   }  
   return advisors;  
}
```

`aspectJAdvisorsBuilder`可以获取容器中所有标注`@Aspect`注解的`bean`，并通过`aspectJAdvisorFactory`创建成`Advisor`对象缓存起来。`BeanFactoryAspectJAdvisorsBuilder#buildAspectJAdvisors()`：
```java
public List<Advisor> buildAspectJAdvisors() {  
   List<String> aspectNames = this.aspectBeanNames;  
  
   if (aspectNames == null) {  
      synchronized (this) {  
         aspectNames = this.aspectBeanNames;  
         if (aspectNames == null) {  
            List<Advisor> advisors = new ArrayList<>();  
            aspectNames = new ArrayList<>();  
            String[] beanNames = BeanFactoryUtils.beanNamesForTypeIncludingAncestors(  
                  this.beanFactory, Object.class, true, false);  
            for (String beanName : beanNames) {  
               if (!isEligibleBean(beanName)) {  
                  continue;  
               }  
               // We must be careful not to instantiate beans eagerly as in this case they  
               // would be cached by the Spring container but would not have been weaved.               
               Class<?> beanType = this.beanFactory.getType(beanName, false);  
               if (beanType == null) {  
                  continue;  
               }  
               if (this.advisorFactory.isAspect(beanType)) {  
                  aspectNames.add(beanName);  
                  AspectMetadata amd = new AspectMetadata(beanType, beanName);  
                  if (amd.getAjType().getPerClause().getKind() == PerClauseKind.SINGLETON) {  
                     MetadataAwareAspectInstanceFactory factory =  
                           new BeanFactoryAspectInstanceFactory(this.beanFactory, beanName);  
                     List<Advisor> classAdvisors = this.advisorFactory.getAdvisors(factory);  
                     if (this.beanFactory.isSingleton(beanName)) {  
                        this.advisorsCache.put(beanName, classAdvisors);  
                     }  
                     else {  
                        this.aspectFactoryCache.put(beanName, factory);  
                     }  
                     advisors.addAll(classAdvisors);  
                  }  
                  else {  
                     // Per target or per this.  
                     if (this.beanFactory.isSingleton(beanName)) {  
                        throw new IllegalArgumentException("Bean with name '" + beanName +  
                              "' is a singleton, but aspect instantiation model is not singleton");  
                     }  
                     MetadataAwareAspectInstanceFactory factory =  
                           new PrototypeAspectInstanceFactory(this.beanFactory, beanName);  
                     this.aspectFactoryCache.put(beanName, factory);  
                     advisors.addAll(this.advisorFactory.getAdvisors(factory));  
                  }  
               }  
            }  
            this.aspectBeanNames = aspectNames;  
            return advisors;  
         }  
      }  
   }  
  
   if (aspectNames.isEmpty()) {  
      return Collections.emptyList();  
   }  
   List<Advisor> advisors = new ArrayList<>();  
   for (String aspectName : aspectNames) {  
      List<Advisor> cachedAdvisors = this.advisorsCache.get(aspectName);  
      if (cachedAdvisors != null) {  
         advisors.addAll(cachedAdvisors);  
      }  
      else {  
         MetadataAwareAspectInstanceFactory factory = this.aspectFactoryCache.get(aspectName);  
         advisors.addAll(this.advisorFactory.getAdvisors(factory));  
      }  
   }  
   return advisors;  
}
```

### 2.3.2 postProcessAfterInitialization
`AnnotationAwareAspectJAutoProxyCreator`创建后，才会创建其他`bean`对象。

由于`AnnotationAwareAspectJAutoProxyCreator`实现了`BeanPostProcessor`接口，每个`bean`实例化后，都会调用它的`postProcessAfterInitialization()`方法：
```java
public Object postProcessAfterInitialization(@Nullable Object bean, String beanName) {  
   if (bean != null) {  
      Object cacheKey = getCacheKey(bean.getClass(), beanName);  
      if (this.earlyProxyReferences.remove(cacheKey) != bean) {  
         return wrapIfNecessary(bean, beanName, cacheKey);  
      }  
   }  
   return bean;  
}
```

实际业务逻辑在`AbstractAutoProxyCreator#wrapIfNecessary()`：
```java
protected Object wrapIfNecessary(Object bean, String beanName, Object cacheKey) {  
   if (StringUtils.hasLength(beanName) && this.targetSourcedBeans.contains(beanName)) {  
      return bean;  
   }  
   if (Boolean.FALSE.equals(this.advisedBeans.get(cacheKey))) {  
      return bean;  
   }  
   if (isInfrastructureClass(bean.getClass()) || shouldSkip(bean.getClass(), beanName)) {  
      this.advisedBeans.put(cacheKey, Boolean.FALSE);  
      return bean;  
   }  
  
   // 判断指定bean是否需要进行代理，并返回对应Advisor数组
   Object[] specificInterceptors = getAdvicesAndAdvisorsForBean(bean.getClass(), beanName, null);  
   if (specificInterceptors != DO_NOT_PROXY) {  
      this.advisedBeans.put(cacheKey, Boolean.TRUE);  
      // 创建代理对象
      Object proxy = createProxy(  
            bean.getClass(), beanName, specificInterceptors, new SingletonTargetSource(bean));  
      this.proxyTypes.put(cacheKey, proxy.getClass());  
      return proxy;  
   }  
  
   this.advisedBeans.put(cacheKey, Boolean.FALSE);  
   return bean;  
}
```

在`AbstractAdvisorAutoProxyCreator#getAdvicesAndAdvisorsForBean()`方法中，它会调用`AbstractAdvisorAutoProxyCreator#findEligibleAdvisors()`方法查找指定`bean`对应的切面信息：
```java
protected List<Advisor> findEligibleAdvisors(Class<?> beanClass, String beanName) {  
   // 调用advisorRetrievalHelper和aspectJAdvisorsBuilder从容器中查找所有切面信息
   List<Advisor> candidateAdvisors = findCandidateAdvisors();  
   // 筛选出指定bean对应的切面信息
   List<Advisor> eligibleAdvisors = findAdvisorsThatCanApply(candidateAdvisors, beanClass, beanName);  
   extendAdvisors(eligibleAdvisors);  
   if (!eligibleAdvisors.isEmpty()) {  
      eligibleAdvisors = sortAdvisors(eligibleAdvisors);  
   }  
   return eligibleAdvisors;  
}
```

通过`AbstractAutoProxyCreator#createProxy()`方法，可以为`bean`对象创建AOP代理：
```java
protected Object createProxy(Class<?> beanClass, @Nullable String beanName,  
      @Nullable Object[] specificInterceptors, TargetSource targetSource) {  
  
   if (this.beanFactory instanceof ConfigurableListableBeanFactory) {  
      AutoProxyUtils.exposeTargetClass((ConfigurableListableBeanFactory) this.beanFactory, beanName, beanClass);  
   }  
  
   ProxyFactory proxyFactory = new ProxyFactory();  
   proxyFactory.copyFrom(this);  
  
   if (proxyFactory.isProxyTargetClass()) {  
      // Explicit handling of JDK proxy targets and lambdas (for introduction advice scenarios)  
      if (Proxy.isProxyClass(beanClass) || ClassUtils.isLambdaClass(beanClass)) {  
         // Must allow for introductions; can't just set interfaces to the proxy's interfaces only.  
         for (Class<?> ifc : beanClass.getInterfaces()) {  
            proxyFactory.addInterface(ifc);  
         }  
      }  
   }  
   else {  
      // No proxyTargetClass flag enforced, let's apply our default checks...  
      if (shouldProxyTargetClass(beanClass, beanName)) {  
         proxyFactory.setProxyTargetClass(true);  
      }  
      else {  
         evaluateProxyInterfaces(beanClass, proxyFactory);  
      }  
   }  
  
   Advisor[] advisors = buildAdvisors(beanName, specificInterceptors);  
   proxyFactory.addAdvisors(advisors);  
   proxyFactory.setTargetSource(targetSource);  
   customizeProxyFactory(proxyFactory);  
  
   proxyFactory.setFrozen(this.freezeProxy);  
   if (advisorsPreFiltered()) {  
      proxyFactory.setPreFiltered(true);  
   }  
  
   // Use original ClassLoader if bean class not locally loaded in overriding class loader  
   ClassLoader classLoader = getProxyClassLoader();  
   if (classLoader instanceof SmartClassLoader && classLoader != beanClass.getClassLoader()) {  
      classLoader = ((SmartClassLoader) classLoader).getOriginalClassLoader();  
   }  
   return proxyFactory.getProxy(classLoader);  
}
```

根据`@EnableAspectJ`配置和`bean`对象是否实现接口等信息，最终会根据`JdkDynamicAopProxy`或`CglibAopProxy`创建AOP代理对象。

对于`JdkDynamicAopProxy`，它的代理逻辑位于`JdkDynamicAopProxy#invoke()`方法，它会创建`Advisor`的调用链，然后按顺序进行触发：
```java
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {  
   Object oldProxy = null;  
   boolean setProxyContext = false;  
  
   TargetSource targetSource = this.advised.targetSource;  
   Object target = null;  
  
   try {  
      if (!this.equalsDefined && AopUtils.isEqualsMethod(method)) {  
         // The target does not implement the equals(Object) method itself.  
         return equals(args[0]);  
      }  
      else if (!this.hashCodeDefined && AopUtils.isHashCodeMethod(method)) {  
         // The target does not implement the hashCode() method itself.  
         return hashCode();  
      }  
      else if (method.getDeclaringClass() == DecoratingProxy.class) {  
         // There is only getDecoratedClass() declared -> dispatch to proxy config.  
         return AopProxyUtils.ultimateTargetClass(this.advised);  
      }  
      else if (!this.advised.opaque && method.getDeclaringClass().isInterface() &&  
            method.getDeclaringClass().isAssignableFrom(Advised.class)) {  
         // Service invocations on ProxyConfig with the proxy config...  
         return AopUtils.invokeJoinpointUsingReflection(this.advised, method, args);  
      }  
  
      Object retVal;  
  
      if (this.advised.exposeProxy) {  
         // Make invocation available if necessary.  
         oldProxy = AopContext.setCurrentProxy(proxy);  
         setProxyContext = true;  
      }  
  
      // Get as late as possible to minimize the time we "own" the target,  
      // in case it comes from a pool.      
      target = targetSource.getTarget();  
      Class<?> targetClass = (target != null ? target.getClass() : null);  
  
      // Get the interception chain for this method.  
      List<Object> chain = this.advised.getInterceptorsAndDynamicInterceptionAdvice(method, targetClass);  
  
      // Check whether we have any advice. If we don't, we can fall back on direct  
      // reflective invocation of the target, and avoid creating a MethodInvocation.      
      if (chain.isEmpty()) {  
         // We can skip creating a MethodInvocation: just invoke the target directly  
         // Note that the final invoker must be an InvokerInterceptor so we know it does         
         // nothing but a reflective operation on the target, and no hot swapping or fancy proxying.         
         Object[] argsToUse = AopProxyUtils.adaptArgumentsIfNecessary(method, args);  
         retVal = AopUtils.invokeJoinpointUsingReflection(target, method, argsToUse);  
      }  
      else {  
         // We need to create a method invocation...  
         MethodInvocation invocation =  
               new ReflectiveMethodInvocation(proxy, target, method, args, targetClass, chain);  
         // Proceed to the joinpoint through the interceptor chain.  
         retVal = invocation.proceed();  
      }  
  
      // Massage return value if necessary.  
      Class<?> returnType = method.getReturnType();  
      if (retVal != null && retVal == target &&  
            returnType != Object.class && returnType.isInstance(proxy) &&  
            !RawTargetAccess.class.isAssignableFrom(method.getDeclaringClass())) {  
         // Special case: it returned "this" and the return type of the method  
         // is type-compatible. Note that we can't help if the target sets         
         // a reference to itself in another returned object.         
         retVal = proxy;  
      }  
      else if (retVal == null && returnType != Void.TYPE && returnType.isPrimitive()) {  
         throw new AopInvocationException(  
               "Null return value from advice does not match primitive return type for: " + method);  
      }  
      return retVal;  
   }  
   finally {  
      if (target != null && !targetSource.isStatic()) {  
         // Must have come from TargetSource.  
         targetSource.releaseTarget(target);  
      }  
      if (setProxyContext) {  
         // Restore old proxy.  
         AopContext.setCurrentProxy(oldProxy);  
      }  
   }  
}
```

# 3 小结
以上介绍了Spring AOP的基本使用和工作原理，更多细节可以查看相关源码。

Spring AOP是对IoC基础扩展点使用的典型案例。

它基于`ApplicationContext`实现类的基础功能`ConfigurationClassPostProcessor`，通过`@Import`注解注册自定义的`AnnotationAwareAspectJAutoProxyCreator`。

由于`AnnotationAwareAspectJAutoProxyCreator`是`BeanPostProcessor`，可以创建并缓存所有切面`bean`，并且在业务`bean`实例化后动态决定是否选择是否进行AOP代理。