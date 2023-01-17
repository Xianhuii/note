# BeanFactory体系
![[BeanFactory.png]]
`BeanFactory`是Spring最核心的功能，它就是我们常说的Spring IoC容器。

`BeanFactory`体系下包含许多接口，它们分别代表Spring IoC容器的不同功能：
- `BeanFactory`：提供最基础的获取`bean`信息的方法，如`getBean()`。
- `HierarchicalBeanFactory`：提供父子层级Spring容器的基础方法，如`getParentBeanFactory()`。
- `AutowireCapableBeanFactory`：提供实例化、自动装配等基础功能，如`createBean()`。
- `ListableBeanFactory`：提供枚举所有`bean`的功能，如`getBeansOfType()`。
- `ConfigurableBeanFactory`：提供配置Spring容器的基础功能，是`BeanFactory`接口的补充。如`addBeanPostProcessor()`。
- `ConfigurableListableBeanFactory`：提供获取和修改`BeanDefinition`、预实例化单例对象的功能。如`getBeanDefinition()`。
- `ApplicationContext`：应用层Spring容器的顶级接口。
- `BeanDefinitionRegistry`：提供注册`BeanDefinition`的功能，如`registerBeanDefinition()`。不是`BeanFactory`的子接口，但它是`BeanFactory`体系的核心组成部分。

虽然`BeanFactory`体系的接口众多，但是它们的核心实现类只有`DefaultListableBeanFactory`。

我们只需要按照`DefaultListableBeanFactory`的基本使用流程，掌握其中的关键性方法的源码，就能够很好的理解Spring IoC容器。

在对Spring IoC容器有了整体的认识后，再去针对性研究它提供的特性功能，就能够完全掌握Spring IoC容器。

# 2 DefaultListableBeanFactory

`DefaultListableBeanFactory`的成员变量很多，这里
## 2.1 registerBeanDefinition
注册`BeanDefinition`是`BeanDefinitionRegistry`接口提供的方法，`DefaultListableBeanFactory`对其进行了实现。

由于

`DefaultListableBeanFactory#registerBeanDefinition()`：
```java
public void registerBeanDefinition(String beanName, BeanDefinition beanDefinition)  
      throws BeanDefinitionStoreException {  
   // 校验beanDefinition信息的完整性
   if (beanDefinition instanceof AbstractBeanDefinition) {  
      try {  
         ((AbstractBeanDefinition) beanDefinition).validate();  
      }  
      catch (BeanDefinitionValidationException ex) {  
         throw new BeanDefinitionStoreException(beanDefinition.getResourceDescription(), beanName, "Validation of bean definition failed", ex);  
      }  
   }  
  
   // 获取缓存
   BeanDefinition existingDefinition = this.beanDefinitionMap.get(beanName);  
   // 如果该beanDefinition已经注册
   if (existingDefinition != null) {  
      // 如果beanFactory不允许beanDefinition重载，抛出异常
      if (!isAllowBeanDefinitionOverriding()) {  
         throw new BeanDefinitionOverrideException(beanName, beanDefinition, existingDefinition);  
      }  
      // 如果beanFactory允许beanDefinition重载，进行覆盖（默认会进行覆盖）
      this.beanDefinitionMap.put(beanName, beanDefinition);  
   }  
   // 如果该beanDefinition未注册
   else {  
      // 如果beanFactory已经创建bean
      if (hasBeanCreationStarted()) {  
         // 加锁，更新beanDefinitionMap缓存
         synchronized (this.beanDefinitionMap) {  
            this.beanDefinitionMap.put(beanName, beanDefinition);  
            List<String> updatedDefinitions = new ArrayList<>(this.beanDefinitionNames.size() + 1);  
            updatedDefinitions.addAll(this.beanDefinitionNames);  
            updatedDefinitions.add(beanName);  
            this.beanDefinitionNames = updatedDefinitions;  
            removeManualSingletonName(beanName);  
         }  
      }  
      // 如果beanFactory还没有创建bean，仍处于注册bean阶段
      else {  
         // 直接更新beanDefinitionMap缓存
         this.beanDefinitionMap.put(beanName, beanDefinition);  
         this.beanDefinitionNames.add(beanName);  
         removeManualSingletonName(beanName);  
      }  
      this.frozenBeanDefinitionNames = null;  
   }  
   // 如果beanDefinition已存在，或单例bean已存在，需要清除相关缓存信息：mergedBeanDefinitions、singletonObjects等
   if (existingDefinition != null || containsSingleton(beanName)) {  
      resetBeanDefinition(beanName);  
   }  
   // 如果beanDefinition不存在，并且单例bean不存在，并且beanFactory会缓存所有beanDefinition的元数据
   else if (isConfigurationFrozen()) {  
      // 清除allBeanNamesByType和singletonBeanNamesByType
      clearByTypeCache();  
   }  
}
```

# createBean

# getBean
