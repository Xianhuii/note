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

接下来对每个步骤进行深入讲解。

## 2.1 创建AnnotatedGenericBeanDefinition
![[AnnotatedGenericBeanDefinition.png]]
`AnnotatedGenericBeanDefinition`是`GenericBeanDefinition`的子类。

除了`GenericBeanDefinition`和`AbstractBeanDefinition`中定义的关于`bean`的基本信息，`AnnotatedGenericBeanDefinition`额外存储了类的注解信息：
![[AnnotatedGenericBeanDefinition 1.png]]

在`AnnotatedGenericBeanDefinition`的构造函数中，除了会设置`beanClass`属性，还会获取类对象中的注解信息：
```java
public AnnotatedGenericBeanDefinition(Class<?> beanClass) {  
   setBeanClass(beanClass);  
   this.metadata = AnnotationMetadata.introspect(beanClass);  
}
```

沿着构造函数继续深入，可以发现会在`AnnotationsScanner#getDeclaredAnnotations()`获取注解信息：
```java
static Annotation[] getDeclaredAnnotations(AnnotatedElement source, boolean defensive) {  
   boolean cached = false;  
   Annotation[] annotations = declaredAnnotationCache.get(source);  
   if (annotations != null) {  
      cached = true;  
   }  
   else {  
      annotations = source.getDeclaredAnnotations();  
      if (annotations.length != 0) {  
         boolean allIgnored = true;  
         for (int i = 0; i < annotations.length; i++) {  
            Annotation annotation = annotations[i];  
            if (isIgnorable(annotation.annotationType()) ||  
                  !AttributeMethods.forAnnotationType(annotation.annotationType()).isValid(annotation)) {  
               annotations[i] = null;  
            }  
            else {  
               allIgnored = false;  
            }  
         }  
         annotations = (allIgnored ? NO_ANNOTATIONS : annotations);  
         if (source instanceof Class || source instanceof Member) {  
            declaredAnnotationCache.put(source, annotations);  
            cached = true;  
         }  
      }  
   }  
   if (!defensive || annotations.length == 0 || !cached) {  
      return annotations;  
   }  
   return annotations.clone();  
}
```

例如，我们在`ComponentA`中添加注解：
```java
@Profile("default")  
@Lazy  
@Component  
@Configuration  
@Controller  
public class ComponentA {  
}
```

通过调用构造函数，就可以从`ComponentA`中的注解信息：
```java
AnnotatedBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(ComponentA.class);  
System.out.println(beanDefinition.getMetadata().getAnnotationTypes());
```

输出结果如下：
```
[org.springframework.context.annotation.Profile, org.springframework.context.annotation.Lazy, org.springframework.stereotype.Component, org.springframework.context.annotation.Configuration, org.springframework.stereotype.Controller]
```

## 2.2 @Conditional校验
获取基本注解信息后，首先会根据`@Conditional`注解信息判断是否需要注册这个类对象：
```java
if (this.conditionEvaluator.shouldSkip(abd.getMetadata())) {  
   return;  
}
```

校验的核心源码位于`ConditionEvaluator#shouldSkip()`方法：
```java
public boolean shouldSkip(@Nullable AnnotatedTypeMetadata metadata, 
						  @Nullable ConfigurationPhase phase) {  
   // 没有标注@Condition，直接返回
   if (metadata == null || !metadata.isAnnotated(Conditional.class.getName())) {  
      return false;  
   }  
  
   if (phase == null) {  
      // 配置类：类标注@Component/@ComponentScan/@Import/@ImportResource，或方法标注@Bean
      if (metadata instanceof AnnotationMetadata &&  
            ConfigurationClassUtils.isConfigurationCandidate((AnnotationMetadata) metadata)) {  
         return shouldSkip(metadata, ConfigurationPhase.PARSE_CONFIGURATION);  
      }  
      // 普通类
      return shouldSkip(metadata, ConfigurationPhase.REGISTER_BEAN);  
   }  
   // 获取所有@Conditional注解对应的Condition实现类
   List<Condition> conditions = new ArrayList<>();  
   for (String[] conditionClasses : getConditionClasses(metadata)) {  
      for (String conditionClass : conditionClasses) {  
         Condition condition = getCondition(conditionClass, this.context.getClassLoader());  
         conditions.add(condition);  
      }  
   }  
   // 对Condition实现类排序
   AnnotationAwareOrderComparator.sort(conditions);  
   // 遍历Condition实现类，调用其matches()方法进行校验
   for (Condition condition : conditions) {  
      ConfigurationPhase requiredPhase = null;  
      if (condition instanceof ConfigurationCondition) {  
         requiredPhase = ((ConfigurationCondition) condition).getConfigurationPhase();  
      }  
      if ((requiredPhase == null || requiredPhase == phase) && !condition.matches(this.context, metadata)) {  
         return true;  
      }  
   }  
   return false;  
}
```

总的来说，`AnnotatedBeanDefinitionReader`会根据标注的`@Conditional`进行校验，具体校验逻辑位于对应`Condition`实现类中。

例如`@Profile`的对应校验实现类为`ProfileCondition`，校验逻辑为：
```java
public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {  
   // 获取@Profile属性
   MultiValueMap<String, Object> attrs = metadata.getAllAnnotationAttributes(Profile.class.getName());  
   if (attrs != null) {  
      // 遍历@Profile属性值
      for (Object value : attrs.get("value")) {  
         // 从environment中获取运行时环境变量进行匹配
         if (context.getEnvironment().acceptsProfiles(Profiles.of((String[]) value))) {  
            return true;  
         }  
      }  
      return false;  
   }  
   return true;  
}
```

在Spring Boot项目中常见的`@ConditionalOnBean`对应的校验实现类为`OnBeanCondition`，它的校验逻辑就比较复杂。简单来说，它会判断指定类是否已经加载：
```java
static boolean isPresent(String className, ClassLoader classLoader) {  
   if (classLoader == null) {  
      classLoader = ClassUtils.getDefaultClassLoader();  
   }  
   try {  
      resolve(className, classLoader);  
      return true;  
   }  
   catch (Throwable ex) {  
      return false;  
   }  
}
```

## 2.3 添加instanceSupplier回调
