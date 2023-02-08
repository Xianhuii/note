Spring事务管理Java后端项目开发中都会用到的一个技术点，每个Java工程师都必须精通。

Spring事务管理可以分为两类：
- 声明式事务管理
- 编程式事务管理

声明式事务管理只需要在代码中添加`@Transactional`注解，即可自动进行事务管理。由于使用方便，是项目开发中的首选。

在Spring Boot中，只要我们引入了相关依赖，就会自动开启声明式事务功能。

例如，我们引入`mybatis-spring-boot-starter`，它会自动引入`spring-tx`等相关依赖，并且自动开启事务功能：
```java
<dependency>  
   <groupId>org.mybatis.spring.boot</groupId>  
   <artifactId>mybatis-spring-boot-starter</artifactId>  
</dependency>
```

![[TransactionAutoConfiguration.png]]
Spring Boot的声明式事务的工作原理如上图，总的来说经过三个步骤：
1. 以`TransactionAutoConfiguration`为核心，基于Spring Boot自动配置机制，创建事务相关bean。对于声明式事务来说，它会开启`@EnableTransactionManagement`注解。
2. `@EnableTransactionManager`会使用@Import注解引入TransactionManagementConfigurationSelector，然后开启基于代理或Aspectj的声明式事务功能。
3. 对于基于代理的声明式事务，`ProxyTransactionManagementConfiguration`会创建基于`@Transactional`注解的`Advisor`。在bean实例化过程中，AOP功能会根据该`Advisor`对相关bean进行代理。

# 1 自动配置原理
## 1.1 TransactionAutoConfiguration
`TransactionAutoConfiguration`会对事务功能进行自动配置，与声明式事务相关的源码如下：
```java
@AutoConfiguration  
@ConditionalOnClass(PlatformTransactionManager.class)  
@EnableConfigurationProperties(TransactionProperties.class)  
public class TransactionAutoConfiguration {  
   @Configuration(proxyBeanMethods = false)  
   @ConditionalOnBean(TransactionManager.class)  
   @ConditionalOnMissingBean(AbstractTransactionManagementConfiguration.class)  
   public static class EnableTransactionManagementConfiguration {  
  
      @Configuration(proxyBeanMethods = false)  
      @EnableTransactionManagement(proxyTargetClass = false)  
      @ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "false")  
      public static class JdkDynamicAutoProxyConfiguration {  
      }  
  
      @Configuration(proxyBeanMethods = false)  
      @EnableTransactionManagement(proxyTargetClass = true)  
      @ConditionalOnProperty(prefix = "spring.aop", name = "proxy-target-class", havingValue = "true",  
            matchIfMissing = true)  
      public static class CglibAutoProxyConfiguration {  
      }  
   }  
}
```

首先，`TransactionAutoConfiguration`标注了三个注解：
- `@AutoConfiguration`：表示`TransactionAutoConfiguration`可以作为配置类被Spring Boot自动配置。
- `@ConditionalOnClass(PlatformTransactionManager.class)`：表示只有存在`PlatformTransactionManager`类时，Spring容器才会注册`TransactionAutoConfiguration`。
- `@EnableConfigurationProperties(TransactionProperties.class)`：表示读取配置文件中的`spring.transaction`属性。

然后，根据配置文件中的`spring.aop`属性，会分别引入JdkDynamicAutoProxyConfiguration或CglibAutoProxyConfiguration（默认）配置类。

这两个配置类都是空的，它们的作用其实是标注`@EnableTransactionManagement`注解。

## 1.2 @EnableTransactionManagement
`@EnableTransactionManagement`注解表示开启声明式事务管理功能：
```java
@Target(ElementType.TYPE)  
@Retention(RetentionPolicy.RUNTIME)  
@Documented  
@Import(TransactionManagementConfigurationSelector.class)  
public @interface EnableTransactionManagement {  
   boolean proxyTargetClass() default false;  
   AdviceMode mode() default AdviceMode.PROXY;  
   int order() default Ordered.LOWEST_PRECEDENCE;  
}
```

它的作用是通过`@Import`注解引入`TransactionManagementConfigurationSelector`，然后根据`AdviceMode`分别开启基于`proxy`或`aspectj`的声明式事务管理功能（注册对应的配置类）：
```java
public class TransactionManagementConfigurationSelector extends AdviceModeImportSelector<EnableTransactionManagement> {  
   protected String[] selectImports(AdviceMode adviceMode) {  
      // 根据adviceMode开启声明式事务管理功能
      switch (adviceMode) {  
         case PROXY:  
            return new String[] {AutoProxyRegistrar.class.getName(),  
                  ProxyTransactionManagementConfiguration.class.getName()};  
         case ASPECTJ:  
            return new String[] {determineTransactionAspectClass()};  
         default:  
            return null;  
      }  
   }  
  
   private String determineTransactionAspectClass() {  
      return (ClassUtils.isPresent("javax.transaction.Transactional", getClass().getClassLoader()) ?  
            TransactionManagementConfigUtils.JTA_TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME :  
            TransactionManagementConfigUtils.TRANSACTION_ASPECT_CONFIGURATION_CLASS_NAME);  
   }  
  
}
```

## 1.3 ProxyTransactionManagementConfiguration
`ProxyTransactionManagementConfiguration`的核心作用就是创建声明式事务管理的`Advisor`，AOP会根据它对bean进行动态代理：
```java
@Configuration(proxyBeanMethods = false)  
@Role(BeanDefinition.ROLE_INFRASTRUCTURE)  
public class ProxyTransactionManagementConfiguration extends AbstractTransactionManagementConfiguration {  
   // 注册Advisor
   @Bean(name = TransactionManagementConfigUtils.TRANSACTION_ADVISOR_BEAN_NAME)  
   @Role(BeanDefinition.ROLE_INFRASTRUCTURE)  
   public BeanFactoryTransactionAttributeSourceAdvisor transactionAdvisor(  
         TransactionAttributeSource transactionAttributeSource, TransactionInterceptor transactionInterceptor) {  
      BeanFactoryTransactionAttributeSourceAdvisor advisor = new BeanFactoryTransactionAttributeSourceAdvisor();  
      advisor.setTransactionAttributeSource(transactionAttributeSource);  
      advisor.setAdvice(transactionInterceptor);  
      if (this.enableTx != null) {  
         advisor.setOrder(this.enableTx.<Integer>getNumber("order"));  
      }  
      return advisor;  
   }  
  
   // 注册@Transactional的pointcut解析数据源
   @Bean  
   @Role(BeanDefinition.ROLE_INFRASTRUCTURE)  
   public TransactionAttributeSource transactionAttributeSource() {  
      return new AnnotationTransactionAttributeSource();  
   }  
  
   // 注册拦截器
   @Bean  
   @Role(BeanDefinition.ROLE_INFRASTRUCTURE)  
   public TransactionInterceptor transactionInterceptor(TransactionAttributeSource transactionAttributeSource) {  
      TransactionInterceptor interceptor = new TransactionInterceptor();  
      interceptor.setTransactionAttributeSource(transactionAttributeSource);  
      if (this.txManager != null) {  
         interceptor.setTransactionManager(this.txManager);  
      }  
      return interceptor;  
   }  
}
```

### 1.3.1 @Transactional解析器
`AnnotationTransactionAttributeSource`中定义了对`@Transactional`等注解的解析规则：
```java
public AnnotationTransactionAttributeSource(boolean publicMethodsOnly) {  
   this.publicMethodsOnly = publicMethodsOnly;  
   if (jta12Present || ejb3Present) {  
      this.annotationParsers = new LinkedHashSet<>(4);  
      this.annotationParsers.add(new SpringTransactionAnnotationParser());  
      if (jta12Present) {  
         this.annotationParsers.add(new JtaTransactionAnnotationParser());  
      }  
      if (ejb3Present) {  
         this.annotationParsers.add(new Ejb3TransactionAnnotationParser());  
      }  
   }  
   else {  
      this.annotationParsers = Collections.singleton(new SpringTransactionAnnotationParser());  
   }  
}
```

例如，`SpringTransactionAnnotationParser`可以判断bean是否标注`@Transactional`注解：
```java
public boolean isCandidateClass(Class<?> targetClass) {  
   return AnnotationUtils.isCandidateClass(targetClass, Transactional.class);  
}
```

也可以获取`@Transactional`的属性信息：
```java
protected TransactionAttribute parseTransactionAnnotation(AnnotationAttributes attributes) {  
   RuleBasedTransactionAttribute rbta = new RuleBasedTransactionAttribute();  
  
   Propagation propagation = attributes.getEnum("propagation");  
   rbta.setPropagationBehavior(propagation.value());  
   Isolation isolation = attributes.getEnum("isolation");  
   rbta.setIsolationLevel(isolation.value());  
  
   rbta.setTimeout(attributes.getNumber("timeout").intValue());  
   String timeoutString = attributes.getString("timeoutString");  
   Assert.isTrue(!StringUtils.hasText(timeoutString) || rbta.getTimeout() < 0,  
         "Specify 'timeout' or 'timeoutString', not both");  
   rbta.setTimeoutString(timeoutString);  
  
   rbta.setReadOnly(attributes.getBoolean("readOnly"));  
   rbta.setQualifier(attributes.getString("value"));  
   rbta.setLabels(Arrays.asList(attributes.getStringArray("label")));  
  
   List<RollbackRuleAttribute> rollbackRules = new ArrayList<>();  
   for (Class<?> rbRule : attributes.getClassArray("rollbackFor")) {  
      rollbackRules.add(new RollbackRuleAttribute(rbRule));  
   }  
   for (String rbRule : attributes.getStringArray("rollbackForClassName")) {  
      rollbackRules.add(new RollbackRuleAttribute(rbRule));  
   }  
   for (Class<?> rbRule : attributes.getClassArray("noRollbackFor")) {  
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));  
   }  
   for (String rbRule : attributes.getStringArray("noRollbackForClassName")) {  
      rollbackRules.add(new NoRollbackRuleAttribute(rbRule));  
   }  
   rbta.setRollbackRules(rollbackRules);  
  
   return rbta;  
}
```

### 1.3.2 事务方法拦截器
`TransactionInterceptor`中定义了事务方法的执行逻辑，`TransactionInterceptor#invoke()`：
```java
public Object invoke(MethodInvocation invocation) throws Throwable {  
   // Work out the target class: may be {@code null}.  
   // The TransactionAttributeSource should be passed the target class   
   // as well as the method, which may be from an interface.   
   Class<?> targetClass = (invocation.getThis() != null ? AopUtils.getTargetClass(invocation.getThis()) : null);  
  
   // Adapt to TransactionAspectSupport's invokeWithinTransaction...  
   return invokeWithinTransaction(invocation.getMethod(), targetClass, new CoroutinesInvocationCallback() {  
      @Override  
      @Nullable      
      public Object proceedWithInvocation() throws Throwable {  
         return invocation.proceed();  
      }  
      @Override  
      public Object getTarget() {  
         return invocation.getThis();  
      }  
      @Override  
      public Object[] getArguments() {  
         return invocation.getArguments();  
      }  
   });  
}
```

`TransactionAspectSupport#invokeWithinTransaction()`中会根据`@Transactional`注解信息执行创建事务/加入事务，执行业务方法，提交事务/回滚等流程：
```java
protected Object invokeWithinTransaction(Method method, @Nullable Class<?> targetClass,  
      final InvocationCallback invocation) throws Throwable {  
  
   // 获取事务属性信息，例如@Transactional注解的属性  
   TransactionAttributeSource tas = getTransactionAttributeSource();  
   final TransactionAttribute txAttr = (tas != null ? tas.getTransactionAttribute(method, targetClass) : null);  
   
   // 获取事务管理器
   final TransactionManager tm = determineTransactionManager(txAttr);  
  
   // ReactiveTransactionManager事务管理器执行流程（一般跳过）
   if (this.reactiveAdapterRegistry != null && tm instanceof ReactiveTransactionManager) {  
      // 省略……
   }  
  
   // PlatformTransactionManager事务管理器执行流程
   PlatformTransactionManager ptm = asPlatformTransactionManager(tm);  
   final String joinpointIdentification = methodIdentification(method, targetClass, txAttr);  
  
   // DataSourceTransactionManager或JtaTransactionManager等事务管理器执行流程
   if (txAttr == null || !(ptm instanceof CallbackPreferringPlatformTransactionManager)) {  
      // 获取事务信息：根据@Transactional注解信息创建事务/加入事务等 
      TransactionInfo txInfo = createTransactionIfNecessary(ptm, txAttr, joinpointIdentification);  
  
      Object retVal;  
      try {  
         // 执行业务方法
         retVal = invocation.proceedWithInvocation();  
      }  
      catch (Throwable ex) {  
         // 业务方法抛异常，根据@Transactional注解信息进行回滚或提交
         completeTransactionAfterThrowing(txInfo, ex);  
         throw ex;  
      }  
      finally {  
         // 重置当前线程的事务信息
         cleanupTransactionInfo(txInfo);  
      }  
  
      if (retVal != null && vavrPresent && VavrDelegate.isVavrTry(retVal)) {  
         // Set rollback-only in case of Vavr failure matching our rollback rules...  
         TransactionStatus status = txInfo.getTransactionStatus();  
         if (status != null && txAttr != null) {  
            retVal = VavrDelegate.evaluateTryFailure(retVal, txAttr, status);  
         }  
      }  
  
      // 如果当前事务仍存在，提交事务
      commitTransactionAfterReturning(txInfo);  
      return retVal;  
   }  
  
   // CallbackPreferringPlatformTransactionManager（WebSphereUowTransactionManager）事务管理器执行流程（一般跳过）
   else {  
      // 省略……
   }  
}
```

### 1.3.3 BeanFactoryTransactionAttributeSourceAdvisor
`BeanFactoryTransactionAttributeSourceAdvisor`定义了声明式事务管理的切面信息，包括