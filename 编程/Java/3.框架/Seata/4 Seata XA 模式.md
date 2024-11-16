# 1 执行原理
XA 规范 是 X/Open 组织定义的分布式事务处理（DTP，Distributed Transaction Processing）标准。Seata XA 模式是利用事务资源（数据库、消息服务等）对 XA 协议的支持，以 XA 协议的机制来管理分支事务的一种事务模式。
- 执行阶段：
    - 可回滚：业务 SQL 操作放在 XA 分支中进行，由资源对 XA 协议的支持来保证 _可回滚_
    - 持久化：XA 分支完成后，执行 XA prepare，同样，由资源对 XA 协议的支持来保证 _持久化_（即，之后任何意外都不会造成无法回滚的情况）
- 完成阶段：
    - 分支提交：执行 XA 分支的 commit
    - 分支回滚：执行 XA 分支的 rollback
# 2 基本使用
XA 模式使用起来与 AT 模式基本一致，用法上的唯一区别在于数据源代理的替换：使用 `DataSourceProxyXA` 来替代 `DataSourceProxy`。
```java
public class DataSourceProxy {
    @Bean("dataSourceProxy")
    public DataSource dataSource(DruidDataSource druidDataSource) {
        // DataSourceProxyXA for XA mode
        return new DataSourceProxyXA(druidDataSource);
        // DataSourceProxy for AT mode
        // return new DataSourceProxy(druidDataSource);
    }
}
```
# 3 源码
- DataSourceProxyXA
- ConnectionProxyXA
- ConnectionProxyXA
- StatementProxyXA
- PreparedStatementProxyXA
