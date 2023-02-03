
# 2 工作原理
`@Configuration`的工作原理十分简单，它基于Spring容器的`BeanFactoryPostProcessor`机制，具体是在`AbstractApplicationContext#refresh()`方法`invokeBeanFactoryPostProcessors()`阶段，使用`ConfigurationClassPostProcessor`遍历容器中所有标注`@Configuration`的`BeanDefinition`缓存进行处理，包括：
1. 处理注解：`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、`@Bean`
2. 

## 2.1 注册ConfigurationClassPostProcessor

## 2.2 执行ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry方法

## 2.3 执行ConfigurationClassPostProcessor#postProcessBeanFactory方法