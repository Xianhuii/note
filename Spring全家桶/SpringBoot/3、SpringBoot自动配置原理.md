传统的Spring项目，需要我们对每个引入的组件进行手动配置。

这需要开发者对组件有深入的了解，否则很容易遗漏某些细节。对于业务开发人员/公司来说，他们只需要知道如何使用组件即可，不需要过多了解底层配置原理。如果有多个项目，则需要将配置进行多次拷贝，会增大无意义的工作量。

实际上，每个第三方组件的配置都是相对固定的，只有其中一些参数可能需要根据运行环境进行修改。例如`spring-mvc`有它自己的一套配置，`spring-mybatis`也有它自己的一套配置。我们可以将这些第三方组件的基础配置抽取出来，通过配置文件动态修改其中的运行参数。后续需要重复使用时，只需要将相同的基础依赖，可以大大减小重复开发的工作量。

另一方面，组件开发者肯定要比我们这些使用者更加熟悉底层细节。因此，最稳妥的方法是由组件开发者去抽象对应组件的基础配置。我们使用者只需要熟悉组件暴露的配置文件即可。这些由组件开发者抽象出的基础配置，在Spring Boot中就是`starter`。

Spring Boot就是为了解决上述问题而开发出来的。它提供自动配置的`SPI`机制，制定了从`starter`读取配置的规则。第三方组件根据规则编写基础配置信息，后续引入依赖时，Spring Boot会根据SPI规则读取配置。

Spring Boot自动配置的`SPI`机制原理基于`ConfigurationClassPostProcessor`，它是一个`BeanFactoryPostProcessor`，会在读取依赖配置后，对配置类`BeanDefinition`进行处理。自动配置机制使用了其中的`@Import`功能。

`@Import`注解可以注册指定依赖配置：
1. `ImportSelector`：根据选择规则注册。
2. `ImportBeanDefinitionRegistrar`：注册额外依赖配置。
3. 其他：作为配置类注册。

通过`org.springframework.boot.autoconfigure.AutoConfigurationPackages.Registrar`和`org.springframework.boot.autoconfigure.AutoConfigurationImportSelector`，自动配置`SPI`机制会自动注册以下依赖：
1. 标注`@EnableAutoConfiguration`注解的配置类所在包下的所有注解依赖。
2. 注册`META-INF/spring.factories`中键为`org.springframework.boot.autoconfigure.EnableAutoConfiguration`的类。
3. 注册`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`中的所有类。

Spring Boot自动配置`SPI`机制的相关类图如下：
![[EnableAutoConfiguration.png]]