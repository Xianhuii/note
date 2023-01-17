# BeanFactory体系
![[BeanFactory.png]]
`BeanFactory`是Spring最核心的功能，它就是我们常说的Spring IoC容器。

`BeanFactory`体系下包含许多接口，它们分别代表Spring IoC容器的不同功能：
- `BeanFactory`：提供最基础的获取`bean`信息的方法，如`getBean()`。
- `HierarchicalBeanFactory`：提供父子层级Spring容器的基础方法，如`getParentBeanFactory()`。
- `AutowireCapableBeanFactory`：提供实例化、自动装配等基础功能，如`createBean()`。
- `ListableBeanFactory`：提供枚举所有`bean`的功能，如

# registerBeanDefinition

# createBean

# getBean
