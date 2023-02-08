Spring事务管理Java后端项目开发中都会用到的一个技术点，每个Java工程师都必须精通。

Spring事务管理可以分为两类：
- 声明式事务管理
- 编程式事务管理

声明式事务管理只需要在代码中添加`@Transactional`注解，即可自动进行事务管理。由于使用方便，是项目开发中的首选。

在Spring Boot中，只要我们引入了相关依赖，就会自动开启声明式事务功能。

例如，我们引入`mybatis-spring-boot-starter`，它会自动引入`spring-tx`等相关依赖，并且自动开启事务功能：
```java
<dependency>  
   <groupId>org.mybatis.spring.boot</groupId>  
   <artifactId>mybatis-spring-boot-starter</artifactId>  
</dependency>
```

