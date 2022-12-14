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
