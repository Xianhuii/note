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
在实际业务场景中，可能需要对数据库进行多次操作，但是这些操作在逻辑上是一个整体。

事务（Transaction）的作用就是保证这些操作在执行过程中的整体性，要么同时成功，要么同时失败。

举个最典型的例子，存在账务表`tb_account`：
| id  | name | money |
| --- | ---- | ----- |
| 1   | zhangsan | 100   |
| 2   | lisi | 100      |

如果`zhangsan`向`lisi`转账50元，需要执行如下SQL（暂时不考虑账户为负的问题）：
```sql
UPDATE tb_account SET money = (money - 50) WHERE id = 1;
UPDATE tb_account SET money = (money + 50) WHERE id = 2;
```

正常情况下，`zhangsan`的账户会变为50，`lisi`的账户会变为150。

但是在处理过程中出现了异常，第一条SQL执行成功，而第二个条SQL执行失败，就会变成`zhangsan`的账户为50，而`lisi`的账户仍为100。

如果使用事务对这两条SQL进行管理，那么在上述异常情况下，第二条SQL执行失败后，第一条SQL会回滚。因此`zhangsan`和`lisi`的账户会恢复成初始状态100。

## 2.2 Spring中事务的配置
Spring中对事务的配置位于`org.springframework.transaction.TransactionDefinition`，可以分为事务隔离级别、事务传播行为和其他三类。

需要注意的是，这些配置本质上是底层数据库所具有的功能，Spring只是将对应的参数传递给数据库进行执行。
### 2.2.1 事务隔离级别
事务隔离级别指的是：多个事务对同一个数据的可见性。

我们先考虑一个问题：什么情况下会存在多个事务对同一个数据进行操作？
1. 高并发情况下，多个请求在同一时间内对同一数据进行操作。
2. 大事务情况下，上一个事务还没有提交，下一个事务就开始对同一个数据进行操作。

事务隔离级别在数据库底层通常是使用锁实现的。

因此，在选择事务隔离级别时，我们需要综合考虑系统的并发程度、事务的大小以及SQL对锁的影响。

事务的隔离级别分为4个层次，它们的值与`java.sql.Connection.TRANSACTION_Xxx`一一对应。

#### 1 读未提交
读未提交（`TransactionDefinition.ISOLATION_READ_UNCOMMITTED`）：后一个事务可以读取前一个事务还未提交的数据。

该级别允许后一个事务读取前一个事务修改但还未提交的数据（即读取所有已修改的数据），由于前一个事务可能会被回滚，因此可能会出现脏读、不可重复读和幻读的问题。

脏读：
1. 后一个事务读取前一个事务修改但未提交内容（B）。
2. 前一个事务回滚后，该数据恢复初始值（A）。
3. 但是后一个事务已经读取到了错误的数据（B）。

不可重复读：
1. 前一个事务修改该数据（B）。
2. 后一个事务读取该数据（B）。
3. 前一个事务回滚后，该数据恢复初始值（A）。
4. 后一个事务再次读取该数据时，发现前后不一致（A）。

幻读：
1. 前一个事务插入了一个新数据。
2. 后一个事务读取到新数据。
3. 前一个事务回滚后，新数据被删除。
4. 后一个事务再次读取时，发现新数据不存在。

#### 2 读已提交
读已提交（`TransactionDefinition.ISOLATION_READ_COMMITTED`）：后一个事务只可以读取SQL执行前，前一个事务已提交的数据。

该级别只允许读取自身事务范围中已提交的数据，解决了脏读问题。

它可以读取当前事务启动后，SQL执行前，前一个事务已提交的数据，可能会出现不可重复读问题。

它没有限制自身事务外的未提交数据（如插入新的数据），可能会出现幻读的问题。

不可重复读：
1. 后一个事务读取数据（A）。
2. 前一个事务修改并提交该数据（B）。
5. 后一个事务再次读取该数据时，发现前后不一致（B）。

幻读：
1. 前一个事务插入了一个新数据。
2. 后一个事务读取到新数据。
3. 前一个事务回滚后，新数据被删除。
4. 后一个事务再次读取时，发现新数据不存在。

### 3 可重复读
可重复读（`TransactionDefinition.ISOLATION_REPEATABLE_READ`）：后一个事务只可以查询到事务启动前，前一个事务已提交的数据。

它主要是对读取数据的版本进行控制，用于解决不可重复读问题。

它没有限制自身事务外的未提交数据（如插入新的数据），仍可能会出现幻读的问题。

不可重复读：
1. 后一个事务开启事务。
2. 后一个事务读取数据（A）。
3. 前一个事务修改并提交该数据（B）。
5. 后一个事务再次读取该数据，获取的是当前事务版本的数据（A）。

幻读：
1. 前一个事务插入了一个新数据。
2. 后一个事务读取到新数据。
3. 前一个事务回滚后，新数据被删除。
4. 后一个事务再次读取时，发现新数据不存在。

#### 4 串行化
串行化（`TransactionDefinition.ISOLATION_SERIALIZABLE`）：所有事务按顺序执行。

由于不存在事务间的交叉执行，因此脏读、不可重复读和幻读问题都不存在。

这个级别的并发程度也最低，一般只有对数据要求极高的情况下使用，比如银行转账。

### 5 默认级别
上述4个级别都是数据库实际支持的，而默认级别（`TransactionDefinition.ISOLATION_DEFAULT`）是Spring在没有显示指定事务隔离级别情况下的配置，它会取底层数据库的默认级别。

例如，MySQL的InnoDB存储引擎的默认事务隔离级别是可重复读。

### 2.2.2 事务传播行为
事务传播行为指的是：多个事务嵌套执行时的行为，例如内层事务加入外层事务，或者内层事务新起事务。

#### 1 TransactionDefinition.PROPAGATION_REQUIRED
总是需要有一个事务：
- 如果当前已存在事务，则加入。
- 如果当前不存在事务，则新建一个事务。

#### 2 TransactionDefinition.PROPAGATION_SUPPORTS
支持存在事务，也支持不存在事务：
- 如果当前已存在事务，则加入。
- 如果当前不存在事务，就不需要事务。

#### 3 TransactionDefinition.PROPAGATION_MANDATORY
强制要求当前存在事务，不存在会抛出异常。

#### 4 TransactionDefinition.PROPAGATION_REQUIRES_NEW
总是要求创建新的事务。如果当前存在事务，会将当前事务暂定。

#### 5 TransactionDefinition.PROPAGATION_NOT_SUPPORTED
不支持事务。如果当前存在事务，会将当前事务暂定。

#### 6 TransactionDefinition.PROPAGATION_NEVER
永远不支持事务。如果当前存在事务，会抛出异常。

#### 7 TransactionDefinition.PROPAGATION_NESTED
支持嵌套事务：
- 如果当前已存在事务，作为嵌套事务执行。
- 如果当前不存在事务，则新建一个事务。

### 2.2.3 其他
#### 1 timeout
表示事务执行的过期时间，只有在事务隔离级别为`PROPAGATION_REQUIRED`和`PROPAGATION_REQUIRES_NEW`时起作用。

如果事务管理器不支持设置过期时间，会抛出异常。

#### 2 readOnly
是否优化为只读事务。

#### 3 rollbackFor、rollbackForClassName
指定在抛出什么异常才会进行回滚。

#### 4 noRollbackFor、noRollbackForClassName
指定在抛出什么异常时不会进行回滚。

# 3 声明式事务
声明式事务的使用方法很简单，只需要在Spring管理的`bean`中添加`@Transactional`注解。

该注解可以直接标注在类上，表示整个类中的所有方法都会使用事务进行管理：
```java
@Service  
@Transactional
public class TransactionServiceImpl implements ITransactionService {  
    @Override    
    public void test() {  
        System.out.println("test transaction");  
    }  
}
```

该注解也可以标注在方法上，表示只有该方法会使用事务进行管理：
```java
@Service  
public class TransactionServiceImpl implements ITransactionService {  
    @Transactional  
    @Override    
    public void test() {  
        System.out.println("test transaction");  
    }  
}
```

通过`@Transactional`注解的各个属性可以对事务进行配置：
![[Transactional.png]]

声明式注解是通过AOP实现的，Spring会在启动时为指定方法添加切面，用于处理创建事务、提交事务和回滚事务等操作。

在使用声明式注解时要注意一些问题。

`@Transactional`可以标注在接口上，或者接口的方法上。但是由于AOP可能是通过JDK动态代理，也可能通过`CGLIB`动态代理，无法保证一定能获取接口中的注解信息。因此，推荐始终在实现类上进行标注。

具有事务功能的方法实际上是代理后的方法。在使用`this`调用其他方法时，调用的实际代理前的方法。为了调用到代理方法，可以注入自身进行调用，也可以将方法移到其他类进行调用。

默认情况下，`@Transactional`只会对`RuntimeException`类型的异常进行回滚。如果遇到了奇怪的不回滚问题，可以从这方面考虑。

# 4 编程式事务
Spring支持使用`TransactionTemplate`、`TransactionalOperator`和`TransactionManager`实现类进行编程式事务。

由于`TransactionalOperator`针对reactive模式下的事务管理，平常很少用到，这里不进行介绍。感兴趣可以查看官方文档。

## 4.1 TransactionTemplate
在Spring Boot中，初始化时会自动创建`TransactionTemplate`，我们在使用时直接引入即可。

### 4.1.1 基本使用
`TransactionTemplate`通过`execute()`方法执行业务方法。它是一个模板方法，会在事务管理中插入执行我们定义的回调方法。

对于有返回值的情况，需要使用`TransactionCallback`回调：
```java
@Service  
public class TransactionServiceImpl implements ITransactionService {  
    @Autowired  
    private TransactionTemplate transactionTemplate;  
    public Object testTransactionCallback() {  
        return transactionTemplate.execute(new TransactionCallback<Object>() {  
            @Override  
            public Object doInTransaction(TransactionStatus status) {  
                // 业务方法  
                return null; // 业务返回值  
            }  
        });  
    }  
}
```

对于没有返回值的情况，需要使用`TransactionCallbackWithoutResult`回调：
```java
@Service  
public class TransactionServiceImpl implements ITransactionService {  
    @Autowired  
    private TransactionTemplate transactionTemplate;  
    public void testTransactionCallbackWithoutResult() {  
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {  
            @Override  
            protected void doInTransactionWithoutResult(TransactionStatus status) {  
                // 业务方法  
            }  
        });  
    }  
}
```

### 4.1.2 设置事务配置
Spring Boot为我们创建的`TransactionTemplate`采取的事务配置都是默认值，如果需要使用自定义配置的`TransactionTemplate`，可以手动创建：
```java
@Configuration  
public class TransactionConfig {  
    @Bean  
    public TransactionTemplate myTransactionTemplate(PlatformTransactionManager platformTransactionManager) {  
        TransactionTemplate myTransactionTemplate = new TransactionTemplate(platformTransactionManager);  
        // 自定义事务配置  
        myTransactionTemplate.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);  
        return myTransactionTemplate;  
    }  
}
```

在使用时，指定需要使用的即可：
```java
@Autowired  
@Qualifier("myTransactionTemplate")  
private TransactionTemplate transactionTemplate;
```

### 4.1.3 指定回滚异常
Spring Boot为我们创建的`TransactionTemplate`，默认会回滚`RuntimeException`、`Error`和`Throwable`。

如果需要指定回滚的异常，可以手动进行回滚：
```java
public void testRollback() {  
    transactionTemplate.execute(new TransactionCallbackWithoutResult() {  
        @Override  
        protected void doInTransactionWithoutResult(TransactionStatus status) {  
            try {  
                // 业务方法  
            } catch (Exception e) {  
                // 手动回滚  
                status.setRollbackOnly();  
            }  
        }  
    });  
}
```

## 4.2 TransactionManager实现类
`TransactionManager`是Spring事务管理的底层核心。

针对不同的模式，可以使用不同的实现类，我们这里讲解的是日常使用最多的`PlatformTransactionManager`。

在Spring Boot中，初始化时会自动创建`PlatformTransactionManager`，我们在使用时直接引入即可。

使用`PlatformTransactionManager`管理事务的方式和JDBC方式类似：
```java
public void testPlatformTransactionManager() {  
    // 事务配置  
    DefaultTransactionDefinition transactionDefinition = new DefaultTransactionDefinition();  
    transactionDefinition.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);  
    // 创建事务  
    TransactionStatus transaction = platformTransactionManager.getTransaction(transactionDefinition);  
    // 执行业务方法  
    try {  
        // 执行业务方法  
    } catch (Exception e) {  
        // 回滚事务  
        platformTransactionManager.rollback(transaction);  
        throw e;  
    }  
    // 提交事务  
    platformTransactionManager.commit(transaction);  
}
```

以上总结了Spring事务的基本使用，由于事务是数据库层面的概念，要深入掌握和灵活运用，需要深入学习数据库相关知识。

对于Spring事务的底层知识，可以查看后续文章、官方文档或者是底层源码。