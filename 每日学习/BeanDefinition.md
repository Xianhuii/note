# 1 介绍
`BeanDefinition`是Spring中非常重要的一个概念。

在Spring启动时，会读取项目中依赖关系的配置（`xml`文件、`groovy`文件或注解），将这些依赖关系通过`BeanDefinition`进行缓存。

在实例化`bean`时，Spring容器会根据`BeanDefinition`中的信息进行创建对象、设置属性，也就是我们常说的控制反转（或者依赖注入）。

在缓存`BeanDefinition`后，实例化`bean`前，还可以通过`BeanFactoryPostProcessor`等方式对`BeanDefinition`进行修改，以此来增强功能（AOP就是通过类似方式实现的）。

因此，我们也可以从这个角度来理解Spring容器：配置文件→`BeanDefinition`→`bean`。

# 2 体系
![[BeanDefinition 1.png]]


BeanDefinition
BeanDefinitionReaderUtils
BeanDefinitionHolder
BeanDefinitionRegistry
BeanDefinitionReader