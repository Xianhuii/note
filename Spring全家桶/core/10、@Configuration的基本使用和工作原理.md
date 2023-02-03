
# 2 工作原理
@Configuration的工作原理十分简单，它基于`ApplicationContext`的`BeanFactoryPostProcessor`机制，具体是在`AbstractApplicationContext#refresh()`方法`invokeBeanFactoryPostProcessors()`阶段，使用`ConfigurationClassPostProcessor`遍历容器中所有标注@Configuration的BeanDefinition缓存进行处理，包括：
1. 处理注解：`@PropertySource`、`@ComponentScan`、`@Import`、`@ImportResource`、`@Bean`。
2. 使用`CGLIB`代理方式增强配置类功能。
3. 注册`ImportAwareBeanPostProcessor`。

需要注意的是，`BeanFactory`并不提供BeanFactoryPostProcessor功能，如果使用底层的DefaultListableBeanFactory作为容器，不能对@Configuration进行处理。

## 2.1 工作流
注册ConfigurationClassPostProcessor
执行ConfigurationClassPostProcessor#postProcessBeanDefinitionRegistry方法
执行ConfigurationClassPostProcessor#postProcessBeanFactory方法

## 2.2 核心方法
