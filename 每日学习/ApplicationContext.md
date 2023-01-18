![[ApplicationContext 1.png]]

# 1 AnnotationConfigApplicationContext
![[AnnotationConfigApplicationContext 1.png]]



`AnnotationConfigApplicationContext`的基本使用：
```java
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();  
context.register(AppConfig.class);  
context.scan("applicationcontext");  
context.refresh();  
AppConfig bean = context.getBean(AppConfig.class);  
System.out.println(bean);
```

## 1.1 构造函数
使用默认无参构造函数，会初始化`AnnotatedBeanDefinitionReader`和`ClassPathBeanDefinitionScanner`对象，用于读取依赖配置：
```java
public AnnotationConfigApplicationContext() {  
   this.reader = new AnnotatedBeanDefinitionReader(this);  
   this.scanner = new ClassPathBeanDefinitionScanner(this);  
}
```

在父类`GenericApplicationContext`的默认无参构造函数中，会初始化`beanFactory`：
```java
public GenericApplicationContext() {  
   this.beanFactory = new DefaultListableBeanFactory();  
}
```

在父类`AbstractApplicationContext`的默认无参构造函数中，会初始化`resourcePatternResolver`：
```java
public AbstractApplicationContext() {  
   this.resourcePatternResolver = getResourcePatternResolver();  
}
```

## 1.2 register和scan
`AnnotationConfigApplicationContext#register()`会调用内部的`AnnotatedBeanDefinitionReader`对象的`register()`方法，读取指定类对象，封装成`BeanDefinition`，然后注册到`BeanFactory`中：
```java
public void register(Class<?>... componentClasses) { 
   this.reader.register(componentClasses);  
}
```

`AnnotationConfigApplicationContext#scan()`会调用内部的`ClassPathBeanDefinitionScanner`对象的`scan()`方法，扫描指定路径下的`@Component`类，封装成`BeanDefinition`，然后注册到`BeanFactory`中：
```java
public void scan(String... basePackages) {
   this.scanner.scan(basePackages);
}
```

同`DefaultListableBeanFacoty`一样，`GenericApplicationContext`也实现了`BeanDefinitionRegistry`接口，它会将`BeanDefinition`注册到内部持有的`beanFactory`（`DefaultListableBeanFactory`对象）中：
```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)  
      throws BeanDefinitionStoreException {  
   this.beanFactory.registerBeanDefinition(beanName, beanDefinition);  
}
```

## 1.3 refresh
`AbstractApplicationContext#refresh()`方法会对容器进行一个初始化操作：
1. 初始化标准上下文的基础配置：`BeanFactoryPostProcessor`等。
2. 注册容器特定的`BeanFactoryPostProcessor`。
3. 触发`BeanFactoryPostProcessor`的回调。
4. 注册`BeanPostProcessor`。
5. 初始化`MessageSource`。
6. 初始化`ApplicationEventMulticaster`。
7. 初始化容器特定的`bean`。
8. 注册`listeners`。
9. 实例化所有单例`bean`(non-lazy-init) 。
10. 清除`context-level`资源缓存。
11. 初始化`LifecycleProcessor`。
12. 触发`LifecycleProcessor#onRefresh()`方法。
13. 发布`ContextRefreshedEvent`事件。
14. 清除占用的资源。

`AbstractApplicationContext#refresh()`：
```java
public void refresh() throws BeansException, IllegalStateException {  
   synchronized (this.startupShutdownMonitor) {
      // Prepare this context for refreshing.  
      prepareRefresh();  
  
      // Tell the subclass to refresh the internal bean factory.  
      ConfigurableListableBeanFactory beanFactory = obtainFreshBeanFactory();  
  
      /*
      * 初始化标准applicationContext的基础配置：
      * 1、上下文的ClassLoader
      * 2、post-processor：ApplicationContextAwareProcessor、ApplicationListenerDetector等
      * 3、注册单例对象：environment、systemProperties、systemEnvironment、applicationStartup
      */
      prepareBeanFactory(beanFactory);  
  
      try {  
         // 不同子类会添加特定的BeanFactoryPostProcessor
         postProcessBeanFactory(beanFactory);  

         // 触发BeanFactoryPostProcessor的回调方法
         invokeBeanFactoryPostProcessors(beanFactory);  
  
         // 注册BeanPostProcessor
         registerBeanPostProcessors(beanFactory);  
  
         // 初始化MessageSource
         initMessageSource();  
  
         // 初始化ApplicationEventMulticaster
         initApplicationEventMulticaster();  
  
         // 不同子类会初始化特定的bean
         onRefresh();  
  
         // 注册ApplicationListener类型的bean为listeners
         registerListeners();  
  
         // 实例化所有单例bean (non-lazy-init) 
         finishBeanFactoryInitialization(beanFactory);  
  
         /*
         * 1、清除context-level资源缓存
         * 2、初始化LifecycleProcessor
         * 3、触发LifecycleProcessor的onRefresh()方法
         * 4、发布ContextRefreshedEvent事件
         */
         finishRefresh();  
      }  
  
      catch (BeansException ex) {  
         // 销毁已创建的所有单例对象，避免资源浪费
         destroyBeans();  
  
         // Reset 'active' flag.  
         cancelRefresh(ex);  
  
         // Propagate exception to caller.  
         throw ex;  
      }  
  
      finally {  
         // Reset common introspection caches in Spring's core, since we  
         // might not ever need metadata for singleton beans anymore...         
         resetCommonCaches();  
      }  
   }  
}
```
# 2 AnnotationConfigServletWebServerApplicationContext
