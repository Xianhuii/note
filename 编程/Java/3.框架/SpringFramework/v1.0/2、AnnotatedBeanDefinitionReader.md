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
   // 指定创建bean的方法
   abd.setInstanceSupplier(supplier);  
   // 指定作用域
   ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);  
   abd.setScope(scopeMetadata.getScopeName());  
   // 生成beanName
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
   // 判断并创建作用域代理
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
`BeanDefinition`的`instanceSupplier`属性通常作为创建`bean`对象的特殊回调。

在`AbstractAutowireCapableBeanFactory#createBeanInstance()`方法中，会根据`BeanDefinition`中是否有`instanceSupplier`信息来执行回调：
```java
protected BeanWrapper createBeanInstance(String beanName, 
										 RootBeanDefinition mbd, 
										 @Nullable Object[] args) {  
   // 省略……
   Supplier<?> instanceSupplier = mbd.getInstanceSupplier();  
   if (instanceSupplier != null) {  
      return obtainFromSupplier(instanceSupplier, beanName);  
   }  
   // 省略……
}
```

内部`AbstractAutowireCapableBeanFactory#obtainFromSupplier()`方法如下：
```java
protected BeanWrapper obtainFromSupplier(Supplier<?> instanceSupplier, String beanName) {  
   Object instance;  
  
   String outerBean = this.currentlyCreatedBean.get();  
   this.currentlyCreatedBean.set(beanName);  
   try {  
      // 调用回调方法
      instance = instanceSupplier.get();  
   }  
   finally {  
      if (outerBean != null) {  
         this.currentlyCreatedBean.set(outerBean);  
      }  
      else {  
         this.currentlyCreatedBean.remove();  
      }  
   }  
  
   if (instance == null) {  
      instance = new NullBean();  
   }  
   BeanWrapper bw = new BeanWrapperImpl(instance);  
   initBeanWrapper(bw);  
   return bw;  
}
```

由于`AbstractAutowireCapableBeanFactory`是`DefaultListableBeanFactory`的父类，`DefaultListableBeanFactory`又是`ApplicationContext`的代理类，所有这个规则基本对于所有Spring容器都成立。

最后，我们举个代码例子。我们有`ComponentB`：
```java
public class ComponentB {  
    private String name;  
  
    public String getName() {  
        return name;  
    }  
  
    public void setName(String name) {  
        this.name = name;  
    }  
}
```

在`registerBean()`方法指定回调方法：
```java
// 创建registry  
BeanDefinitionRegistry registry = new DefaultListableBeanFactory();  
// 创建reader，指定registry  
AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);  
// 注册指定类作为bean，指定创建bean的方法  
reader.registerBean(ComponentB.class, () -> {  
    ComponentB componentB = new ComponentB();  
    componentB.setName("instanceSupplier");  
    return componentB;  
});  
// 打印ComponentB的信息  
ComponentB componentB = ((BeanFactory) registry).getBean(ComponentB.class);  
System.out.println(componentB.getName());
```

输出结果如下：
```
instanceSupplier
```

## 2.4 设置作用域
Spring作用域表示`bean`的存活周期。

例如，`singleton`表示`bean`在容器中只会创建一次，多次调用`getBean()`方法得到的都是同一个对象；`prototype`表示`bean`在容器中会创建多次，每次调用`getBean()`方法得到的都是新的对象。

`AnnotatedBeanDefinitionReader`会从`@Scope`中获取作用域，然后设置到`Beandefinition`的`scope`属性中：
```java
ScopeMetadata scopeMetadata = this.scopeMetadataResolver.resolveScopeMetadata(abd);  
abd.setScope(scopeMetadata.getScopeName());
```

`AnnotatedBeanDefinitionReader`会调用`AnnotationScopeMetadataResolver#resolveScopeMetadata()`方法获取作用域信息：
```java
public ScopeMetadata resolveScopeMetadata(BeanDefinition definition) {  
   ScopeMetadata metadata = new ScopeMetadata();  
   if (definition instanceof AnnotatedBeanDefinition) {  
      AnnotatedBeanDefinition annDef = (AnnotatedBeanDefinition) definition;  
      // 获取@Scope注解信息
      AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(  
            annDef.getMetadata(), this.scopeAnnotationType);  
      if (attributes != null) {  
         metadata.setScopeName(attributes.getString("value"));  
         ScopedProxyMode proxyMode = attributes.getEnum("proxyMode");  
         if (proxyMode == ScopedProxyMode.DEFAULT) {  
            proxyMode = this.defaultProxyMode;  
         }  
         metadata.setScopedProxyMode(proxyMode);  
      }  
   }  
   return metadata;  
}
```

如果没有添加`@Scope`注解，那么返回的是`scopeName`默认值`singleton`。

## 2.5 生成beanName
接下来会生成`beanName`。如果指定名字，则用对应形参值；如果没有指定名字，会生成名字：
```java
String beanName = (name != null ? name : this.beanNameGenerator.generateBeanName(abd, this.registry));
```

`AnnotatedBeanDefinition`会调用`AnnotationBeanNameGenerator#generateBeanName()`方法生成`beanName`：
```java
public String generateBeanName(BeanDefinition definition, BeanDefinitionRegistry registry) {  
   if (definition instanceof AnnotatedBeanDefinition) {  
      // 根据注解生成beanName
      String beanName = determineBeanNameFromAnnotation((AnnotatedBeanDefinition) definition);  
      if (StringUtils.hasText(beanName)) {  
         // Explicit bean name found.  
         return beanName;  
      }  
   }  
   // 根据默认规则生成beanName 
   return buildDefaultBeanName(definition, registry);  
}
```

`AnnotationBeanNameGenerator#determineBeanNameFromAnnotation()`方法会获取`@Component`及其子注解、`@ManagedBean`和`@Named`注解的`value`值作为`beanName`：
```java
protected String determineBeanNameFromAnnotation(AnnotatedBeanDefinition annotatedDef) {  
   // 获取注解类型
   AnnotationMetadata amd = annotatedDef.getMetadata();  
   Set<String> types = amd.getAnnotationTypes();  
   // 遍历注解类型，设置beanName
   String beanName = null;  
   for (String type : types) {  
      // 获取注解信息
      AnnotationAttributes attributes = AnnotationConfigUtils.attributesFor(amd, type);  
      if (attributes != null) {  
         // 获取元注解信息
         Set<String> metaTypes = this.metaAnnotationTypesCache.computeIfAbsent(type, key -> {  
            Set<String> result = amd.getMetaAnnotationTypes(key);  
            return (result.isEmpty() ? Collections.emptySet() : result);  
         });  
         // 校验元注解是否是@Component、注解是否是@Component、@ManagedBean或@Named
         if (isStereotypeWithNameValue(type, metaTypes, attributes)) {  
            // 获取value属性作为beanName
            Object value = attributes.get("value");  
            if (value instanceof String) {  
               String strVal = (String) value;  
               if (StringUtils.hasLength(strVal)) {  
                  if (beanName != null && !strVal.equals(beanName)) {  
                     throw new IllegalStateException("Stereotype annotations suggest inconsistent " +  
                           "component names: '" + beanName + "' versus '" + strVal + "'");  
                  }  
                  beanName = strVal;  
               }  
            }  
         }  
      }  
   }  
   return beanName;  
}
```

`AnnotationBeanNameGenerator#buildDefaultBeanName()`方法会根据类名生成`beanName`：
```java
protected String buildDefaultBeanName(BeanDefinition definition) {  
   // 获取全限定类名
   String beanClassName = definition.getBeanClassName();  
   Assert.state(beanClassName != null, "No bean class name set");  
   // 获取类名：根据包名分隔符（.)、CGLIB代理类分隔符（$$）和内部类分隔符（$）进行字符串处理
   String shortClassName = ClassUtils.getShortName(beanClassName);  
   // 类名处理：如果长度大于1且第一二个字母都是大写（或类名为空），直接返回，否则首字母小写返回
   return Introspector.decapitalize(shortClassName);  
}
```

## 2.6 通用注解处理
接下来会对一些通用注解进行解析和设置，这里的通用注解包括：`@Lazy`、`@Primary`、`@DependsOn`、`@Role`和`@Description`。
```java
// 解析类对象中的注解信息
AnnotationConfigUtils.processCommonDefinitionAnnotations(abd);  
// 解析形参中的注解信息，会进行覆盖
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
```

`AnnotationConfigUtils#processCommonDefinitionAnnotations()`方法会解析类对象中的通用注解信息：
```java
static void processCommonDefinitionAnnotations(AnnotatedBeanDefinition abd, AnnotatedTypeMetadata metadata) {  
   AnnotationAttributes lazy = attributesFor(metadata, Lazy.class);  
   if (lazy != null) {  
      abd.setLazyInit(lazy.getBoolean("value"));  
   }  
   else if (abd.getMetadata() != metadata) {  
      lazy = attributesFor(abd.getMetadata(), Lazy.class);  
      if (lazy != null) {  
         abd.setLazyInit(lazy.getBoolean("value"));  
      }  
   }  
  
   if (metadata.isAnnotated(Primary.class.getName())) {  
      abd.setPrimary(true);  
   }  
   AnnotationAttributes dependsOn = attributesFor(metadata, DependsOn.class);  
   if (dependsOn != null) {  
      abd.setDependsOn(dependsOn.getStringArray("value"));  
   }  
  
   AnnotationAttributes role = attributesFor(metadata, Role.class);  
   if (role != null) {  
      abd.setRole(role.getNumber("value").intValue());  
   }  
   AnnotationAttributes description = attributesFor(metadata, Description.class);  
   if (description != null) {  
      abd.setDescription(description.getString("value"));  
   }  
}
```

## 2.7 BeanDefinition自定义处理拦截
接下来，会根据形参的`BeanDefinitionCustomizer`进行自定义处理，可以修改`BeanDefinition`的信息：
```java
if (customizers != null) {  
   for (BeanDefinitionCustomizer customizer : customizers) {  
      customizer.customize(abd);  
   }  
}
```

## 2.8 作用域代理处理
接下来，会将`BeanDefinition`用`BeanDefinitionHolder`对象封装起来。然后根据作用域中的代理模式进行判断是否需要代理：
```java
BeanDefinitionHolder definitionHolder = new BeanDefinitionHolder(abd, beanName);  
definitionHolder = AnnotationConfigUtils.applyScopedProxyMode(scopeMetadata, definitionHolder, this.registry);
```

如果代理模式是`ScopedProxyMode.NO`（默认），则不会进行代理，直接返回。否则会创建包含代理信息的`BeanDefinitionHolder`：
```java
static BeanDefinitionHolder applyScopedProxyMode(  
      ScopeMetadata metadata, 
      BeanDefinitionHolder definition, 
      BeanDefinitionRegistry registry) {  
   // 获取scopedProxyMode
   ScopedProxyMode scopedProxyMode = metadata.getScopedProxyMode();  
   // 如果是ScopedProxyMode.NO，直接返回
   if (scopedProxyMode.equals(ScopedProxyMode.NO)) {  
      return definition;  
   }  
   // 否则，创建包含代理信息的BeanDefinitionHolder
   boolean proxyTargetClass = scopedProxyMode.equals(ScopedProxyMode.TARGET_CLASS);  
   return ScopedProxyCreator.createScopedProxy(definition, registry, proxyTargetClass);  
}
```

创建作用域代理的方式主要是新建一个`beanClass`为`ScopedProxyFactoryBean`的`RootBeanDefinition`。然后将原始的`bean`基本信息复制到该`RootBeanDefinition`对象中，并且设置一些代理相关的信息。并且会将原始的`BeanDefinition`注册到容器中。
```java
public static BeanDefinitionHolder createScopedProxy(BeanDefinitionHolder definition,  
      BeanDefinitionRegistry registry, boolean proxyTargetClass) {  
   String originalBeanName = definition.getBeanName();  
   BeanDefinition targetDefinition = definition.getBeanDefinition();  
   // 原始的beanName被替换为"scopedTarget."+beanName
   String targetBeanName = getTargetBeanName(originalBeanName);  
  
   // Create a scoped proxy definition for the original bean name,  
   // "hiding" the target bean in an internal target definition.   
   RootBeanDefinition proxyDefinition = new RootBeanDefinition(ScopedProxyFactoryBean.class);  
   proxyDefinition.setDecoratedDefinition(new BeanDefinitionHolder(targetDefinition, targetBeanName));  
   proxyDefinition.setOriginatingBeanDefinition(targetDefinition);  
   proxyDefinition.setSource(definition.getSource());  
   proxyDefinition.setRole(targetDefinition.getRole());  
  
   proxyDefinition.getPropertyValues().add("targetBeanName", targetBeanName);  
   if (proxyTargetClass) {  
      targetDefinition.setAttribute(AutoProxyUtils.PRESERVE_TARGET_CLASS_ATTRIBUTE, Boolean.TRUE);  
      // ScopedProxyFactoryBean's "proxyTargetClass" default is TRUE, so we don't need to set it explicitly here.  
   }  
   else {  
      proxyDefinition.getPropertyValues().add("proxyTargetClass", Boolean.FALSE);  
   }  
  
   // Copy autowire settings from original bean definition.  
   proxyDefinition.setAutowireCandidate(targetDefinition.isAutowireCandidate());  
   proxyDefinition.setPrimary(targetDefinition.isPrimary());  
   if (targetDefinition instanceof AbstractBeanDefinition) {  
      proxyDefinition.copyQualifiersFrom((AbstractBeanDefinition) targetDefinition);  
   }  
  
   // The target bean should be ignored in favor of the scoped proxy.  
   targetDefinition.setAutowireCandidate(false);  
   targetDefinition.setPrimary(false);  
  
   // Register the target bean as separate bean in the factory.  
   registry.registerBeanDefinition(targetBeanName, targetDefinition);  
  
   // Return the scoped proxy definition as primary bean definition  
   // (potentially an inner bean).   
   return new BeanDefinitionHolder(proxyDefinition, originalBeanName, definition.getAliases());  
}
```

例如，对于`ComponentE`：
```java
@Scope(proxyMode = ScopedProxyMode.TARGET_CLASS)  
public class ComponentE {  
}
```

对`ComponentE`进行注册，并获取`bean`信息：
```java
// 创建registry  
BeanDefinitionRegistry registry = new DefaultListableBeanFactory();  
// 创建reader，指定registry  
AnnotatedBeanDefinitionReader reader = new AnnotatedBeanDefinitionReader(registry);  
// 注册指定类作为bean，指定创建bean的方法  
reader.registerBean(ComponentE.class);  
// 打印ComponentE的信息  
System.out.println(((BeanFactory) registry).getBean("componentE"));  
System.out.println(((BeanFactory) registry).getBean("scopedTarget.componentE"));  
System.out.println(((BeanFactory) registry).getBean(ComponentE.class));  
System.out.println(((BeanFactory) registry).getBean(ComponentE.class).getClass());
```

输出结果如下：
```java
ComponentE@7cbd213e
ComponentE@7cbd213e
ComponentE@7cbd213e
class ComponentE$$EnhancerBySpringCGLIB$$d7c7ac87
```

实际上，作用域代理要配合`ScopedObject`接口使用。`ScopedProxyFactoryBean`创建代理`bean`对象时，会使用`DelegatingIntroductionInterceptor`对`ScopedObject`接口的方法进行拦截（由于目前不知道这样做的目的，这里不过多介绍）：
```java
public Object invoke(MethodInvocation mi) throws Throwable {  
   if (isMethodOnIntroducedInterface(mi)) {  
      // Using the following method rather than direct reflection, we  
      // get correct handling of InvocationTargetException      
      // if the introduced method throws an exception.      
      Object retVal = AopUtils.invokeJoinpointUsingReflection(this.delegate, mi.getMethod(), mi.getArguments());  
  
      // Massage return value if possible: if the delegate returned itself,  
      // we really want to return the proxy.      
      if (retVal == this.delegate && mi instanceof ProxyMethodInvocation) {  
         Object proxy = ((ProxyMethodInvocation) mi).getProxy();  
         if (mi.getMethod().getReturnType().isInstance(proxy)) {  
            retVal = proxy;  
         }  
      }  
      return retVal;  
   }  
  
   return doProceed(mi);  
}
```

## 2.9 注册到Spring容器
最后，会将`BeanDefinitionHolder`信息添加到Spring容器：
```java
BeanDefinitionReaderUtils.registerBeanDefinition(definitionHolder, this.registry);
```

`BeanDefinitionReaderUtils#registerBeanDefinition()`方法如下：
```java
public static void registerBeanDefinition(  
      BeanDefinitionHolder definitionHolder, 
      BeanDefinitionRegistry registry)  
      throws BeanDefinitionStoreException {  
  
   // Register bean definition under primary name.  
   String beanName = definitionHolder.getBeanName();  
   registry.registerBeanDefinition(beanName, definitionHolder.getBeanDefinition());  
  
   // Register aliases for bean name, if any.  
   String[] aliases = definitionHolder.getAliases();  
   if (aliases != null) {  
      for (String alias : aliases) {  
         registry.registerAlias(beanName, alias);  
      }  
   }  
}
```

不同的`BeanDefinitionRegistry`实现类会有不同的注册逻辑，这里主要介绍`DefaultListableBeanFactory`的实现。

`DefaultListableBeanFactory#registerBeanDefinition()`方法
```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)  
      throws BeanDefinitionStoreException {  
  
   Assert.hasText(beanName, "Bean name must not be empty");  
   Assert.notNull(beanDefinition, "BeanDefinition must not be null");  
  
   if (beanDefinition instanceof AbstractBeanDefinition) {  
      try {  
         // 校验
         ((AbstractBeanDefinition) beanDefinition).validate();  
      }  
      catch (BeanDefinitionValidationException ex) {  
         throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName,  
               "Validation of bean definition failed", ex);  
      }  
   }  
  
   // 校验beanName是否已存在
   BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);  
   // 如果beanName已存在
   if (existingDefinition != null) {  
      // 如果Spring容器不允许重载BeanDefinition，抛出异常
      if (!isAllowBeanDefinitionOverriding()) {  
         throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);  
      }  
      // 如果Spring容器不允许重载BeanDefinition，会进行覆盖
      else if (existingDefinition.getRole() < beanDefinition.getRole()) {  
         // e.g. was ROLE_APPLICATION, now overriding with ROLE_SUPPORT or ROLE_INFRASTRUCTURE  
         if (logger.isInfoEnabled()) {  
            logger.info("Overriding user-defined bean definition for bean '" + beanName +  
                  "' with a framework-generated bean definition: replacing [" +  
                  existingDefinition + "] with [" + beanDefinition + "]");  
         }  
      }  
      else if (!beanDefinition.equals(existingDefinition)) {  
         if (logger.isDebugEnabled()) {  
            logger.debug("Overriding bean definition for bean '" + beanName +  
                  "' with a different definition: replacing [" + existingDefinition +  
                  "] with [" + beanDefinition + "]");  
         }  
      }  
      else {  
         if (logger.isTraceEnabled()) {  
            logger.trace("Overriding bean definition for bean '" + beanName +  
                  "' with an equivalent definition: replacing [" + existingDefinition +  
                  "] with [" + beanDefinition + "]");  
         }  
      }  
      this.beanDefinitionMap.put(beanName, beanDefinition);  
   }  
   // 如果beanName不存在
   else {  
      // 如果Spring容器已经进入创建bean的阶段：加锁进行更新
      if (hasBeanCreationStarted()) {  
         synchronized (this.beanDefinitionMap) {  
            this.beanDefinitionMap.put(beanName, beanDefinition);  
            List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);  
            updatedDefinitions.addAll(this.beanDefinitionNames);  
            updatedDefinitions.add(beanName);  
            this.beanDefinitionNames = updatedDefinitions;  
            removeManualSingletonName(beanName);  
         }  
      }  
      // 如果Spring容器没有进入创建bean的阶段，即还在注册阶段：直接更新
      else {  
         this.beanDefinitionMap.put(beanName, beanDefinition);  
         this.beanDefinitionNames.add(beanName);  
         removeManualSingletonName(beanName);  
      }  
      this.frozenBeanDefinitionNames = null;  
   }  
   // 如果beanName对应的BeanDefinition和单例对象已经存在：即发生了覆盖
   if (existingDefinition != null || containsSingleton(beanName)) {  
      // 清除该beanName（以及子bean）的mergedBeanDefinition和单例对象缓存
      resetBeanDefinition(beanName);  
   }  
   // 否则，如果Spring容器可以缓存BeanDefinition的元数据
   else if (isConfigurationFrozen()) {  
      // 清除所有allBeanNamesByType和singletonBeanNamesByType缓存（类对象-beanNmae映射的缓存）
      clearByTypeCache();  
   }  
}
```

# 3 小结
通过阅读`AnnotatedBeanDefinitionReader`的源码，我们对注解形式的依赖配置有了更深层次的理解。

它可以类对象中的所有注解信息，生成`beanName`，并且设置一些基本的`BeanDefinition`属性：instanceSupplier、scope、lazyInit、primary、dependsOn、description。

如果进行了作用域代理，还会设置代理相关信息。