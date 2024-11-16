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
	- GlobalTransactionalInterceptorHandler：