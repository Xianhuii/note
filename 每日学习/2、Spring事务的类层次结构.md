如果有这么一个需求：支持所有数据库系统的事务管理，包括创建事务、提交事务和回滚事务。

你会怎么设计？
![[Pasted image 20230108121500.png]]

本文介绍Spring是如何设计事务管理功能，将事务管理中的各个功能抽象成Java中的类。

通过学习Spring事务的类层次结构，一方面可以深入理解Spring事务的使用，另一方面可以提高自己的抽象思维。

# 1 TransactionManager
Spring将事务管理抽象成`TransactionManager`体系结构，该体系的核心功能就是封装各种数据库的创建事务、提交事务和回滚事务方法。
![[TransactionManager.png]]

`TransactionManager`是事务管理的顶级抽象。为了兼容命令式和反应式的实现，实际上它只是一个标记接口，并没有定义方法。

`TransactionManager`下层有`PlatformTransactionManager`和`ReactiveTransactionManager`两个子接口，分别代表命令式和反应式的编程风格。其中，`ReactiveTransactionManager`的反应式接口在日常工作中几乎不使用，这里不过多介绍。

`PlatformTransactionManager`（命令式编程）是日常使用的事务管理器，其中定义了创建事务、提交事务和回滚事务3个基础方法：
![[PlatformTransactionManager.png]]

## 1.1 AbstractPlatformTransactionManager
`AbstractPlatformTransactionManager`抽象类Spring事务管理的核心。

`AbstractPlatformTransactionManager`抽象类实现上述3个基础方法，定义了Spring事务管理的工作流，但是具体功能实际上仍然是交给不同数据库实现类去完成（模板方法）。

`AbstractPlatformTransactionManager#getTransaction()`方法中定义的创建事务的工作流，会根据事务传播行为进行对应处理：
```java
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
   // 1、创建事务
   Object transaction = doGetTransaction();  
   // 2、当前已存在事务：根据事务传播行为处理
   if (isExistingTransaction(transaction)) {
      return handleExistingTransaction(def, transaction, debugEnabled);  
   }  
   // 3、当前不存在事务：根据事务传播行为分别处理
   // 3.1、PROPAGATION_MANDATORY：强制要求存在事务
   if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {  
      throw new IllegalTransactionStateException(  
            "No existing transaction found for transaction marked with propagation 'mandatory'");  
   }  
   // 3.2、PROPAGATION_REQUIRED、PROPAGATION_REQUIRES_NEW、PROPAGATION_NESTED
   else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||  
         def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||  
         def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {  
      // 暂定外层事务
      SuspendedResourcesHolder suspendedResources = suspend(null);  
      if (debugEnabled) {  
         logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);  
      }  
      try {  
         // 开始新事务
         return startTransaction(def, transaction, debugEnabled, suspendedResources);  
      }  
      catch (RuntimeException | Error ex) {  
         resume(null, suspendedResources);  
         throw ex;  
      }  
   }  
   // 3.3、其他事务传播行为：创建空事务
   else {  
      if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {  
         logger.warn("Custom isolation level specified but no actual transaction initiated; " +  
               "isolation level will effectively be ignored: " + def);  
      }  
      boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);  
      return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);  
   }  
}
```

`AbstractPlatformTransactionManager#commit()`方法中定义的提交事务的工作流：
```java
public final void commit(TransactionStatus status) throws TransactionException {  
   DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;  
   // 1、校验是否需要回滚
   if (defStatus.isLocalRollbackOnly()) {  
      if (defStatus.isDebug()) {  
         logger.debug("Transactional code has requested rollback");  
      }  
      processRollback(defStatus, false);  
      return;  
   }  
   if (!shouldCommitOnGlobalRollbackOnly() && defStatus.isGlobalRollbackOnly()) {  
      if (defStatus.isDebug()) {  
         logger.debug("Global transaction is marked as rollback-only but transactional code requested commit");  
      }  
      processRollback(defStatus, true);  
      return;  
   }  
   // 2、提交事务
   processCommit(defStatus);  
}
```