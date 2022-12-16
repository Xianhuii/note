上篇文章里，我们讲解了`RequestMappingHandlerMapping`请求地址映射的初始化流程，理解了`@Controller`和`@RequestMapping`是如何被加载到缓存中的。

今天我们来进一步学习，在接收到请求时，`RequestMappingHandlerMapping`是如何进行请求地址映射的。

先放一个类图，在请求地址映射过程中，会依次执行到这些方法：
![[RequestMappingHandlerMapping 1.png]]

# 1 HandlerMapping
首先，`DispatcherServlet`会调用`HandlerMapping`接口的`getHandler()`方法：
```java
HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception;
```
这个方法主要起着规范的作用，`DispatcherServlet`可以根据这个方法调用所有`HandlerMapping`实现类进行请求地址映射。

# 2 AbstractHandlerMapping
`AbstractHandlerMapping`是所有`HandlerMapping`的抽象基类，提供了拦截器、排序和默认处理器等功能。

`AbstractHandlerMapping`是常见`HandlerMapping`实现类的共同父类，它的核心功能是定义了获取`HandlerExecutionChain`的基础流程：
1. 获取`handler`（由实现类定义具体逻辑）
2. 创建`HandlerExecutionChain`，添加拦截器
3. 添加跨域拦截器

`AbstractHandlerMapping`的`getHandler()`源码如下：
```java
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {  
// 1、获取handler
   Object handler = getHandlerInternal(request);  
   if (handler == null) {  
      handler = getDefaultHandler();  
   }  
   if (handler == null) {  
      return null;  
   }  
   // Bean name or resolved handler?  
   if (handler instanceof String) {  
      String handlerName = (String) handler;  
      handler = obtainApplicationContext().getBean(handlerName);  
   }  
  
   // Ensure presence of cached lookupPath for interceptors and others  
   if (!ServletRequestPathUtils.hasCachedPath(request)) {  
      initLookupPath(request);  
   }  
   // 2、创建HandlerExecutionChain，添加拦截器
   HandlerExecutionChain executionChain = getHandlerExecutionChain(handler, request);  
  
   if (logger.isTraceEnabled()) {  
      logger.trace("Mapped to " + handler);  
   }  
   else if (logger.isDebugEnabled() && !DispatcherType.ASYNC.equals(request.getDispatcherType())) {  
      logger.debug("Mapped to " + executionChain.getHandler());  
   }  
   // 3、添加跨域拦截器
   if (hasCorsConfigurationSource(handler) || CorsUtils.isPreFlightRequest(request)) {  
      CorsConfiguration config = getCorsConfiguration(handler, request);  
      if (getCorsConfigurationSource() != null) {  
         CorsConfiguration globalConfig = getCorsConfigurationSource().getCorsConfiguration(request);  
         config = (globalConfig != null ? globalConfig.combine(config) : config);  
      }  
      if (config != null) {  
         config.validateAllowCredentials();  
      }  
      executionChain = getCorsHandlerExecutionChain(request, executionChain, config);  
   }  
  
   return executionChain;  
}
```

## 2.1 获取handler
`AbstractHandlerMapping`通过`getHandlerInternal()`方法获取`handler`。

该方法由具体实现类进行实现，如果找到匹配的`handler`，则会返回该`handler`；如果没有找到，则会返回`null`。

具体实现我们会在下文的实现类中进行讲解。

## 2.2 创建HandlerExecutionChain，添加拦截器
`AbstractHandlerMapping`通过`getHandlerExecutionChain()`方法创建`HandlerExecutionChain`对象，并添加拦截器。源码如下：
```java
protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {  
// 1、创建HandlerExecutionChain对象
   HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?  
         (HandlerExecutionChain) handler : new HandlerExecutionChain(handler));  

// 2、添加拦截器
   for (HandlerInterceptor interceptor : this.adaptedInterceptors) {  
      if (interceptor instanceof MappedInterceptor) {  
         MappedInterceptor mappedInterceptor = (MappedInterceptor) interceptor;  
         if (mappedInterceptor.matches(request)) {  
            chain.addInterceptor(mappedInterceptor.getInterceptor());  
         }  
      }  
      else {  
         chain.addInterceptor(interceptor);  
      }  
   }  
   return chain;  
}
```

它会对初始化时配置的拦截器进行遍历：
1. 如果是`MappedInterceptor`实现类，会根据匹配规则进行判断是否添加。
2. 如果不是`MappedInterceptor`实现类，会直接添加。

## 2.3 添加跨域拦截器
添加跨域拦截器分为以下几个步骤：
1. 判断是否存在跨域配置，或是否预检请求
2. 获取`handler`级别的跨域配置
3. 获取`HandlerMapping`级别的跨域配置
4. 整合跨域配置
5. 创建并添加跨域拦截器

### 2.3.1 判断是否存在跨域配置
在`AbstractHandlerMapping`中，会判断`handler`是否`CorsConfigurationSource`的实现类（对于`RequestMappingHandlerMapping`而言，`handler`是`HandlerMethod`类型，所以第一个条件永远是`false`），以及是否存在`HandlerMapping`级别的跨域配置源：
```java
protected boolean hasCorsConfigurationSource(Object handler) {  
   if (handler instanceof HandlerExecutionChain) {  
      handler = ((HandlerExecutionChain) handler).getHandler();  
   }  
   return (handler instanceof CorsConfigurationSource || this.corsConfigurationSource != null);  
}
```

而在`AbstractHandlerMethodMapping`子抽象类中，会进一步判断是否存在`handler`级别（也就是`@CrossOrigin`级别）的跨域配置：
```java
protected boolean hasCorsConfigurationSource(Object handler) {  
   return super.hasCorsConfigurationSource(handler) ||  
         (handler instanceof HandlerMethod &&  
               this.mappingRegistry.getCorsConfiguration((HandlerMethod) handler) != null);  
}
```

### 2.3.2 判断是否是预检请求
`org.springframework.web.cors.CorsUtils#isPreFlightRequest`：
```java
public static boolean isPreFlightRequest(HttpServletRequest request) {  
   return (HttpMethod.OPTIONS.matches(request.getMethod()) &&  
         request.getHeader(HttpHeaders.ORIGIN) != null &&  
         request.getHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD) != null);  
}
```

### 2.3.3 获取handler级别跨域配置
在`AbstractHandlerMapping`中，会判断`handler`是否`CorsConfigurationSource`的实现类，从中获取`handler`级别的跨域配置。对于`RequestMappingHandlerMapping`而言，`handler`是`HandlerMethod`类型，所以第一个条件永远返回`null`：
```java
protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {  
   Object resolvedHandler = handler;  
   if (handler instanceof HandlerExecutionChain) {  
      resolvedHandler = ((HandlerExecutionChain) handler).getHandler();  
   }  
   if (resolvedHandler instanceof CorsConfigurationSource) {  
      return ((CorsConfigurationSource) resolvedHandler).getCorsConfiguration(request);  
   }  
   return null;  
}
```

在`AbstractHandlerMethodMapping`子抽象类中，会从`mappingRegistry`（`request-handler`缓存）中获取`handler`级别的跨域配置（在上篇文章中，我们有讲述过`RequestMappingHandlerMapping`如何缓存`@CrossOrigin`级别的跨域配置的）：
```java
protected CorsConfiguration getCorsConfiguration(Object handler, HttpServletRequest request) {  
   CorsConfiguration corsConfig = super.getCorsConfiguration(handler, request);  
   if (handler instanceof HandlerMethod) {  
      HandlerMethod handlerMethod = (HandlerMethod) handler;  
      if (handlerMethod.equals(PREFLIGHT_AMBIGUOUS_MATCH)) {  
         return AbstractHandlerMethodMapping.ALLOW_CORS_CONFIG;  
      }  
      else {  
         CorsConfiguration corsConfigFromMethod = this.mappingRegistry.getCorsConfiguration(handlerMethod);  
         corsConfig = (corsConfig != null ? corsConfig.combine(corsConfigFromMethod) : corsConfigFromMethod);  
      }  
   }  
   return corsConfig;  
}
```

### 2.3.4  获取HandlerMapping级别的跨域配置
从`AbstractHandlerMapping`的`corsConfigurationSource`成员变量中，可以获取到`HandlerMapping`级别的跨域配置，该配置可以通过以下方式添加：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void addCorsMappings(CorsRegistry registry) {
	    // 添加HandlerMapping级别的跨域配置
    }
}
```

### 2.3.5 整合跨域配置
在整合跨域配置过程中，有三种情况：
1. 对于`origins`、`originPatterns`、`allowedHeaders`、`exposedHeaders`和`methods`等列表属性，会获取全部。
2. 对于`allowCredentials`，会优先获取方法级别的配置。
3. 对于`maxAge`，会获取最大值。

具体可以查看相关源码：
```java
public CorsConfiguration combine(@Nullable CorsConfiguration other) {  
   if (other == null) {  
      return this;  
   }  
   // Bypass setAllowedOrigins to avoid re-compiling patterns  
   CorsConfiguration config = new CorsConfiguration(this);  
   List<String> origins = combine(getAllowedOrigins(), other.getAllowedOrigins());  
   List<OriginPattern> patterns = combinePatterns(this.allowedOriginPatterns, other.allowedOriginPatterns);  
   config.allowedOrigins = (origins == DEFAULT_PERMIT_ALL && !CollectionUtils.isEmpty(patterns) ? null : origins);  
   config.allowedOriginPatterns = patterns;  
   config.setAllowedMethods(combine(getAllowedMethods(), other.getAllowedMethods()));  
   config.setAllowedHeaders(combine(getAllowedHeaders(), other.getAllowedHeaders()));  
   config.setExposedHeaders(combine(getExposedHeaders(), other.getExposedHeaders()));  
   Boolean allowCredentials = other.getAllowCredentials();  
   if (allowCredentials != null) {  
      config.setAllowCredentials(allowCredentials);  
   }  
   Long maxAge = other.getMaxAge();  
   if (maxAge != null) {  
      config.setMaxAge(maxAge);  
   }  
   return config;  
}
```

### 2.3.6 创建并添加跨域拦截器
在这一步，对于预检请求，会创建`HandlerExecutionChain`；对于普通请求，会创建`CorsInterceptor`拦截器，并添加到首位：
```java
protected HandlerExecutionChain getCorsHandlerExecutionChain(HttpServletRequest request,  
      HandlerExecutionChain chain, @Nullable CorsConfiguration config) {  
  
   if (CorsUtils.isPreFlightRequest(request)) {  
      HandlerInterceptor[] interceptors = chain.getInterceptors();  
      return new HandlerExecutionChain(new PreFlightHandler(config), interceptors);  
   }  
   else {  
      chain.addInterceptor(0, new CorsInterceptor(config));  
      return chain;  
   }  
}
```

# 3 AbstractHandlerMethodMapping
`AbstractHandlerMethodMapping`是`HandlerMethod`请求映射的抽象基类，它的`getHandlerInternal()`方法定义了请求地址映射的核心流程：
1. 解析请求路径
2. 根据请求地址查找`HandlerMethod`

`AbstractHandlerMethodMapping#getHandlerInternal`：
```java
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {  
// 1、解析请求地址
   String lookupPath = initLookupPath(request);  
   this.mappingRegistry.acquireReadLock();  
   try {  
   // 2、根据请求地址查找HandlerMethod
      HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);  
      return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);  
   }  
   finally {  
      this.mappingRegistry.releaseReadLock();  
   }  
}
```

## 3.1 解析请求路径
解析请求路径过程会获取当前请求的接口地址路径。

简单来说，会去除请求地址开头的`contextPaht`。例如在`application.properties`配置`contextPath`如下：
```properties
server.servlet.context-path=/context-path
```

此时，请求`/context-path/test`地址，经过`initLookPath()`方法处理，会返回`/test`为实际请求路径。

实际上，这也很容易理解。因为在`RequestMappingHandlerMapping`初始化`pathLookup`映射缓存时，就没有将`contextPath`考虑在内，那么在实际处理请求时，当然也要把`contextPath`去掉。

解析请求路径的作用也是为了方便直接从`pathLookup`映射缓存中获取对应的`RequestMappingInfo`信息。

`AbstractHandlerMapping#initLookupPath`源码如下：
```java
protected String initLookupPath(HttpServletRequest request) {  
   if (usesPathPatterns()) {  
      request.removeAttribute(UrlPathHelper.PATH_ATTRIBUTE);  
      RequestPath requestPath = ServletRequestPathUtils.getParsedRequestPath(request);  
      String lookupPath = requestPath.pathWithinApplication().value();  
      return UrlPathHelper.defaultInstance.removeSemicolonContent(lookupPath);  
   }  
   else {  
      return getUrlPathHelper().resolveAndCacheLookupPath(request);  
   }  
}
```
## 3.2 根据请求路径查找HandlerMethod
在`AbstractHandlerMethodMapping#lookupHandlerMethod`方法中，会按如下步骤获取`HandlerMethod`：
1. 根据请求路径从`pathLookup`映射缓存查找对应的`RequestMappingInfo`列表。
2. 根据`RequestMappingInfo`从`registry`缓存中获取对应的`MappingRegistration`列表。
3. 根据当前`request`，对`MappingRegistration`列表按匹配度进行排序。
4. 从中取匹配度最高的`HandlerMethod`进行返回。
