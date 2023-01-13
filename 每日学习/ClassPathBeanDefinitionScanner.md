# 1 介绍
`ClassPathBeanDefinitionScanner`可以扫描指定路径下的`@Component`类，将这些类解析成`BeanDefinition`，注册到Spring容器中。



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

# 3 源码解读

# 4 典型案例
