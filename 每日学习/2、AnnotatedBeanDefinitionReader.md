![[AnnotatedBeanDefinitionReader 2.png]]
`AnnotatedBeanDefinitionReader`是干什么的？
1. 从类对象中获取基本注解信息，创建`BeanDefinition`对象。
2. 将`BeanDefinition`对象注册到`BeanDefinitionRegistry`对象中。

由于`BeanFactory`和`ApplicationContext`的实现类基本上都会实现`BeanDefinitionRegistry`接口。所以`AnnotatedBeanDefinitionReader`实际上会将`BeanDefinition`对象注册到Spring容器中。

# 1 基本使用
举个简单的例子。

现在有`ComponentA`：
```java
public class ComponentA {  
}
```

需要将`ComponentA`注册到Spring容器中：
```java
// 创建registry  
BeanDefinitionRegistry registry = new DefaultListableBeanFactory();  
// 创建reader，指定registry  
AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);  
// 注册指定类作为bean  
reader.registerBean(ComponentA.class);  
// 打印ComponentA的BeanDefinition信息  
System.out.println(registry.getBeanDefinition("componentA"));
```

输出结果如下：
```text
Generic bean: class [ComponentA]; scope=singleton; abstract=false; lazyInit=null; autowireMode=0; dependencyCheck=0; autowireCandidate=true; primary=false; factoryBeanName=null; factoryMethodName=null; initMethodName=null; destroyMethodName=null
```

可以看到，`AnnotatedBeanDefinition`起着一个中间层的作用，它一方面读取类对象的信息，另一方面将这些信息添加到Spring容器中。

# 2 源码解读
`AnnotatedBeanDefinition`的核心步骤在`AnnotatedBeanDefinitionReader#registerBean()`方法。

`registerBean()`方法有很多重载的实现，最终都会调用`AnnotatedBeanDefinitionReader#doRegisterBean()`方法执行具体业务。

因此，`AnnotatedBeanDefinition`的核心在于`doRegisterBean()`方法，我们先大概看一下它的实现：
```java
private <T> void doRegisterBean(
      Class<T> beanClass,
      @Nullable String name, 
      @Nullable Class<? extends Annotation>[] qualifiers, 
      @Nullable Supplier<T> supplier,  
      @Nullable BeanDefinitionCustomizer[] customizers) {  
   // 根据给定类对象，创建BeanDefinition基本信息
   AnnotatedGenericBeanDefinition abd = new AnnotatedGenericBeanDefinition(beanClass);  
   // 根据@Conditional判断是否应该跳过
   if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {  
      return;  
   }  
   // 指定创建bean的回调/工厂方法
   abd.setInstanceSupplier(supplier);  
   // 指定作用域
   ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);  
   abd.setScope(scopeMetadata.getScopeName());  
   // 指定bean的名字
   String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));  
   // 指定类对象标注通用的注解：@Lazy、@Primary、@DependsOn、@Role、@Description
   AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);  
   // 指定传参的注解：@Primary、@Lazy等
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
   // BeanDefinition自定义处理拦截
   if (customizers != null) {  
      for (BeanDefinitionCustomizer customizer : customizers) {  
         customizer.customize(abd);  
      }  
   }  
  
   BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);  
   // 判断并创建代理
   definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);  
   // 注册到Spring容器
   BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);  
}
```

## 2.1 创建AnnotatedGenericBeanDefinition
![[AnnotatedGenericBeanDefinition.png]]
`AnnotatedGenericBeanDefinition`是`GenericBeanDefinition`的子类。