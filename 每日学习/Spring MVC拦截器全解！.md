Spring MVC拦截器（`HandlerInterceptor`）是一个十分重要且常用的功能，是我们学习和使用Spring MVC必须掌握的基础技能之一。

`HandlerInterceptor`和`Servlet`规范中的`Filter`类似，都可以用来对请求进行拦截。不同的是，`Filter`针对的是`servlet`，而`HandlerInterceptor`针对的是`handler`。

## 1 拦截器工作原理
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

下图表示完整请求流程中`HandlerInterceptor`调用的节点：
![[HandlerInterceptor.png]]

如果在`preHandle()`阶段就有某个拦截器校验不通过，会从上一个拦截器开始执行`afterCompletion()`进行返回，流程如下：
![[HandlerInterceptor-fail.png]]

`HandlerInterceptor`的执行顺序是通过`HandlerExecutionChain`的`interceptorIndex`成员变量决定的，它的默认值是`-1`。

上述流程图的执行原理可以通过相关源码验证，`HandlerExecutionChain#applyPreHandle`源码如下：
```java
boolean applyPreHandle(HttpServletRequest request, HttpServletResponse response) throws Exception {  
   for (int i = 0; i < this.interceptorList.size(); i++) {  
      HandlerInterceptor interceptor = this.interceptorList.get(i);  
      if (!interceptor.preHandle(request, response, this.handler)) {  
         triggerAfterCompletion(request, response, null);  
         return false;  
      }  
      this.interceptorIndex = i;  
   }  
   return true;  
}
```

`HandlerExecutionChain#applyPostHandle`源码如下：
```java
void applyPostHandle(HttpServletRequest request, HttpServletResponse response, @Nullable ModelAndView mv) throws Exception {  
   for (int i = this.interceptorList.size() - 1; i >= 0; i--) {  
      HandlerInterceptor interceptor = this.interceptorList.get(i);  
      interceptor.postHandle(request, response, this.handler, mv);  
   }  
}
```

`HandlerExecutionChain#triggerAfterCompletion`源码如下：
```java
void triggerAfterCompletion(HttpServletRequest request, HttpServletResponse response, @Nullable Exception ex) {  
   for (int i = this.interceptorIndex; i >= 0; i--) {  
      HandlerInterceptor interceptor = this.interceptorList.get(i);  
      try {  
         interceptor.afterCompletion(request, response, this.handler, ex);  
      }  
      catch (Throwable ex2) {  
         logger.error("HandlerInterceptor.afterCompletion threw exception", ex2);  
      }  
   }  
}
```

# 2 拦截器配置流程
Spring MVC拦截器的使用十分简单，只需要两个步骤：
1. 实现`HandlerInterceptor`接口，自定应拦截规则。
2. 将自定义拦截器添加到`HandlerMapping`成员变量中。

Spring提供了通过在`WebMvcConfigurer`中注册拦截器的方式：
```java
@Configuration 
@EnableWebMvc 
public class WebConfig implements WebMvcConfigurer {   
	@Override  
	public void addInterceptors(InterceptorRegistry registry) {  
		registry.addInterceptor(new LocaleChangeInterceptor());
		registry.addInterceptor(new ThemeChangeInterceptor()).addPathPatterns("/**").excludePathPatterns("/admin/**");   
		registry.addInterceptor(new SecurityInterceptor()).addPathPatterns("/secure/*"); 
	} 
}
```

