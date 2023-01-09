# 1 初始化
在`DataSourceTransactionManagerAutoConfiguration.JdbcTransactionManagerConfiguration#transactionManager()`会创建`transactionManager`的`bean`对象：
```java
@Bean  
@ConditionalOnMissingBean(TransactionManager.class)  
DataSourceTransactionManager transactionManager(Environment environment, DataSource dataSource,  
      ObjectProvider<TransactionManagerCustomizers> transactionManagerCustomizers) {  
   DataSourceTransactionManager transactionManager = createTransactionManager(environment, dataSource);  
   transactionManagerCustomizers.ifAvailable((customizers) -> customizers.customize(transactionManager));  
   return transactionManager;  
}
```

它会创建`JdbcTransactionManager`或`DataSourceTransactionManager`实现类：
```java
private DataSourceTransactionManager createTransactionManager(Environment environment, DataSource dataSource) {  
   return environment.getProperty("spring.dao.exceptiontranslation.enabled", Boolean.class, Boolean.TRUE)  
         ? new JdbcTransactionManager(dataSource) : new DataSourceTransactionManager(dataSource);  
}
```

在`TransactionAutoConfiguration.TransactionTemplateConfiguration#transactionTemplate()`会创建`transactionTemplate`的`bean`对象，将上述`transactionManager`注入：
```java
@Bean  
@ConditionalOnMissingBean(TransactionOperations.class)  
public TransactionTemplate transactionTemplate(PlatformTransactionManager transactionManager) {  
   return new TransactionTemplate(transactionManager);  
}
```

# 2 事务管理流程
通过`TransactionTemplate#execute()`方法可以进行编程式事务管理，其内部会调用`transactionManager`进行获取事务、提交事务和回滚事务：
```java
public <T> T execute(TransactionCallback<T> action) throws TransactionException {  
   Assert.state(this.transactionManager != null, "No PlatformTransactionManager set");  
  
   if (this.transactionManager instanceof CallbackPreferringPlatformTransactionManager) {  
      // WebSphereUowTransactionManager事务管理流程
      return ((CallbackPreferringPlatformTransactionManager) this.transactionManager).execute(this, action);  
   }  
   else {  
      // 获取事务
      TransactionStatus status = this.transactionManager.getTransaction(this);  
      T result;  
      try {  
         // 执行业务方法
         result = action.doInTransaction(status);  
      }  
      // 回滚事务：RuntimeException/Error/Throwable异常才会回滚
      catch (RuntimeException | Error ex) {  
         rollbackOnException(status, ex);  
         throw ex;  
      }  
      catch (Throwable ex) {  
         // Transactional code threw unexpected exception -> rollback  
         rollbackOnException(status, ex);  
         throw new UndeclaredThrowableException(ex, "TransactionCallback threw undeclared checked exception");  
      }  
      // 提交事务
      this.transactionManager.commit(status);  
      return result;  
   }  
}
```