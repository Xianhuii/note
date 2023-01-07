# 1 依赖
Spring事务的实际源码在`spring-tx`中：
```xml
<dependency>
   <groupId>org.springframework</groupId>
   <artifactId>spring-tx</artifactId>
</dependency>
```
在Spring体系中，通常ORM框架内部都会直接引用`spring-tx`。因此，我们不必额外手动引入。
例如，我们需要使用`Mybatis`作为数据库访问层，只需要引入如下依赖：
```xml
<dependency>  
    <groupId>org.mybatis.spring.boot</groupId>  
    <artifactId>mybatis-spring-boot-starter</artifactId>
</dependency>
```
`mybatis-spring-boot-starter`内部会引入`spring-tx`： 
![[Pasted image 20230106231444.png]]
# 2 基础理论
# 2.1 事务
事务（Transaction）是

# 3 声明式事务

# 4 编程式事务