# 1 介绍

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
- `registry`：
- `beanDefinitionDefaults`：
- `autowireCandidatePatterns`：
- `beanNameGenerator`：
- `scopeMetadataResolver`：
- `includeAnnotationConfig`：
# 2 基本使用

# 3 源码解读

# 4 典型案例
