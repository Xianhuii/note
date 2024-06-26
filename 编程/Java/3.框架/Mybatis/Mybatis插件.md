# 1 插件的作用
在Mybatis执行SQL的生命周期中，会使用插件进行埋点，主要包括Executor、StatementHandler、ParameterHandler和ResultSetHandler等。在执行到这些特殊节点时，就会触发拦截器的拦截方法。

通过自定义插件，我们可以对这些核心的节点中进行特殊处理，主要应用场景包括分页、记录日志、加解密等。

# 2 插件的工作原理
Mybatis插件的核心类包括：
- Interceptor：拦截器
- Interceptors和Signature：拦截信息注解，指定需要拦截的类和方法
- InterceptorChain：拦截器集合
- Plugin：创建动态代理对象的工具类
- Invocation：封装被代理对象、执行方法和参数信息

Mybatis插件的工作流程如下：
1. 通过实现Interceptor接口自定义拦截器，并使用@Interceptors和@Signature注解指定需要拦截的类和方法
2. 将自定义拦截器添加到Mybatis配置
3. 在Mybatis启动时，会使用InterceptorChain保存配置的所有拦截器
4. 在Mybatis执行SQL时，会读取InterceptorChain，使用Plugin对Executor/StatementHandler/ParameterHandler/ResultSetHandler进行动态代理
5. 在执行代理对象方法时，会将被代理对象、当前执行方法和参数信息封装成Invocation，传递给拦截器

创建Executor/StatementHandler/ParameterHandler/ResultSetHandler代理对象的方法都位于Configuration：
```java
// org.apache.ibatis.session.Configuration#newExecutor
public Executor newExecutor(Transaction transaction, ExecutorType executorType) {
  executorType = executorType == null ? defaultExecutorType : executorType;
  executorType = executorType == null ? ExecutorType.SIMPLE : executorType;
  Executor executor;
  if (ExecutorType.BATCH == executorType) {
    executor = new BatchExecutor(this, transaction);
  } else if (ExecutorType.REUSE == executorType) {
    executor = new ReuseExecutor(this, transaction);
  } else {
    executor = new SimpleExecutor(this, transaction);
  }
  if (cacheEnabled) {
    executor = new CachingExecutor(executor);
  }
  // 使用拦截器进行动态代理
  executor = (Executor) interceptorChain.pluginAll(executor);
  return executor;
}
// org.apache.ibatis.session.Configuration#newStatementHandler
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
  // 使用拦截器进行动态代理
  statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
  return statementHandler;
}
// org.apache.ibatis.session.Configuration#newParameterHandler
public ParameterHandler newParameterHandler(MappedStatement mappedStatement, Object parameterObject, BoundSql boundSql) {
  ParameterHandler parameterHandler = mappedStatement.getLang().createParameterHandler(mappedStatement, parameterObject, boundSql);
  // 使用拦截器进行动态代理
  parameterHandler = (ParameterHandler) interceptorChain.pluginAll(parameterHandler);
  return parameterHandler;
}
// org.apache.ibatis.session.Configuration#newResultSetHandler
public ResultSetHandler newResultSetHandler(Executor executor, MappedStatement mappedStatement, RowBounds rowBounds, ParameterHandler parameterHandler,
    ResultHandler resultHandler, BoundSql boundSql) {
  ResultSetHandler resultSetHandler = new DefaultResultSetHandler(executor, mappedStatement, parameterHandler, resultHandler, boundSql, rowBounds);
  // 使用拦截器进行动态代理
  resultSetHandler = (ResultSetHandler) interceptorChain.pluginAll(resultSetHandler);
  return resultSetHandler;
}
```

接下来我们来看创建Executor/StatementHandler/ParameterHandler/ResultSetHandler对象&进行代理的节点。

在使用SqlSessionFactory#openSession创建SqlSEession时，会创建Executor对象，并进行代理：
```java
// org.apache.ibatis.session.defaults.DefaultSqlSessionFactory#openSessionFromDataSource
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
  Transaction tx = null;
  try {
    final Environment environment = configuration.getEnvironment();
    final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
    tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
	// 创建Executor
    final Executor executor = configuration.newExecutor(tx, execType);
    return new DefaultSqlSession(configuration, executor, autoCommit);
  } catch (Exception e) {
    closeTransaction(tx); // may have fetched a connection so lets call close()
    throw ExceptionFactory.wrapException("Error opening session.  Cause: " + e, e);
  } finally {
    ErrorContext.instance().reset();
  }
}
```

在Executor执行SQL时，会创建StatementHandler，并进行代理：
```java
// org.apache.ibatis.executor.SimpleExecutor#doQuery
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
	// 创建StatementHandler
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    stmt = prepareStatement(handler, ms.getStatementLog());
    return handler.query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```

ParameterHandler和ResultSetHandler作为StatementHandler的成员变量存在，会在其构造函数中进行创建和代理：
```java
// BaseStatementHandler构造函数
protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  // ……
  this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
  this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
}
```

接下来我们来看InterceptorChain如何使用拦截器集合对Executor/StatementHandler/ParameterHandler/ResultSetHandler对象进行代理。

InterceptorChain会遍历拦截器集合，进行一层一层代理：
```java
// org.apache.ibatis.plugin.InterceptorChain#pluginAll
public Object pluginAll(Object target) {
  for (Interceptor interceptor : interceptors) {
    target = interceptor.plugin(target);
  }
  return target;
}
```

每一次代理都会调用Plugin#wrap，它只是对JDK动态代理进行了简单应用：
```java
// org.apache.ibatis.plugin.Interceptor#plugin
default Object plugin(Object target) {
  return Plugin.wrap(target, this);
}
// org.apache.ibatis.plugin.Plugin#wrap
public static Object wrap(Object target, Interceptor interceptor) {
  // 获取拦截配置信息
  Map<Class<?>, Set<Method>> signatureMap = getSignatureMap(interceptor);
  Class<?> type = target.getClass();
  Class<?>[] interfaces = getAllInterfaces(type, signatureMap);
  if (interfaces.length > 0) {
    return Proxy.newProxyInstance(
        type.getClassLoader(),
        interfaces,
        new Plugin(target, interceptor, signatureMap));
  }
  return target;
}
```
- target：被代理对象，Executor、StatementHandler、ParameterHandler或ResultSetHandler对象
- interfaces：被代理对象所实现的接口，Executor、StatementHandler、ParameterHandler或ResultSetHandler接口
- interceptor：拦截器
- signatureMap：拦截方法信息（哪些方法需要拦截）

Plugin本身实现了InvocationHandler方法，其中就定义了代理逻辑，主要会根据配置判断是否需要进行拦截，并执行对应方法：
```java
// org.apache.ibatis.plugin.Plugin#invoke
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  try {
  	// 从当前拦截器配置信息中获取当前方法的拦截信息
    Set<Method> methods = signatureMap.get(method.getDeclaringClass());
    // 如果存在拦截配置，执行拦截器的拦截方法
	if (methods != null && methods.contains(method)) {
      return interceptor.intercept(new Invocation(target, method, args));
    }
	// 如果不存在拦截配置，执行原始方法
    return method.invoke(target, args);
  } catch (Exception e) {
    throw ExceptionUtil.unwrapThrowable(e);
  }
}
```

上述拦截配置信息来自于Interceptor实现类上的@Intercepts和@Signature注解，通过Signature#type指定需要拦截的类，通过Signature#method和Signature#args共同指定需要拦截的方法：
```java
// org.apache.ibatis.plugin.Plugin#getSignatureMap
private static Map<Class<?>, Set<Method>> getSignatureMap(Interceptor interceptor) {
  // 获取@Intercepts注解
  Intercepts interceptsAnnotation = interceptor.getClass().getAnnotation(Intercepts.class);
  if (interceptsAnnotation == null) {
    throw new PluginException("No @Intercepts annotation was found in interceptor " + interceptor.getClass().getName());
  }
  // 获取@Sinature注解
  Signature[] sigs = interceptsAnnotation.value();
  Map<Class<?>, Set<Method>> signatureMap = new HashMap<>();
  // 获取需要拦截的类（type）、方法（method和args）
  for (Signature sig : sigs) {
    Set<Method> methods = MapUtil.computeIfAbsent(signatureMap, sig.type(), k -> new HashSet<>());
    try {
      Method method = sig.type().getMethod(sig.method(), sig.args());
      methods.add(method);
    } catch (NoSuchMethodException e) {
      throw new PluginException("Could not find method on " + sig.type() + " named " + sig.method() + ". Cause: " + e, e);
    }
  }
  return signatureMap;
}
```

# 3 自定义拦截器
自定义拦截器主要有两个步骤：
1. 创建拦截器：实现Interceptor接口，标注@Intercepts和@Signature注解
2. 注册拦截器：添加拦截器到Mybatis配置

## 3.1 创建拦截器
创建拦截器只需要实现Interceptor接口：
```java
public class CustomInterceptor implements Interceptor {
    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        // 拦截业务处理
        return null;
    }
}
```

如果仅仅是进行切面处理（如记录日志），要记得执行代理对象的代理方法：
```java
public Object intercept(Invocation invocation) throws Throwable {
    // before……
    // 获取代理信息
    Object target = invocation.getTarget();
    Method method = invocation.getMethod();
    Object[] args = invocation.getArgs();
    Object result = method.invoke(target, args);
    // after……
    return result;
}
```

如果需要执行自定义逻辑，甚至可以不执行代理对象的代理方法，完全由我们自己定义业务逻辑。

我们还需要指定需要拦截的类和方法，例如如果要拦截org.apache.ibatis.executor.Executor#query(org.apache.ibatis.mapping.MappedStatement, java.lang.Object, org.apache.ibatis.session.RowBounds, org.apache.ibatis.session.ResultHandler)方法，我们可以添加如下注解：
```java
@Intercepts(
    {
        @Signature(type = Executor.class, method = "query", args = {MappedStatement.class, Object.class, RowBounds.class, ResultHandler.class})
    }
)
```

我们还可以配置多个拦截类和方法，既可以是同一个类，也可以是不同类。

但是通常不推荐&不会为一个拦截器配置多个拦截类，因为这样会造成代码逻辑混乱，职责不明确。

创建拦截器很简单，但是最重要的是要**选择适合的需要拦截的类和方法**。

因为Executor/StatementHandler/ParameterHandler/ResultSetHandler的方法很多，在Mybatis执行SQL过程中，有些方法可能不会被触发。

这就对开发人员有两个要求：
1. 熟悉Mybatis执行SQL流程
2. 明确拦截业务需求

## 3.2 注册拦截器
注册拦截器，本质上是需要将自定义的拦截器添加到Mybatis的配置信息中（InterceptorChain）。

对于原生Mybatis或Mybatis-Spring场景中，可以直接使用Configuration#addInterceptor方法：
```java
CustomInterceptor customInterceptor = new CustomInterceptor();
configuration.addInterceptor(customInterceptor);
```

如果使用Mybatis-SpringBoot框架，则只需要将拦截器注册为Bean添加到Spring容器中：
1. 直接添加@Component注解
2. 使用@Bean添加

在自动配置过程中，会按以下流程注册拦截器：
1. 读取容器中的Interceptor Bean对象
2. 添加到SqlSessionFactoryBean
3. 注册到Configuration

```java
// org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration#MybatisAutoConfiguration
public MybatisAutoConfiguration(MybatisProperties properties, ObjectProvider<Interceptor[]> interceptorsProvider, ObjectProvider<TypeHandler[]> typeHandlersProvider, ObjectProvider<LanguageDriver[]> languageDriversProvider, ResourceLoader resourceLoader, ObjectProvider<DatabaseIdProvider> databaseIdProvider, ObjectProvider<List<ConfigurationCustomizer>> configurationCustomizersProvider, ObjectProvider<List<SqlSessionFactoryBeanCustomizer>> sqlSessionFactoryBeanCustomizers) {
    // 1、读取容器中的Interceptor Bean对象
    this.interceptors = (Interceptor[])interceptorsProvider.getIfAvailable();
    // ……
}
// org.mybatis.spring.boot.autoconfigure.MybatisAutoConfiguration#sqlSessionFactory
public SqlSessionFactory sqlSessionFactory(DataSource dataSource) throws Exception {
    SqlSessionFactoryBean factory = new SqlSessionFactoryBean();
    if (!ObjectUtils.isEmpty(this.interceptors)) {
        // 2、添加到SqlSessionFactoryBean
        factory.setPlugins(this.interceptors);
    }
    // ……
}

protected SqlSessionFactory buildSqlSessionFactory() throws Exception {
  if (!isEmpty(this.plugins)) {
    // 3、注册到Configuration
    Stream.of(this.plugins).forEach(plugin -> {
      targetConfiguration.addInterceptor(plugin);
    });
  }
}
```

# 4 开源框架案例
基于Mybatis插件扩展的开源框架比较少，最常用、最热门的应该是PageHelper。

GitHub：https://github.com/pagehelper/Mybatis-PageHelper

它的原理是自定义了拦截器：com.github.pagehelper.PageInterceptor

在设置分页信息时，会将分页信息添加到线程变量中：
```java
PageHelper.startPage(pageNum, pageSize);
// com.github.pagehelper.page.PageMethod#setLocalPage
protected static void setLocalPage(Page page) {
    LOCAL_PAGE.set(page);
}
// com.github.pagehelper.page.PageMethod#LOCAL_PAGE
protected static final ThreadLocal<Page> LOCAL_PAGE = new ThreadLocal<Page>();
```

在执行org.apache.ibatis.executor.Executor#query方法时，会触发该拦截器，如果线程变量中存在分页信息，进行分页逻辑。主要流程如下：
1. 查询总数
2. 查询分页
3. 封装响应
4. 清除线程变量的分页信息
