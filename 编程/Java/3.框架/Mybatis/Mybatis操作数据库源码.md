Java操作数据库需要经过3个大步骤：
1. 获取数据库连接
2. 执行SQL语句
3. 关闭数据库连接

Mybatis将这几个步骤进行了封装，将获取数据库连接的给工作交给了SqlSessionFactory，将执行SQL的工作交给了SqlSession。

# 1 获取SqlSession
在程序启动时，会根据配置创建SqlSessionFactory：
```java
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder().build(configuration);
```

SqlSessionFactory是一个工厂，在整个程序中应该是单例的存在。它会保存所有配置信息，需要操作数据库时，通过调用SqlSessionFactory#openSession()方法，可以根据配置信息生成新的SqlSession。

SqlSessionFactory#openSessionFromDataSource()展示了创建SqlSession的流程，本质上只是创建了Executor对象，并对其进行了封装：
```java
private SqlSession openSessionFromDataSource(ExecutorType execType, TransactionIsolationLevel level, boolean autoCommit) {
  Transaction tx = null;
  try {
    final Environment environment = configuration.getEnvironment();
    final TransactionFactory transactionFactory = getTransactionFactoryFromEnvironment(environment);
    tx = transactionFactory.newTransaction(environment.getDataSource(), level, autoCommit);
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

需要注意的是，Configuration#newExecutor()方法会对执行器进行缓存代理和插件代理。默认情况下，生成从外到内Interceptor -> CachingExecutor -> SimpleExecutor的层级结构：
```java
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
  executor = (Executor) interceptorChain.pluginAll(executor);
  return executor;
}
```

# 2 执行SQL语句
Mybatis提供了两种执行SQL的方式：
1. 根据statementId执行
2. 使用映射接口代理对象执行

## 2.1 根据statementId执行

SqlSession提供了selectXxx/insert/update/delete等方法操作数据库。

默认情况下，DefaultSqlSession会将selectXxx（除了selectCursor）方法统一交给`selectList`方法处理，将insert/update/delete方法统一交给`update`方法处理。它们的处理方式相同：
1. 从Configuration中获取对应的MappedStatement
2. 使用Executor执行SQL

DefaultSqlSession#selectList：
```java
private <E> List<E> selectList(String statement, Object parameter, RowBounds rowBounds, ResultHandler handler) {
  try {
    MappedStatement ms = configuration.getMappedStatement(statement);
    return executor.query(ms, wrapCollection(parameter), rowBounds, handler);
  } catch (Exception e) {
    throw ExceptionFactory.wrapException("Error querying database.  Cause: " + e, e);
  } finally {
    ErrorContext.instance().reset();
  }
}
```

DefaultSqlSession#update：
```java
public int update(String statement, Object parameter) {
  try {
    dirty = true;
    MappedStatement ms = configuration.getMappedStatement(statement);
    return executor.update(ms, wrapCollection(parameter));
  } catch (Exception e) {
    throw ExceptionFactory.wrapException("Error updating database.  Cause: " + e, e);
  } finally {
    ErrorContext.instance().reset();
  }
}
```

### 2.2.1 Executor#selectList
除了自定义Interceptor，首先会进入到CachingExecutor#query方法，它主要对全局缓存进行了处理：
1. 根据查询条件（包括id、offset、limit、sql、查询参数和environmentId）创建CacheKey
2. 获取MappedStatement的缓存对象Cache
3. 从Cache中获取查询条件对应的缓存：如果有缓存，直接返回；如果没有缓存，进一步查询，并添加缓存

CachingExecutor#query：
```java
public <E> List<E> query(MappedStatement ms, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql)
      throws SQLException {
  Cache cache = ms.getCache();
  if (cache != null) {
    flushCacheIfRequired(ms);
    if (ms.isUseCache() && resultHandler == null) {
      ensureNoOutParams(ms, boundSql);
      @SuppressWarnings("unchecked")
      List<E> list = (List<E>) tcm.getObject(cache, key);
      if (list == null) {
        list = delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
        tcm.putObject(cache, key, list); // issue #578 and #116
      }
      return list;
    }
  }
  return delegate.query(ms, parameterObject, rowBounds, resultHandler, key, boundSql);
}
```

之所以说CachingExecutor#query方法处理的是全局缓存，是因为这个缓存存在于MappedStatement，是所有会话共享的对象。

接着，会执行BaseExecutor#query方法，它主要对会话缓存进行处理：
1. 从执行器localCache中获取查询条件对应的缓存
2. 如果有缓存，直接返回；如果没有缓存，进一步查询数据库，并添加缓存

之所以说BaseExecutor#query方法处理的是会话缓存，是应为这个缓存存在于BaseExecutor，只有当前SqlSession才可以访问。

BaseExecutor#query和BaseExecutor#queryFromDatabase：
```java
public <E> List<E> query(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
  ErrorContext.instance().resource(ms.getResource()).activity("executing a query").object(ms.getId());
  if (closed) {
    throw new ExecutorException("Executor was closed.");
  }
  if (queryStack == 0 && ms.isFlushCacheRequired()) {
    clearLocalCache();
  }
  List<E> list;
  try {
    queryStack++;
    list = resultHandler == null ? (List<E>) localCache.getObject(key) : null;
    if (list != null) {
      handleLocallyCachedOutputParameters(ms, key, parameter, boundSql);
    } else {
      list = queryFromDatabase(ms, parameter, rowBounds, resultHandler, key, boundSql);
    }
  } finally {
    queryStack--;
  }
  if (queryStack == 0) {
    for (DeferredLoad deferredLoad : deferredLoads) {
      deferredLoad.load();
    }
    deferredLoads.clear();
    if (configuration.getLocalCacheScope() == LocalCacheScope.STATEMENT) {
      clearLocalCache();
    }
  }
  return list;
}

private <E> List<E> queryFromDatabase(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, CacheKey key, BoundSql boundSql) throws SQLException {
  List<E> list;
  localCache.putObject(key, EXECUTION_PLACEHOLDER);
  try {
    list = doQuery(ms, parameter, rowBounds, resultHandler, boundSql);
  } finally {
    localCache.removeObject(key);
  }
  localCache.putObject(key, list);
  if (ms.getStatementType() == StatementType.CALLABLE) {
    localOutputParameterCache.putObject(key, parameter);
  }
  return list;
}
```

具体查询数据库逻辑交给子类实现，默认SimpleExecutor执行步骤如下：
1. 创建StatementHandler
2. 创建Statement（参数解析）
3. 执行Statement
4. 结果集封装

SimpleExecutor#doQuery：
```java
public <E> List<E> doQuery(MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler
    StatementHandler handler = configuration.newStatementHandler(wrapper, ms, parameter, rowBounds, resultHandler, boundSql);
    // 创建Statement（参数解析）
    stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行Statement&结果集封装
    return handler.query(stmt, resultHandler);
  } finally {
    closeStatement(stmt);
  }
}
```

默认会创建RoutingStatementHandler，它只是一个代理对象，会根据MappedStatement创建具体SimpleStatementHandler/PreparedStatementHandler/CallableStatementHandler实现类。

在创建StatementHandler过程中，还会使用Interceptor进行代理，因此最终返回的对象是Interceptor -> RoutingStatementHandler -> 具体实现类的层级结构。

默认情况下，创建的StatementHandler具体实现类一般是PreparedStatementHandler。

BaseStatementHandler是SimpleStatementHandler/PreparedStatementHandler/CallableStatementHandler的共同父类，它的构造函数会创建ParameterHander和ResultSetHandler对象（并使用插件代理），用于参数解析和结果集封装。

Configuration#newStatementHandler、RoutingStatementHandler构造函数和BaseStatementHandler构造函数：
```java
public StatementHandler newStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  // 创建RoutingStatementHandler
  StatementHandler statementHandler = new RoutingStatementHandler(executor, mappedStatement, parameterObject, rowBounds, resultHandler, boundSql);
  // 使用插件代理
  statementHandler = (StatementHandler) interceptorChain.pluginAll(statementHandler);
  return statementHandler;
}

public RoutingStatementHandler(Executor executor, MappedStatement ms, Object parameter, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  switch (ms.getStatementType()) {
    case STATEMENT:
      delegate = new SimpleStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
      break;
    case PREPARED:
      delegate = new PreparedStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
      break;
    case CALLABLE:
      delegate = new CallableStatementHandler(executor, ms, parameter, rowBounds, resultHandler, boundSql);
      break;
    default:
      throw new ExecutorException("Unknown statement type: " + ms.getStatementType());
  }
}

protected BaseStatementHandler(Executor executor, MappedStatement mappedStatement, Object parameterObject, RowBounds rowBounds, ResultHandler resultHandler, BoundSql boundSql) {
  this.configuration = mappedStatement.getConfiguration();
  this.executor = executor;
  this.mappedStatement = mappedStatement;
  this.rowBounds = rowBounds;
  this.typeHandlerRegistry = configuration.getTypeHandlerRegistry();
  this.objectFactory = configuration.getObjectFactory();
  if (boundSql == null) { // issue #435, get the key before calculating the statement
    generateKeys(parameterObject);
    boundSql = mappedStatement.getBoundSql(parameterObject);
  }
  this.boundSql = boundSql;
  this.parameterHandler = configuration.newParameterHandler(mappedStatement, parameterObject, boundSql);
  this.resultSetHandler = configuration.newResultSetHandler(executor, mappedStatement, rowBounds, parameterHandler, resultHandler, boundSql);
}
```

SimpleExecutor#prepareStatement会创建Statement，并设置参数：
```java
private Statement prepareStatement(StatementHandler handler, Log statementLog) throws SQLException {
  Statement stmt;
  Connection connection = getConnection(statementLog);
  // 创建Statement
  stmt = handler.prepare(connection, transaction.getTimeout());
  // 设置参数
  handler.parameterize(stmt);
  return stmt;
}
```

设置参数时，除了自定义Interceptor，默认会执行PreparedStatementHandler#parameterize方法，实际上会调用ParameterHandler（默认是DefaultParameterHandler）进行处理：
1. 获取参数映射配置信息
2. 遍历参数映射配置信息，使用TypeHandler设置参数


PreparedStatementHandler#parameterize、DefaultParameterHandler#setParameters和BaseTypeHandler#setParameter：
```java
public void parameterize(Statement statement) throws SQLException {
  parameterHandler.setParameters((PreparedStatement) statement);
}

public void setParameters(PreparedStatement ps) {
  ErrorContext.instance().activity("setting parameters").object(mappedStatement.getParameterMap().getId());
  List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
  if (parameterMappings != null) {
    for (int i = 0; i < parameterMappings.size(); i++) {
      ParameterMapping parameterMapping = parameterMappings.get(i);
      if (parameterMapping.getMode() != ParameterMode.OUT) {
        Object value;
        String propertyName = parameterMapping.getProperty();
        if (boundSql.hasAdditionalParameter(propertyName)) { // issue #448 ask first for additional params
          value = boundSql.getAdditionalParameter(propertyName);
        } else if (parameterObject == null) {
          value = null;
        } else if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
          value = parameterObject;
        } else {
          MetaObject metaObject = configuration.newMetaObject(parameterObject);
          value = metaObject.getValue(propertyName);
        }
        TypeHandler typeHandler = parameterMapping.getTypeHandler();
        JdbcType jdbcType = parameterMapping.getJdbcType();
        if (value == null && jdbcType == null) {
          jdbcType = configuration.getJdbcTypeForNull();
        }
        try {
          typeHandler.setParameter(ps, i + 1, value, jdbcType);
        } catch (TypeException | SQLException e) {
          throw new TypeException("Could not set parameters for mapping: " + parameterMapping + ". Cause: " + e, e);
        }
      }
    }
  }
}

public void setParameter(PreparedStatement ps, int i, T parameter, JdbcType jdbcType) throws SQLException {
  if (parameter == null) {
    if (jdbcType == null) {
      throw new TypeException("JDBC requires that the JdbcType must be specified for all nullable parameters.");
    }
    try {
      ps.setNull(i, jdbcType.TYPE_CODE);
    } catch (SQLException e) {
      throw new TypeException("Error setting null for parameter #" + i + " with JdbcType " + jdbcType + " . "
            + "Try setting a different JdbcType for this parameter or a different jdbcTypeForNull configuration property. "
            + "Cause: " + e, e);
    }
  } else {
    try {
      setNonNullParameter(ps, i, parameter, jdbcType);
    } catch (Exception e) {
      throw new TypeException("Error setting non null for parameter #" + i + " with JdbcType " + jdbcType + " . "
            + "Try setting a different JdbcType for this parameter or a different configuration property. "
            + "Cause: " + e, e);
    }
  }
}
```

接下来，默认情况下使用PreparedStatementHandler#query查询数据库，它会执行Statement，然后使用ResultSetHandler对结果集进行封装：
```java
public <E> List<E> query(Statement statement, ResultHandler resultHandler) throws SQLException {
  PreparedStatement ps = (PreparedStatement) statement;
  ps.execute();
  return resultSetHandler.handleResultSets(ps);
}
```

ResultSetHandler的默认实现类是DefaultResultSetHandler，处理步骤如下：
1. 从MappedStatement获取结果集字段映射关系ResultMap
2. 根据ResultMap，使用ResultHandler对字段进行封装

DefaultResultSetHandler#handleResultSets、DefaultResultSetHandler#handleResultSet、DefaultResultSetHandler#handleRowValues、DefaultResultSetHandler#handleRowValuesForSimpleResultMap：
```java
public List<Object> handleResultSets(Statement stmt) throws SQLException {
  ErrorContext.instance().activity("handling results").object(mappedStatement.getId());

  final List<Object> multipleResults = new ArrayList<>();
  int resultSetCount = 0;
  ResultSetWrapper rsw = getFirstResultSet(stmt);

  List<ResultMap> resultMaps = mappedStatement.getResultMaps();
  int resultMapCount = resultMaps.size();
  validateResultMapsCount(rsw, resultMapCount);
  while (rsw != null && resultMapCount > resultSetCount) {
    ResultMap resultMap = resultMaps.get(resultSetCount);
    handleResultSet(rsw, resultMap, multipleResults, null);
    rsw = getNextResultSet(stmt);
    cleanUpAfterHandlingResultSet();
    resultSetCount++;
  }

  String[] resultSets = mappedStatement.getResultSets();
  if (resultSets != null) {
    while (rsw != null && resultSetCount < resultSets.length) {
      ResultMapping parentMapping = nextResultMaps.get(resultSets[resultSetCount]);
      if (parentMapping != null) {
        String nestedResultMapId = parentMapping.getNestedResultMapId();
        ResultMap resultMap = configuration.getResultMap(nestedResultMapId);
        handleResultSet(rsw, resultMap, null, parentMapping);
      }
      rsw = getNextResultSet(stmt);
      cleanUpAfterHandlingResultSet();
      resultSetCount++;
    }
  }
  return collapseSingleResultList(multipleResults);
}

private void handleResultSet(ResultSetWrapper rsw, ResultMap resultMap, List<Object> multipleResults,
    ResultMapping parentMapping) throws SQLException {
  try {
    if (parentMapping != null) {
      handleRowValues(rsw, resultMap, null, RowBounds.DEFAULT, parentMapping);
    } else if (resultHandler == null) {
      DefaultResultHandler defaultResultHandler = new DefaultResultHandler(objectFactory);
      handleRowValues(rsw, resultMap, defaultResultHandler, rowBounds, null);
      multipleResults.add(defaultResultHandler.getResultList());
    } else {
      handleRowValues(rsw, resultMap, resultHandler, rowBounds, null);
    }
  } finally {
    closeResultSet(rsw.getResultSet());
  }
}

public void handleRowValues(ResultSetWrapper rsw, ResultMap resultMap, ResultHandler<?> resultHandler,
    RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
  if (resultMap.hasNestedResultMaps()) {
    ensureNoRowBounds();
    checkResultHandler();
    handleRowValuesForNestedResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  } else {
    handleRowValuesForSimpleResultMap(rsw, resultMap, resultHandler, rowBounds, parentMapping);
  }
}
```

结果集封装主要有两个步骤：
1. 根据ResultMap初始化响应Java对象
2. 根据的属性-列字段名映射关系，从结果集获取对应值，设置到响应对象中
2. 将该行结果集封装的Java对象，添加到返回值列表中

DefaultResultSetHandler#handleRowValuesForSimpleResultMap、DefaultResultSetHandler#getRowValue和DefaultResultSetHandler#applyAutomaticMappings：
```java
private void handleRowValuesForSimpleResultMap(ResultSetWrapper rsw, ResultMap resultMap,
    ResultHandler<?> resultHandler, RowBounds rowBounds, ResultMapping parentMapping) throws SQLException {
  DefaultResultContext<Object> resultContext = new DefaultResultContext<>();
  ResultSet resultSet = rsw.getResultSet();
  skipRows(resultSet, rowBounds);
  while (shouldProcessMoreRows(resultContext, rowBounds) && !resultSet.isClosed() && resultSet.next()) {
    ResultMap discriminatedResultMap = resolveDiscriminatedResultMap(resultSet, resultMap, null);
    // 获取每一行的响应对象
    Object rowValue = getRowValue(rsw, discriminatedResultMap, null);
    // 添加返回值列表
    storeObject(resultHandler, resultContext, rowValue, parentMapping, resultSet);
  }
}

private Object getRowValue(ResultSetWrapper rsw, ResultMap resultMap, String columnPrefix) throws SQLException {
  final ResultLoaderMap lazyLoader = new ResultLoaderMap();
  Object rowValue = createResultObject(rsw, resultMap, lazyLoader, columnPrefix);
  if (rowValue != null && !hasTypeHandlerForResultObject(rsw, resultMap.getType())) {
    final MetaObject metaObject = configuration.newMetaObject(rowValue);
    boolean foundValues = this.useConstructorMappings;
    if (shouldApplyAutomaticMappings(resultMap, false)) {
      // 自动映射
      foundValues = applyAutomaticMappings(rsw, resultMap, metaObject, columnPrefix) || foundValues;
    }
    foundValues = applyPropertyMappings(rsw, resultMap, metaObject, lazyLoader, columnPrefix) || foundValues;
    foundValues = lazyLoader.size() > 0 || foundValues;
    rowValue = foundValues || configuration.isReturnInstanceForEmptyRow() ? rowValue : null;
  }
  return rowValue;
}

private boolean applyAutomaticMappings(ResultSetWrapper rsw, ResultMap resultMap, MetaObject metaObject,
    String columnPrefix) throws SQLException {
  List<UnMappedColumnAutoMapping> autoMapping = createAutomaticMappings(rsw, resultMap, metaObject, columnPrefix);
  boolean foundValues = false;
  if (!autoMapping.isEmpty()) {
    // 遍历属性-列字段名映射
    for (UnMappedColumnAutoMapping mapping : autoMapping) {
      // 将列数据封装成对应Java类型的属性值
      final Object value = mapping.typeHandler.getResult(rsw.getResultSet(), mapping.column);
      if (value != null) {
        foundValues = true;
      }
      if (value != null || configuration.isCallSettersOnNulls() && !mapping.primitive) {
        // 将属性值设置到响应对象的成员变量中
        metaObject.setValue(mapping.property, value);
      }
    }
  }
  return foundValues;
}
```

### 2.1.2 Executor#update
除了自定义Interceptor，首先会进入到CachingExecutor#update方法，它主要对全局缓存进行清除工作，然后再调用代理对象的BaseExecutor#update方法。

CachingExecutor#update和CachingExecutor#flushCacheIfRequired：
```java
public int update(MappedStatement ms, Object parameterObject) throws SQLException {
  // 清除全局缓存
  flushCacheIfRequired(ms);
  // 执行代理对象更新方法
  return delegate.update(ms, parameterObject);
}

private void flushCacheIfRequired(MappedStatement ms) {
  // 从MappedStatement中获取缓存
  Cache cache = ms.getCache();
  if (cache != null && ms.isFlushCacheRequired()) {
    // 清除缓存
    tcm.clear(cache);
  }
}
```

BaseExecutor#update会对局部缓存进行清除，然后调用实现类的doUpdate方法。

BaseExecutor#update和BaseExecutor#clearLocalCache：
```
public int update(MappedStatement ms, Object parameter) throws SQLException {
  ErrorContext.instance().resource(ms.getResource()).activity("executing an update").object(ms.getId());
  if (closed) {
    throw new ExecutorException("Executor was closed.");
  }
  // 清除局部缓存
  clearLocalCache();
  // 执行实现类更新方法
  return doUpdate(ms, parameter);
}

public void clearLocalCache() {
  if (!closed) {
    localCache.clear();
    localOutputParameterCache.clear();
  }
}
```

默认执行器实现类为SimpleExecutor，与doQuery方法类似，其doUpdate方法会执行如下逻辑：
1. 创建StatementHandler
2. 创建Statement（参数解析）
3. 执行Statement
4. 结果集封装

SimpleExecutor#doUpdate：
```java
public int doUpdate(MappedStatement ms, Object parameter) throws SQLException {
  Statement stmt = null;
  try {
    Configuration configuration = ms.getConfiguration();
    // 创建StatementHandler
    StatementHandler handler = configuration.newStatementHandler(this, ms, parameter, RowBounds.DEFAULT, null, null);
    // 创建Statement（参数解析）
    stmt = prepareStatement(handler, ms.getStatementLog());
    // 执行Statement&结果集封装
    return handler.update(stmt);
  } finally {
    closeStatement(stmt);
  }
}
```

前2个步骤与doQuery方法相同，这里不再赘述，主要看后两个步骤，对应为StatementHandler#update方法，默认实现类为PreparedStatementHandler。

由于数据库insert/update/delete操作返回的都是影响条数，同查询相比，它们的结果集封装就显得十分简单：直接返回即可。

相反，由于数据库insert操作会新增主键，为了将主键值回显给入参对象，这里还需要进行特殊处理。

```java
public int update(Statement statement) throws SQLException {
  PreparedStatement ps = (PreparedStatement) statement;
  // 执行SQL
  ps.execute();
  // 获取影响条数
  int rows = ps.getUpdateCount();
  // 回显主键值
  Object parameterObject = boundSql.getParameterObject();
  KeyGenerator keyGenerator = mappedStatement.getKeyGenerator();
  keyGenerator.processAfter(executor, mappedStatement, ps, parameterObject);
  return rows;
}
```

如果没有设置回显主键（默认情况下），KeyGenerator实现类为NoKeyGenerator，其processAfter是个空方法：
```java
public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
  // Do Nothing
}
```

通过以下方式可以开启回显主键功能：
```xml
<insert id="createUser" useGeneratedKeys="true" keyColumn="id" keyProperty="id">
  <!-- SQL -->
</insert>
```

此时默认KeyGenerator实现类为Jdbc3KeyGenerator，它会从结果集中获取对应数据库主键值（keyColumn），设置到入参对象的对应属性（keyProperty）：
```java
public void processAfter(Executor executor, MappedStatement ms, Statement stmt, Object parameter) {
  processBatch(ms, stmt, parameter);
}

public void processBatch(MappedStatement ms, Statement stmt, Object parameter) {
  final String[] keyProperties = ms.getKeyProperties();
  if (keyProperties == null || keyProperties.length == 0) {
    return;
  }
  // 遍历结果集
  try (ResultSet rs = stmt.getGeneratedKeys()) {
    final ResultSetMetaData rsmd = rs.getMetaData();
    final Configuration configuration = ms.getConfiguration();
    if (rsmd.getColumnCount() < keyProperties.length) {
      // Error?
    } else {
      // 回显主键
      assignKeys(configuration, rs, rsmd, keyProperties, parameter);
    }
  } catch (Exception e) {
    throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e, e);
  }
}

private void assignKeys(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd, String[] keyProperties,
    Object parameter) throws SQLException {
  if (parameter instanceof ParamMap || parameter instanceof StrictMap) {
    // Multi-param or single param with @Param
    assignKeysToParamMap(configuration, rs, rsmd, keyProperties, (Map<String, ?>) parameter);
  } else if (parameter instanceof ArrayList && !((ArrayList<?>) parameter).isEmpty()
      && ((ArrayList<?>) parameter).get(0) instanceof ParamMap) {
    // Multi-param or single param with @Param in batch operation
    assignKeysToParamMapList(configuration, rs, rsmd, keyProperties, (ArrayList<ParamMap<?>>) parameter);
  } else {
    // Single param without @Param
    assignKeysToParam(configuration, rs, rsmd, keyProperties, parameter);
  }
}

private void assignKeysToParam(Configuration configuration, ResultSet rs, ResultSetMetaData rsmd,
    String[] keyProperties, Object parameter) throws SQLException {
  Collection<?> params = collectionize(parameter);
  if (params.isEmpty()) {
    return;
  }
  List<KeyAssigner> assignerList = new ArrayList<>();
  for (int i = 0; i < keyProperties.length; i++) {
    assignerList.add(new KeyAssigner(configuration, rsmd, i + 1, null, keyProperties[i]));
  }
  Iterator<?> iterator = params.iterator();
  while (rs.next()) {
    if (!iterator.hasNext()) {
      throw new ExecutorException(String.format(MSG_TOO_MANY_KEYS, params.size()));
    }
    Object param = iterator.next();
    // 设值
    assignerList.forEach(x -> x.assign(rs, param));
  }
}

protected void assign(ResultSet rs, Object param) {
    if (paramName != null) {
      // If paramName is set, param is ParamMap
      param = ((ParamMap<?>) param).get(paramName);
    }
    MetaObject metaParam = configuration.newMetaObject(param);
    try {
      if (typeHandler == null) {
        if (!metaParam.hasSetter(propertyName)) {
          throw new ExecutorException("No setter found for the keyProperty '" + propertyName + "' in '"
              + metaParam.getOriginalObject().getClass().getName() + "'.");
        }
        Class<?> propertyType = metaParam.getSetterType(propertyName);
        typeHandler = typeHandlerRegistry.getTypeHandler(propertyType,
            JdbcType.forCode(rsmd.getColumnType(columnPosition)));
      }
      if (typeHandler == null) {
        // Error?
      } else {
        // 获取数据库主键值
        Object value = typeHandler.getResult(rs, columnPosition);
        // 设置入参
        metaParam.setValue(propertyName, value);
      }
    } catch (SQLException e) {
      throw new ExecutorException("Error getting generated key or setting result to parameter object. Cause: " + e,
          e);
    }
  }
```

## 2.2 使用映射接口代理对象执行
通过SqlSession#getMapper方法，可以获取映射接口的代理对象。

该代理对象实际上是从Configuration中获取的，Configuration又是从MapperRegistry中获取的，MapperRegistry会从缓存中获取MapperProxyFactory创建SqlSession的代理对象。

DefaultSqlSession#getMapper、Configuration#getMapper、MapperRegistry#getMapper和MapperProxyFactory#newInstance：
```java
// DefaultSqlSession
public <T> T getMapper(Class<T> type) {
  return configuration.getMapper(type, this);
}

// Configuration
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
  return mapperRegistry.getMapper(type, sqlSession);
}

// MapperRegistry
public <T> T getMapper(Class<T> type, SqlSession sqlSession) {
  // 获取缓存对应的MapperProxyFactory
  final MapperProxyFactory<T> mapperProxyFactory = (MapperProxyFactory<T>) knownMappers.get(type);
  if (mapperProxyFactory == null) {
    throw new BindingException("Type " + type + " is not known to the MapperRegistry.");
  }
  try {
    // 创建代理对象
    return mapperProxyFactory.newInstance(sqlSession);
  } catch (Exception e) {
    throw new BindingException("Error getting mapper instance. Cause: " + e, e);
  }
}

// MapperProxyFactory
public T newInstance(SqlSession sqlSession) {
  final MapperProxy<T> mapperProxy = new MapperProxy<>(sqlSession, mapperInterface, methodCache);
  return newInstance(mapperProxy);
}
protected T newInstance(MapperProxy<T> mapperProxy) {
  return (T) Proxy.newProxyInstance(mapperInterface.getClassLoader(), new Class[] { mapperInterface }, mapperProxy);
}
```

MapperProxyFactory使用JDK的Proxy创建代理对象，实际逻辑位于InvocationHandler实现类MapperProxy的invoke方法中，它会创建MapperMethodInvoker对象（PlainMethodInvoker）并执行其invoke方法：
```java
// MapperProxy
public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
  try {
    if (Object.class.equals(method.getDeclaringClass())) {
      return method.invoke(this, args);
    }
    return cachedInvoker(method).invoke(proxy, method, args, sqlSession);
  } catch (Throwable t) {
    throw ExceptionUtil.unwrapThrowable(t);
  }
}
private MapperMethodInvoker cachedInvoker(Method method) throws Throwable {
  try {
    return MapUtil.computeIfAbsent(methodCache, method, m -> {
      if (!m.isDefault()) {
        // 对于映射接口方法，会创建此对象
        return new PlainMethodInvoker(new MapperMethod(mapperInterface, method, sqlSession.getConfiguration()));
      }
      try {
        if (privateLookupInMethod == null) {
          return new DefaultMethodInvoker(getMethodHandleJava8(method));
        } else {
          return new DefaultMethodInvoker(getMethodHandleJava9(method));
        }
      } catch (IllegalAccessException | InstantiationException | InvocationTargetException
          | NoSuchMethodException e) {
        throw new RuntimeException(e);
      }
    });
  } catch (RuntimeException re) {
    Throwable cause = re.getCause();
    throw cause == null ? re : cause;
  }
}

// PlainMethodInvoker
public Object invoke(Object proxy, Method method, Object[] args, SqlSession sqlSession) throws Throwable {
  return mapperMethod.execute(sqlSession, args);
}
```

PlainMethodInvoker#invoke又会执行MapperMethod#execute方法，从该方法中可以明显看到，映射接口代理对象只不过是对SqlSession对象进行的封装，实际操作数据库还是得依靠SqlSession（此时又回到了2.1根据statementId执行的流程）：
```java
public Object execute(SqlSession sqlSession, Object[] args) {
  Object result;
  switch (command.getType()) {
    // 插入
    case INSERT: {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.insert(command.getName(), param));
      break;
    }
    // 更新
    case UPDATE: {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.update(command.getName(), param));
      break;
    }
    // 删除
    case DELETE: {
      Object param = method.convertArgsToSqlCommandParam(args);
      result = rowCountResult(sqlSession.delete(command.getName(), param));
      break;
    }
    // 查询
    case SELECT:
      if (method.returnsVoid() && method.hasResultHandler()) {
        executeWithResultHandler(sqlSession, args);
        result = null;
      } else if (method.returnsMany()) {
        result = executeForMany(sqlSession, args);
      } else if (method.returnsMap()) {
        result = executeForMap(sqlSession, args);
      } else if (method.returnsCursor()) {
        result = executeForCursor(sqlSession, args);
      } else {
        Object param = method.convertArgsToSqlCommandParam(args);
        result = sqlSession.selectOne(command.getName(), param);
        if (method.returnsOptional() && (result == null || !method.getReturnType().equals(result.getClass()))) {
          result = Optional.ofNullable(result);
        }
      }
      break;
    // 刷新
    case FLUSH:
      result = sqlSession.flushStatements();
      break;
    default:
      throw new BindingException("Unknown execution method for: " + command.getName());
  }
  if (result == null && method.getReturnType().isPrimitive() && !method.returnsVoid()) {
    throw new BindingException("Mapper method '" + command.getName()
        + "' attempted to return null from a method with a primitive return type (" + method.getReturnType() + ").");
  }
  return result;
}
```

还有个问题，MapperProxyFactory是什么时候生成的？实际上，在Mybatis启动时，通过MapperRegistry#addMapper方法就会创建MapperProxyFactory缓存（具体流程可以查看[Mybatis如何添加映射接口和映射文件？](https://www.cnblogs.com/Xianhuii/p/17625217.html)）：
```java
public <T> void addMapper(Class<T> type) {
  if (type.isInterface()) {
    if (hasMapper(type)) {
      throw new BindingException("Type " + type + " is already known to the MapperRegistry.");
    }
    boolean loadCompleted = false;
    try {
      // 添加MapperProxyFactory缓存
      knownMappers.put(type, new MapperProxyFactory<>(type));
      // It's important that the type is added before the parser is run
      // otherwise the binding may automatically be attempted by the
      // mapper parser. If the type is already known, it won't try.
      MapperAnnotationBuilder parser = new MapperAnnotationBuilder(config, type);
      parser.parse();
      loadCompleted = true;
    } finally {
      if (!loadCompleted) {
        knownMappers.remove(type);
      }
    }
  }
}
```

我们也可以发现，MapperProxyFactory缓存与MappedStatement并没有直接关系，它们之间需要SqlSession进行连接。即映射接口代理对象 -> SqlSession -> Configuration -> MapperRegistry -> 映射文件。

# 3 关闭SqlSession
数据库连接是计算机网络资源，使用完需要及时关闭，避免资源泄露，造成服务器性能下降。

如果使用了数据库连接池，则需要清理本次会话的缓存数据，将连接放回连接池。

使用SqlSession#close()可以关闭本次会话连接，由于SqlSession实现了Closeable接口，通常使用以下方式自动关闭：
```java
try (SqlSession session = sqlSessionFactory.openSession(true)) {
  // 执行SQL语句
}
```

SqlSession#close()方法会本次会话的执行器Executor和游标Cursor，具体怎么关闭资源，实际上是由执行器和游标实现类自己去实现的。总体来说会完成一下工作：
1. 清除会话缓存数据。
2. 关闭数据库连接。如果使用连接池，则会本次连接放回连接池。


以下为相关源码。

SqlSession#close()关闭executor和cursor：
```java
public void close() {
  try {
    executor.close(isCommitOrRollbackRequired(false));
    closeCursors();
    dirty = false;
  } finally {
    ErrorContext.instance().reset();
  }
}
```

CachingExecutor#close()会回滚/提交本次会话的缓存，然后关闭代理执行器资源：
```java
public void close(boolean forceRollback) {
  try {
    // issues #499, #524 and #573
    if (forceRollback) {
      tcm.rollback();
    } else {
      tcm.commit();
    }
  } finally {
    delegate.close(forceRollback);
  }
}
```

BaseExecutor#close()会关闭事务，并清除本地缓存：
```java
public void close(boolean forceRollback) {
  try {
    try {
      rollback(forceRollback);
    } finally {
      if (transaction != null) {
        transaction.close();
      }
    }
  } catch (SQLException e) {
    // Ignore. There's nothing that can be done at this point.
    log.warn("Unexpected exception on closing transaction.  Cause: " + e);
  } finally {
    transaction = null;
    deferredLoads = null;
    localCache = null;
    localOutputParameterCache = null;
    closed = true;
  }
}
```

Transaction#close()会关闭数据库连接，具体怎么实现交给子类决定。

例如，JdbcTransaction#Close()会直接关闭connection：
```java
public void close() throws SQLException {
  if (connection != null) {
    resetAutoCommit();
    if (log.isDebugEnabled()) {
      log.debug("Closing JDBC Connection [" + connection + "]");
    }
    connection.close();
  }
}
```

SqlSession#closeCursors()遍历关闭所有corsor:
```java
private void closeCursors() {
  if (cursorList != null && !cursorList.isEmpty()) {
    for (Cursor<?> cursor : cursorList) {
      try {
        cursor.close();
      } catch (IOException e) {
        throw ExceptionFactory.wrapException("Error closing cursor.  Cause: " + e, e);
      }
    }
    cursorList.clear();
  }
}
```

DefaultCursor#close()关闭结果集ResultSet：
```java
public void close() {
  if (isClosed()) {
    return;
  }
  ResultSet rs = rsw.getResultSet();
  try {
    if (rs != null) {
      rs.close();
    }
  } catch (SQLException e) {
    // ignore
  } finally {
    status = CursorStatus.CLOSED;
  }
}
```