# 1 执行原理
Seata 在内部做了对数据库操作的代理层，我们使用 Seata AT 模式时，实际上用的是 Seata 自带的数据源代理 DataSourceProxy，Seata 在这层代理中加入了很多逻辑，比如插入回滚 undo_log 日志，检查全局锁等。
两阶段提交协议的演变：
- 一阶段：业务数据和回滚日志记录在同一个本地事务中提交，释放本地锁和连接资源。
- 二阶段：
    - 提交异步化，非常快速地完成。
    - 回滚通过一阶段的回滚日志进行反向补偿。
# 2 基本使用
```java
@GlobalTransactional
public void purchase(String userId, String commodityCode, int count, int money) {
    jdbcTemplateA.update("update stock_tbl set count = count - ? where commodity_code = ?", new Object[] {count, commodityCode});
    jdbcTemplateB.update("update account_tbl set money = money - ? where user_id = ?", new Object[] {money, userId});
}
```
# 3 源码
## 3.1 核心组成
AT模式由以下部分组成：
- `@GlobalTransactional`：
	- AspectTransactionalInterceptor：aop拦截器，对注解标识的方法进行拦截
	- GlobalTransactionalInterceptorHandler：代理执行事务逻辑
- TransactionalTemplate：事务执行逻辑的模板工具
	1.  获取事务上下文信息TransactionInfo
	2. 获取当前事务GlobalTransaction
	3. 处理事务传播机制
	4. 开始事务，触发TransactionHook
	5. 执行业务方法：即标注@GlobalTransactional的方法
	6. 捕获异常，进行回滚，触发TransactionHook
	7. 正常执行，提交事务，触发TransactionHook
	8. 释放资源，触发TransactionHook
- DefaultGlobalTransaction：事务方法的封装，包括begin、commit、rollback等，实际执行交给TransactionManager处理
- DefaultCoordinator：事务协调器（TC），负责接收通知，然后转发给资源管理器进行处理
- DefaultTransactionManager：事务管理器（TM），将事务方法通过RPC请求发给远程事务协调器
- DefaultResourceManager：资源管理器（RM），在收到通知时进行加锁、提交、回滚等
- GlobalTransactionRole：当前事务在全局事务中的角色
	- Launcher：全局事务发起者，即第一个创建事务的线程，发起者在执行begin、commit、rollback等时会请求远程事务协调者
	- Participant：全局事务参与者，即后续加入事务的线程
- DataSourceProxy：数据源代理
- ConnectionProxy：连接代理
	- commit：
		1. 获请求资源管理器，获取全局锁
		2. 插入undo_log日志，提交事务
		3. 释放资源
	- rollback
		- 回滚事务
		- 通知资源管理器
		- 释放资源
- StatementProxy、PreparedStatementProxy：执行语句代理，实际由ExecuteTemplate执行
- ExecuteTemplate：执行语句的模板方法，根据语句类型不同交给对应Executor实现了执行

整体流程TransactionalTemplate：
1. 获取事务上下文信息，设置配置参数
2. 获取当前事务（null或Participant）
3. 处理事务传播，即多个事务方法嵌套的场景。此时如果当前事务为null，可能会在此创建新事务（Launcher）。
4. 保存当前事务上下文信息，并弹出上个事务的上下文配置
5. 开始事务：
	1. Launcher：调用TM的begin方法，请求TC创建全局事务会话，获取xId
	2. Participant：不处理
6. 执行业务方法：
	1. 获取数据源DataSourceProxy
	2. 获取连接ConnectionProxy
	3. 创建语句PreparedStatement
	4. 执行语句ExecuteTemplate
7. 本地事务异常，进行回滚：
	1. Launcher：重试调用TM的rollback方法，请求TC回滚全局事务，转发到RM进行回滚事务（执行undo_log日志）
	2. Participant：不处理
8. 正常执行，进行提交：
	1. Launcher：重试调用TM的commit方法，请求TC提交全局事务，转发到RM进行提交事务（删除undo_log日志）
	2. Participant：不处理
9. 恢复上个事务的上下文配置
10. 释放当前事务上下文资源