如果有这么一个需求：支持所有数据库系统的事务管理，包括获取事务、提交事务和回滚事务。

你会怎么设计？
![[Pasted image 20230108121500.png]]

本文介绍Spring是如何设计事务管理功能，将事务管理中的各个功能抽象成Java中的类。

通过学习Spring事务的类层次结构，一方面可以深入理解Spring事务的使用，另一方面可以提高自己的抽象思维。

# 1 TransactionManager
Spring将事务管理抽象成`TransactionManager`体系结构，该体系的核心功能就是封装各种数据库的获取事务、提交事务和回滚事务方法。
![[TransactionManager.png]]

`TransactionManager`是事务管理的顶级抽象。为了兼容命令式和反应式的实现，实际上它只是一个标记接口，并没有定义方法。

`TransactionManager`下层有`PlatformTransactionManager`和`ReactiveTransactionManager`两个子接口，分别代表命令式和反应式的编程风格。其中，`ReactiveTransactionManager`的反应式接口在日常工作中几乎不使用，这里不过多介绍。

`PlatformTransactionManager`（命令式编程）是日常使用的事务管理器，其中定义了获取事务、提交事务和回滚事务3个基础方法：
![[PlatformTransactionManager.png]]

## 1.1 AbstractPlatformTransactionManager
`AbstractPlatformTransactionManager`抽象类Spring事务管理的核心。

`AbstractPlatformTransactionManager`抽象类实现上述3个基础方法，定义了Spring事务管理的工作流，但是具体功能实际上仍然是交给不同数据库实现类去完成（模板方法）。

本节简单介绍获取事务、提交事务和回滚事务的入口，后续会详细介绍整个流程。

`AbstractPlatformTransactionManager#getTransaction()`方法中定义的获取事务的工作流，会根据事务传播行为进行对应处理：
```java
public final TransactionStatus getTransaction(@Nullable TransactionDefinition definition) throws TransactionException {
   // 1、获取事务
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
      // 暂停外层事务
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

`AbstractPlatformTransactionManager#rollback()`方法中定义了回滚事务的工作流：
```java
public final void rollback(TransactionStatus status) throws TransactionException {  
   if (status.isCompleted()) {  
      throw new IllegalTransactionStateException(  
            "Transaction is already completed - do not call commit or rollback more than once per transaction");  
   }  
  
   DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;  
   // 回滚事务
   processRollback(defStatus, false);  
}
```

在工作流中，`AbstractPlatformTransactionManager`提供了以下模板方法，需要不同子类进行实现：
- `doGetTransaction()`：返回当前事务状态的事务对象。
- `isExistingTransaction()`：判断当前是否是否已经存在。
- `doBegin()`：根据给定的事务定义开始一个具有语义的新事务。
- `doSuspend()`：暂停当前​​事务的资源。
- `doResume()`：恢复当前事务的资源。
- `doCommit()`：提交事务。
- `doRollback()`：回滚事务。
- ……

`AbstractPlatformTransactionManager`有`DataSourceTransactionManager`和`JtaTransactionManager`两个主要的子类。其中`DataSourceTransactionManager`是针对数据源的实现，是我们日常工作中所使用的实现。

## 1.2 DataSourceTransactionManager
`DataSourceTransactionManager`内部持有一个`DataSource`对象，通过该对象进行不同数据库的事务管理。
![[DataSourceTransactionManager.png]]

`DataSourceTransactionManager`有一个`JdbcTransactionManager`实现类，它与事务管理工作流关系不大，主要用来对JDBC异常进行解析。

# 2 TransactionDefinition
`TransactionDefinition`表示事务的配置。

在创建事务时，需要为事务指定配置，就需要传入`TransactionDefinition`的实现类对象。

在`TransactionDefinition`中，定义了获取事务配置的方法，Spring通过这些方法获取相关配置。它还定义了事务配置的各个常量。
![[TransactionDefinition.png]]

我们在使用编程式事务时，可以使用`DefaultTransactionDefinition`实现类，它指定了各个事务配置的默认值：
- 事务传播行为：`PROPAGATION_REQUIRED`。
- 事务隔离级别：`ISOLATION_DEFAULT`，对于MySQL是可重复读。
- 过期时间：`TIMEOUT_DEFAULT`。
- 是否只读：`false`。

如果需要修改事务配置，直接更改`DefaultTransactionDefinition`的对应值即可。

实际上`TransactionDefinition`本身就会设置各个事务配置的默认值。

![[DefaultTransactionDefinition.png]]

`DefaultTransactionDefinition`有一个特别的子类：`TransactionTemplate`。它定义了事务管理的模板方法`execute()`，通过内部持有`PlatformTransactionManager`对象，以自身为事务配置，可以很方便地对事务进行管理。
![[DefaultTransactionDefinition 1.png]]

# 3 TransactionStatus
`TransactionStatus`表示事务的状态。

Spring事务管理器可以通过`TransactionStatus`对象来判断事务的状态，用来决定是否进行提交事务、回滚事务或者其他操作。
![[TransactionStatus.png]]

需要注意的是，`TransactionStatus`表示的是逻辑事务的状态，即虽然它的`isNewTransaction()`返回值是`true`，但实际上数据库并没有创建物理事务。

# 4 TransactionSynchronizationManager

`TransactionSynchronizationManager`贯穿了事务管理的整个流程，它会保存事务资源、事务名称、事务隔离级别等信息。

同时，`TransactionSynchronizationManager`可以对当前线程的事务添加`TransactionSynchronization`回调，可以对事务管理的节点进行拦截：
![[TransactionSynchronization 1.png]]

# 5 获取事务流程
获取事务的入口在`PlatformTransactionManager#getTransaction()`方法。

`AbstractPlatformTransactionManager#getTransaction()`对该方法进行了实现。该方法会根据事务传播行为进行创建事务，或者返回已存在的事务。接下来介绍该方法的执行逻辑。
## 5.1 获取事务配置
首先，会判断是否有指定事务配置对象。如果不存在，会使用`StaticTransactionDefinition`对象作为默认值，它本质上使用`TransactionDefinition`中的默认配置：
```java
TransactionDefinition def = (definition != null ? definition : TransactionDefinition.withDefaults());
```

## 5.2 获取当前线程的事务（外层方法事务）
使用`AbstractPlatformTransactionManager#doGetTransaction()`方法获取当前线程绑定的事务：
```java
Object transaction = doGetTransaction();
```

`DataSourceTransactionManager#doGetTransaction()`对该方法进行了实现：
```java
protected Object doGetTransaction() {  
   // 创建txObject
   DataSourceTransactionObject txObject = new DataSourceTransactionObject();  
   txObject.setSavepointAllowed(isNestedTransactionAllowed());  
   // 获取当前线程绑定的数据库连接：当前事务/null
   ConnectionHolder conHolder =  
         (ConnectionHolder) TransactionSynchronizationManager.getResource(obtainDataSource());  
   txObject.setConnectionHolder(conHolder, false);  
   return txObject;  
}
```

`TransactionSynchronizationManager#resources`中会保存每个`dataSource`对应的事务资源：
```java
private static final ThreadLocal<Map<Object, Object>> resources =  
      new NamedThreadLocal<>("Transactional resources");
```

如果当前已存在事务，会返回该事务资源。如果当前不存在事务，会返回`null`。

## 5.3 已存在外层事务的处理流程
如果当前线程已经存在事务，说明出现了Spring事务方法的相互调用，会根据事务传播行为进行不同处理：
```java
// 判断当前线程是否已存在事务
if (isExistingTransaction(transaction)) {  
   // 执行已存在事务的处理流程
   return handleExistingTransaction(def, transaction, debugEnabled);  
}
```

通过`AbstractPlatformTransactionManager#isExistingTransaction()`方法可以判断当前线程是否已存在事务。

`DataSourceTransactionManager#isExistingTransaction()`实现如下：
```java
protected boolean isExistingTransaction(Object transaction) {  
   DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;  
   // 是否存在connectionHolder && 事务是否启动
   return (txObject.hasConnectionHolder() && txObject.getConnectionHolder().isTransactionActive());  
}
```

`AbstractPlatformTransactionManager#handleExistingTransaction()`方法定义了外层方法已存在事务时，内层方法根据不同事务传播行为进行的不同处理流程：
1. 如果内层方法的事务传播行为是`PROPAGATION_NEVER`，会抛出异常。
2. 如果内层方法的事务传播行为是`PROPAGATION_NOT_SUPPORTED`，会暂停外层方法的事务，并返回当前处理结果的`TransactionStatus`对象。
3. 如果内层方法的事务传播行为是`PROPAGATION_REQUIRES_NEW`，会暂停外层方法的事务，开启内层方法的新事务，并返回该新事务的`TransactionStatus`对象。
4. 如果内层方法的事务传播行为是`PROPAGATION_NESTED`，会判断数据库是否支持嵌套事务。如果不支持，会抛出异常；如果支持保存点方式的嵌套事务（JDBC），会创建保存点；如果不支持保存点方式的嵌套事务（JTA），会创建嵌套事务作为新事务。
5. 如果内层方法的事务传播行为是`PROPAGATION_SUPPORTS`或`PROPAGATION_REQUIRED`。如果内层方法的事务隔离级别是`ISOLATION_DEFAULT`，并且外层方法的事务隔离级别与内层方法不一致，会抛出异常。如果内层方法不是只读，但外层方法是只读，会抛出异常。由于当前已存在事务，所以不用其他特殊处理。
```java
private TransactionStatus handleExistingTransaction(  
      TransactionDefinition definition, Object transaction, boolean debugEnabled)  
      throws TransactionException {  
   // 如果内层方法的事务传播行为是PROPAGATION_NEVER，会抛出异常。
   if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NEVER) {  
      throw new IllegalTransactionStateException(  
            "Existing transaction found for transaction marked with propagation 'never'");  
   }  
   // 如果内层方法的事务传播行为是`PROPAGATION_NOT_SUPPORTED`，会暂停外层方法的事务，并返回当前处理结果的`TransactionStatus`对象。
   if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NOT_SUPPORTED) {  
      if (debugEnabled) {  
         logger.debug("Suspending current transaction");  
      }  
      Object suspendedResources = suspend(transaction);  
      boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);  
      return prepareTransactionStatus(  
            definition, null, false, newSynchronization, debugEnabled, suspendedResources);  
   }  
   // 如果内层方法的事务传播行为是`PROPAGATION_REQUIRES_NEW`，会暂停外层方法的事务，开启内层方法的新事务，并返回该新事务的`TransactionStatus`对象。
   if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW) {  
      if (debugEnabled) {  
         logger.debug("Suspending current transaction, creating new transaction with name [" +  
               definition.getName() + "]");  
      }  
      SuspendedResourcesHolder suspendedResources = suspend(transaction);  
      try {  
         return startTransaction(definition, transaction, debugEnabled, suspendedResources);  
      }  
      catch (RuntimeException | Error beginEx) {  
         resumeAfterBeginException(transaction, suspendedResources, beginEx);  
         throw beginEx;  
      }  
   }  
   // 如果内层方法的事务传播行为是`PROPAGATION_NESTED`，会判断数据库是否支持嵌套事务。如果不支持，会抛出异常；如果支持保存点方式的嵌套事务（JDBC），会创建保存点；如果不支持保存点方式的嵌套事务（JTA），会创建嵌套事务作为新事务。
   if (definition.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {  
      if (!isNestedTransactionAllowed()) {  
         throw new NestedTransactionNotSupportedException(  
               "Transaction manager does not allow nested transactions by default - " +  
               "specify 'nestedTransactionAllowed' property with value 'true'");  
      }  
      if (debugEnabled) {  
         logger.debug("Creating nested transaction with name [" + definition.getName() + "]");  
      }  
      if (useSavepointForNestedTransaction()) {  
         // Create savepoint within existing Spring-managed transaction,  
         // through the SavepointManager API implemented by TransactionStatus.         // Usually uses JDBC 3.0 savepoints. Never activates Spring synchronization.         DefaultTransactionStatus status =  
               prepareTransactionStatus(definition, transaction, false, false, debugEnabled, null);  
         status.createAndHoldSavepoint();  
         return status;  
      }  
      else {  
         // Nested transaction through nested begin and commit/rollback calls.  
         // Usually only for JTA: Spring synchronization might get activated here         // in case of a pre-existing JTA transaction.         
         return startTransaction(definition, transaction, debugEnabled, null);  
      }  
   }  
   // 如果内层方法的事务传播行为是`PROPAGATION_SUPPORTS`或`PROPAGATION_REQUIRED`。如果内层方法的事务隔离级别是`ISOLATION_DEFAULT`，并且外层方法的事务隔离级别与内层方法不一致，会抛出异常。如果内层方法不是只读，但外层方法是只读，会抛出异常。由于当前已存在事务，所以不用其他特殊处理。
   if (debugEnabled) {  
      logger.debug("Participating in existing transaction");  
   }  
   if (isValidateExistingTransaction()) {  
      if (definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT) {  
         Integer currentIsolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();  
         if (currentIsolationLevel == null || currentIsolationLevel != definition.getIsolationLevel()) {  
            Constants isoConstants = DefaultTransactionDefinition.constants;  
            throw new IllegalTransactionStateException("Participating transaction with definition [" +  
                  definition + "] specifies isolation level which is incompatible with existing transaction: " +  
                  (currentIsolationLevel != null ?  
                        isoConstants.toCode(currentIsolationLevel, DefaultTransactionDefinition.PREFIX_ISOLATION) :  
                        "(unknown)"));  
         }  
      }  
      if (!definition.isReadOnly()) {  
         if (TransactionSynchronizationManager.isCurrentTransactionReadOnly()) {  
            throw new IllegalTransactionStateException("Participating transaction with definition [" +  
                  definition + "] is not marked as read-only but existing transaction is");  
         }  
      }  
   }  
   boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);  
   return prepareTransactionStatus(definition, transaction, false, newSynchronization, debugEnabled, null);  
}
```

## 5.4 不存在外层事务，校验过期时间
如果外层方法不存在事务，需要校验内层方法设置的过期时间。如果过期时间小于`-1`，会抛出异常：
```java
if (def.getTimeout() < TransactionDefinition.TIMEOUT_DEFAULT) {  
   throw new InvalidTimeoutException("Invalid transaction timeout", def.getTimeout());  
}
```

## 5.5 不存在外层事务，内层方法事务传播行为是PROPAGATION_MANDATORY
如果外层方法不存在事务，但是内层方法设置的事务传播行为是`PROPAGATION_MANDATORY`，会抛出异常：
```java
if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_MANDATORY) {  
   throw new IllegalTransactionStateException(  
         "No existing transaction found for transaction marked with propagation 'mandatory'");  
}
```

## 5.6 不存在外层事务，内层方法事务传播行为是PROPAGATION_REQUIRED/PROPAGATION_REQUIRES_NEW/PROPAGATION_NESTED
如果外层方法不存在事务，同时内层方法设置的事务传播行为是`PROPAGATION_REQUIRED`、`PROPAGATION_REQUIRES_NEW`或`PROPAGATION_NESTED`，会创建一个新事务：
```java
else if (def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRED ||  
      def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_REQUIRES_NEW ||  
      def.getPropagationBehavior() == TransactionDefinition.PROPAGATION_NESTED) {  
   SuspendedResourcesHolder suspendedResources = suspend(null);  
   if (debugEnabled) {  
      logger.debug("Creating new transaction with name [" + def.getName() + "]: " + def);  
   }  
   try {  
      return startTransaction(def, transaction, debugEnabled, suspendedResources);  
   }  
   catch (RuntimeException | Error ex) {  
      resume(null, suspendedResources);  
      throw ex;  
   }  
}
```

## 5.7 不存在外层事务，内层方法事务为其他
如果外层方法不存在事务，同时内层方法设置的事务传播行为不是上述所有情况时，即当前事务传播行为是`PROPAGATION_SUPPORTS`、`PROPAGATION_NOT_SUPPORTED`或`PROPAGATION_NEVER`，实际上不会创建新事务：
```java
else {  
   // Create "empty" transaction: no actual transaction, but potentially synchronization.  
   if (def.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT && logger.isWarnEnabled()) {  
      logger.warn("Custom isolation level specified but no actual transaction initiated; " +  
            "isolation level will effectively be ignored: " + def);  
   }  
   boolean newSynchronization = (getTransactionSynchronization() == SYNCHRONIZATION_ALWAYS);  
   return prepareTransactionStatus(def, null, true, newSynchronization, debugEnabled, null);  
}
```

## 5.8 其他方法
在获取事务流程中，会调用一些比较重要的方法：
- `startTransaction()`：开始新事务。
- `suspend()`：暂停事务。
- `resume()`：恢复事务。
- `prepareSynchronization()`：保存`TransactionSynchronizationManager`中当前线程事务的状态信息。

### 1 startTransaction()
`startTransaction()`方法会开启一个新的物理事务：
1. 开启新的物理事务，并绑定到`TransactionSynchronizationManager`，交给子类实现。对于`DataSourceTransactionManager`，会调用`con.setAutoCommit(false);`方法。
2. 初始化`TransactionSynchronizationManager`中当前事务的信息。

方法源码如下：
```java
private TransactionStatus startTransaction(TransactionDefinition definition, Object transaction,  
      boolean debugEnabled, @Nullable SuspendedResourcesHolder suspendedResources) {  
  
   boolean newSynchronization = (getTransactionSynchronization() != SYNCHRONIZATION_NEVER);  
   DefaultTransactionStatus status = newTransactionStatus(  
         definition, transaction, true, newSynchronization, debugEnabled, suspendedResources);  
   // 开启新的物理事务，并绑定到TransactionSynchronizationManager，交给子类实现。对于DataSourceTransactionManager，会调用con.setAutoCommit(false)方法
   doBegin(transaction, definition);  
   // 保存当前事务信息
   prepareSynchronization(status, definition);  
   return status;  
}
```

### 2 suspend()
`suspend()`方法可以暂停指定事务，例如内层方法的事务传播行为是`PROPAGATION_NOT_SUPPORTED`或`PROPAGATION_REQUIRES_NEW`，就需要暂停外层方法的事务：
```java
protected final SuspendedResourcesHolder suspend(@Nullable Object transaction) throws TransactionException {  
   if (TransactionSynchronizationManager.isSynchronizationActive()) {  
      // 执行TransactionSynchronizationManager中的suspend()回调
      List<TransactionSynchronization> suspendedSynchronizations = doSuspendSynchronization();  
      try {  
         Object suspendedResources = null;  
         if (transaction != null) {  
            // 由子类去暂定事务
            suspendedResources = doSuspend(transaction);  
         }  
         // 清空被暂停事务在TransactionSynchronizationManager中的信息
         String name = TransactionSynchronizationManager.getCurrentTransactionName();  
         TransactionSynchronizationManager.setCurrentTransactionName(null);  
         boolean readOnly = TransactionSynchronizationManager.isCurrentTransactionReadOnly();  
         TransactionSynchronizationManager.setCurrentTransactionReadOnly(false);  
         Integer isolationLevel = TransactionSynchronizationManager.getCurrentTransactionIsolationLevel();  
         TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(null);  
         boolean wasActive = TransactionSynchronizationManager.isActualTransactionActive();  
         TransactionSynchronizationManager.setActualTransactionActive(false);  
         // 保存被暂停事务的信息，用于后续恢复事务
         return new SuspendedResourcesHolder(  
               suspendedResources, suspendedSynchronizations, name, readOnly, isolationLevel, wasActive);  
      }  
      catch (RuntimeException | Error ex) {  
         // doSuspend failed - original transaction is still active...  
         doResumeSynchronization(suspendedSynchronizations);  
         throw ex;  
      }  
   }  
   else if (transaction != null) {  
      // Transaction active but no synchronization active.  
      Object suspendedResources = doSuspend(transaction);  
      return new SuspendedResourcesHolder(suspendedResources);  
   }  
   else {  
      // Neither transaction nor synchronization active.  
      return null;  
   }  
}
```

对于`DataSourceTransactionManager`来说，它的`doSuspend()`方法会清除`txObject`对象的数据库连接，同时解除`TransactionSynchronizationManager`中当前事务的绑定：
```java
protected Object doSuspend(Object transaction) {  
   DataSourceTransactionObject txObject = (DataSourceTransactionObject) transaction;  
   txObject.setConnectionHolder(null);  
   return TransactionSynchronizationManager.unbindResource(obtainDataSource());  
}
```

### 3 resume()
如果暂停外层方法事务后，创建内层方法的事务失败了，需要恢复外层方法的事务。
如果内层方法的事务提交或回滚后，也需要恢复外层方法的事务。
```java
protected final void resume(@Nullable Object transaction, @Nullable SuspendedResourcesHolder resourcesHolder)  
      throws TransactionException {  
  
   if (resourcesHolder != null) {  
      Object suspendedResources = resourcesHolder.suspendedResources;  
      if (suspendedResources != null) {  
         // 由子类进行恢复事务
         doResume(transaction, suspendedResources);  
      }  
      List<TransactionSynchronization> suspendedSynchronizations = resourcesHolder.suspendedSynchronizations;  
      // 更新TransactionSynchronizationManager中当前事务的信息，执行其resume()回调
      if (suspendedSynchronizations != null) {  
         TransactionSynchronizationManager.setActualTransactionActive(resourcesHolder.wasActive);  
         TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(resourcesHolder.isolationLevel);  
         TransactionSynchronizationManager.setCurrentTransactionReadOnly(resourcesHolder.readOnly);  
         TransactionSynchronizationManager.setCurrentTransactionName(resourcesHolder.name);  
         doResumeSynchronization(suspendedSynchronizations);  
      }  
   }  
}
```

对于对于`DataSourceTransactionManager`来说，它的`duResume()`方法会重新在`TransactionSynchronizationManager`中绑定当前事务：
```java
protected void doResume(@Nullable Object transaction, Object suspendedResources) {  
   TransactionSynchronizationManager.bindResource(obtainDataSource(), suspendedResources);  
}
```

### 4 prepareSynchronization()
`prepareSynchronization()`方法用于更新`TransactionSynchronizationManager`中当前事务的信息：
```java
protected void prepareSynchronization(DefaultTransactionStatus status, TransactionDefinition definition) {  
   if (status.isNewSynchronization()) {  
      TransactionSynchronizationManager.setActualTransactionActive(status.hasTransaction());  
      TransactionSynchronizationManager.setCurrentTransactionIsolationLevel(  
            definition.getIsolationLevel() != TransactionDefinition.ISOLATION_DEFAULT ?  
                  definition.getIsolationLevel() : null);  
      TransactionSynchronizationManager.setCurrentTransactionReadOnly(definition.isReadOnly());  
      TransactionSynchronizationManager.setCurrentTransactionName(definition.getName());  
      TransactionSynchronizationManager.initSynchronization();  
   }  
}
```

# 6 提交事务流程
获取事务的入口在`PlatformTransactionManager#commit()`方法。

`AbstractPlatformTransactionManager#commit()`对该方法进行了实现：
```java
public final void commit(TransactionStatus status) throws TransactionException {  
   if (status.isCompleted()) {  
      throw new IllegalTransactionStateException(  
            "Transaction is already completed - do not call commit or rollback more than once per transaction");  
   }  
  
   DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;  
   // 根据rollbackOnly状态进行回滚
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
   // 具体提交事务流程
   processCommit(defStatus);  
}
```

`processCommit()`方法如下：
```java
private void processCommit(DefaultTransactionStatus status) throws TransactionException {  
   try {  
      boolean beforeCompletionInvoked = false;  
  
      try {  
         boolean unexpectedRollback = false;  
         prepareForCommit(status);  
         // 触发TransactionSynchronizationManager的beforeCommit()回调
         triggerBeforeCommit(status);  
         // 触发TransactionSynchronizationManager的beforeCompletion()回调
         triggerBeforeCompletion(status);  
         beforeCompletionInvoked = true;  
  
         if (status.hasSavepoint()) {  
            if (status.isDebug()) {  
               logger.debug("Releasing transaction savepoint");  
            }  
            unexpectedRollback = status.isGlobalRollbackOnly();  
            // 如果有保存点，请求数据库释放保存点
            status.releaseHeldSavepoint();  
         }  
         else if (status.isNewTransaction()) {  
            if (status.isDebug()) {  
               logger.debug("Initiating transaction commit");  
            }  
            unexpectedRollback = status.isGlobalRollbackOnly();  
            // 如果是新事务，请求数据库提交事务
            doCommit(status);  
         }  
         else if (isFailEarlyOnGlobalRollbackOnly()) {  
            unexpectedRollback = status.isGlobalRollbackOnly();  
         }  
  
         // Throw UnexpectedRollbackException if we have a global rollback-only  
         // marker but still didn't get a corresponding exception from commit.         if (unexpectedRollback) {  
            throw new UnexpectedRollbackException(  
                  "Transaction silently rolled back because it has been marked as rollback-only");  
         }  
      }  
      catch (UnexpectedRollbackException ex) {  
         // can only be caused by doCommit  
         triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);  
         throw ex;  
      }  
      catch (TransactionException ex) {  
         // can only be caused by doCommit  
         if (isRollbackOnCommitFailure()) {  
            doRollbackOnCommitException(status, ex);  
         }  
         else {  
            triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);  
         }  
         throw ex;  
      }  
      catch (RuntimeException | Error ex) {  
         if (!beforeCompletionInvoked) {  
            triggerBeforeCompletion(status);  
         }  
         doRollbackOnCommitException(status, ex);  
         throw ex;  
      }  
     try {  
         // 触发TransactionSynchronizationManager的afterCommit()回调
         triggerAfterCommit(status);  
      }  
      finally {  
         // 触发TransactionSynchronizationManager的afterCompletion()回调
         triggerAfterCompletion(status, TransactionSynchronization.STATUS_COMMITTED);  
      }  
  
   }  
   finally {  
      // 清除TransactionSynchronizationManager中当前线程事务的信息
      cleanupAfterCompletion(status);  
   }  
}
```

需要注意的是，在`cleanupAfterCompletion()`方法中对事务是否需要恢复进行的处理：
```java
private void cleanupAfterCompletion(DefaultTransactionStatus status) {  
   status.setCompleted();  
   if (status.isNewSynchronization()) {  
      TransactionSynchronizationManager.clear();  
   }  
   if (status.isNewTransaction()) {  
      doCleanupAfterCompletion(status.getTransaction());  
   }  
   if (status.getSuspendedResources() != null) {  
      if (status.isDebug()) {  
         logger.debug("Resuming suspended transaction after completion of inner transaction");  
      }  
      Object transaction = (status.hasTransaction() ? status.getTransaction() : null);  
      // 恢复方法外层的事务
      resume(transaction, (SuspendedResourcesHolder) status.getSuspendedResources());  
   }  
}
```

# 7 回滚事务流程
获取事务的入口在`PlatformTransactionManager#rollback()`方法。

`AbstractPlatformTransactionManager#rollback()`对该方法进行了实现：
```java
public final void rollback(TransactionStatus status) throws TransactionException {  
   if (status.isCompleted()) {  
      throw new IllegalTransactionStateException(  
            "Transaction is already completed - do not call commit or rollback more than once per transaction");  
   }  
  
   DefaultTransactionStatus defStatus = (DefaultTransactionStatus) status;  
   // 具体事务回滚
   processRollback(defStatus, false);  
}
```

`processRollback()`方法如下：
```java
private void processRollback(DefaultTransactionStatus status, boolean unexpected) {  
   try {  
      boolean unexpectedRollback = unexpected;  
  
      try {  
         // 触发TransactionSynchronizationManager的beforeCompletion()回调
         triggerBeforeCompletion(status);  
  
         if (status.hasSavepoint()) {  
            if (status.isDebug()) {  
               logger.debug("Rolling back transaction to savepoint");  
            }  
            // 如果有保存点，请求数据库回滚到保存点
            status.rollbackToHeldSavepoint();  
         }  
         else if (status.isNewTransaction()) {  
            if (status.isDebug()) {  
               logger.debug("Initiating transaction rollback");  
            }  
            // 如果是单纯新事务，请求数据库回滚事务
            doRollback(status);  
         }  
         else {  
            // 如果加入到外层方法的事务中  
            if (status.hasTransaction()) {  
               if (status.isLocalRollbackOnly() || isGlobalRollbackOnParticipationFailure()) {  
                  if (status.isDebug()) {  
                     logger.debug("Participating transaction failed - marking existing transaction as rollback-only");  
                  }  
                  // 将外层方法的事务设为rollbackOnly，由外层事务提交时进行回滚
                  doSetRollbackOnly(status);  
               }  
               else {  
                  if (status.isDebug()) {  
                     logger.debug("Participating transaction failed - letting transaction originator decide on rollback");  
                  }  
               }  
            }  
            else {  
               logger.debug("Should roll back transaction but cannot - no transaction available");  
            }  
            // Unexpected rollback only matters here if we're asked to fail early  
            if (!isFailEarlyOnGlobalRollbackOnly()) {  
               unexpectedRollback = false;  
            }  
         }  
      }  
      catch (RuntimeException | Error ex) {  
         triggerAfterCompletion(status, TransactionSynchronization.STATUS_UNKNOWN);  
         throw ex;  
      }  
      // 触发TransactionSynchronizationManager的afterCompletion()回调
      triggerAfterCompletion(status, TransactionSynchronization.STATUS_ROLLED_BACK);  
  
      // Raise UnexpectedRollbackException if we had a global rollback-only marker  
      if (unexpectedRollback) {  
         throw new UnexpectedRollbackException(  
               "Transaction rolled back because it has been marked as rollback-only");  
      }  
   }  
   finally {  
      // 清除TransactionSynchronizationManager中当前事务的信息
      cleanupAfterCompletion(status);  
   }  
}
```

# 8 总结
Spring事务管理本质上就是`PlatformTransactionManager`中的3个方法：
1. `getTransaction(TransactionDefinition):TransactionStatus`：获取事务。
2. `commit(TransactionStatus)`：提交事务。
3. `rollback(TransactionStatus)`：回滚事务。

如果我们要使用`PlatformTransactionManager`进行事务管理，也非常简单：
```java
PlatformTransactionManager txManager = new JdbcTransactionManager(dataSource);
DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
TransactionStatus txStatus = txManager.getTransaction(txDefinition);
try {
	// 业务方法
} catch (Exception e) {
	txManager.rollback(txStatus);
	throw e;
}
txManager.commit(status);
```

`TransactionSynchronizationManager`贯穿整个事务管理的流程，我们可以通过其实时获取当前事务的信息，也可以添加各个回调来对事务管理流程进行拦截。