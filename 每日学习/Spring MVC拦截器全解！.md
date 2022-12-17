Spring MVC拦截器（`HandlerInterceptor`）是一个十分重要且常用的功能，是我们学习和使用Spring MVC必须掌握的基础技能之一。

`HandlerInterceptor`和`Servlet`规范中的`Filter`类似，都可以用来对请求进行拦截。不同的是，`Filter`针对的是`servlet`，而`HandlerInterceptor`针对的是`handler`。

## 1 HandlerInterceptor工作原理
`org.springframework.web.servlet.HandlerInterceptor`源码：
```java
public interface HandlerInterceptor {  
    default boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {  
      return true;  
   }
  default void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable ModelAndView modelAndView) throws Exception {  
   }
  default void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {  
   }
}
```

`HandlerInterceptor`的三个方法会在不同的请求处理节点进行调用：
1. `preHandle()`：在`handler`执行前调用。
2. `postHandle()`：在`handler`执行后调用。
3. `afterCompletion()`：在请求处理完后调用。

在项目初始化时，会将配置的一系列`HandlerInterceptor`按顺序收集起来，设置为`HandlerMapping`的成员变量`adaptedInterceptors`。

在处理请求时，通过`HandlerMapping`的`getHanlder()`方法，一方面会获取请求映射的`handler`，另一方面会从`adaptedInterceptors`获取拦截器，共同构造`HandlerExecutionChain`返回。

在后续流程中，会依次在各个节点调用`HandlerInterceptor`的方法。

