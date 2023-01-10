Spring IoC容器本质上是一个管理Java对象的工具。

在项目启动时，它会读取开发人员定义的依赖关系，将这些依赖关系缓存到容器内部。

在适当的时机，比如开发人员需要使用某个类对象时，Spring IoC容器会根据依赖关系进行创建对象，设置成员变量，然后将对象返回给开发人员。

在Spring IoC容器中，将上述功能拆分成了以下模块：
1. `BeanFactory`
2. `ApplicationContext`
4. 读取依赖配置：`AnnotatedBeanDefinitionReader`、`ClassPathBeanDefinitionScanner`和`BeanDefinitionReader`
5. `Environment`

通过系统学习这些模块，就可以建立Spring的坚实基础。

后续再深入学习Spring的启动流程、生命周期、作用域等细节，就能够全面掌握Spring的核心功能了。

# 1 BeanFactory
![[DefaultListableBeanFactory.png]]
`BeanFactory`翻译过来就是`bean`工厂，通常将Spring容器管理的Java对象叫做`bean`。

`BeanFactory`实际上就是Spring IoC容器，它会缓存开发人员定义的依赖关系。

开发人员通过`getBean()`方法从`BeanFactory`中获取`bean`。

`BeanFactory`在执行`getBean()`方法时，会根据依赖关系进行创建对象，设置成员变量，然后将对象（`bean`）返回给开发人员。如果作用域是`singleton`，还会将`bean`缓存起来，下次获取时直接返回缓存值。

`DefaultListableBeanFactory`是`BeanFactory`的一个默认实现，只要掌握`DefaultListableBeanFactory`的基本使用、执行原理和相关源码，就可以对Spring IoC容器有深入的理解，打下Spring的坚实基础。

`BeanFactory`实际上是针对Spring内部开发的底层工具，应用开发人员通常不会跟`BeanFactory`打交道，接触更多的是`ApplicationContext`。

`ApplicationContext`会对`BeanFactory`进行代理，通常会使用`DefaultListableBeanFactory`作为`bean`工厂，所以`DefaultListableBeanFactory`是十分重要的。

# 2 AplicationContext
![[AnnotationConfigApplicationContext.png]]
`ApplicationContext`是对应用层开放的一个模块。

`ApplicationContext`继承了`BeanFactory`接口，它的实现类通常会持有`DefaultListableBeanFactory`成员变量，通过代理设计模式向开发人员提供`getBean()`等方法。

`ApplicationContext`除了集成`BeanFactory`的功能，它提供了从不同形式配置中获取依赖关系和获取环境变量的能力。

例如，`AnnotationConfigApplicationContext`可以从注解中获取依赖关系，`ClassPathXmlApplicationContext`和`FileSystemXmlApplicationContext`可以从`xml`文件中获取依赖关系。

因此，我们只要掌握上述`ApplicationContext`实现类，就能够对不同形式的依赖配置，环境变量的读取有深入的理解。

需要注意的是，在日常工作中可能使用的并不是上述实现类。

例如Spring Boot中可能会使用`ServletWebServerApplicationContext`，但是它们的基本原理都是差不多的。只要我们掌握上述实现类，管他是什么实现类，大概看一下源码就都能很快的掌握。

# 3 BeanDefinition
![[BeanDefinition.png]]
`BeanDefinition`是依赖关系在`BeanFactory`中的缓存。

在创建`bean`时，`BeanFactory`会根据对应的`BeanDefinition`进行创建对象，设置成员变量。

`BeanDefinition`定义了依赖关系的各种属性，学习`BeanDefinitio`可以帮助我们深入理解日常工作中要怎么定义配置依赖关系，也可以深入理解`BeanFactory`是怎么创建对象，设置成员变量的。

# 4 AnnotatedBeanDefinitionReader
![[AnnotatedBeanDefinitionReader.png]]
`AnnotatedBeanDefinitionReader`会根据类对象读取注解形式的依赖关系。

例如，给定一个`@Configuration`标注的类对象，它可以从该类对象为入口，读取所有相关的依赖关系。

# 5 ClassPathBeanDefinitionScanner
![[ClassPathBeanDefinitionScanner.png]]
`ClassPathBeanDefinitionScanner`会根据给定包路径进行扫描，读取所有注解形式的依赖关系。

# 6 BeanDefinitionReader
![[BeanDefinitionReader.png]]
`BeanDefinitionReader`是读取`xml`和`groovy`形式依赖关系的工具，可以将这些配置文件中的依赖关系解析成`BeanDefinition`，并且缓存到`BeanFactory`中。

# 7 Environment
![[StandardEnvironment.png]]
`Environment`表示环境变量，包括系统环境变量、Spring Boot的profile、`xxx.properties`和`xxx.yml`。

通常使用的实现类是`StandardEnvironment`，我们只要掌握了它的源码，对Spring体系的环境变量就有了整体的把握。