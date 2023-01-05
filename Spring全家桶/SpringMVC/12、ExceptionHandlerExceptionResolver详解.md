`ExceptionHandlerExceptionResolver`是Spring Web MVC中使用最简单、最常用的异常处理器之一，可以进行全局异常统一处理。

在项目初始化时，`ExceptionHandlerExceptionResolver`对`@ControllerAdvice`和`@ExceptionHandler`标注的异常处理方法进行缓存，构筑异常-处理方法的映射。

在处理请求时，如果业务中抛出异常到`DispatcherServlet`，`ExceptionHandlerExceptionResolver`会根据异常对象找到对应处理方法，进行统一异常处理。

# 1 基本使用
例如，在业务中会抛出`java.lang.ArithmeticException`异常：
```java
@RestController  
@RequestMapping("ControllerAdvicerConfig")  
public class ControllerAdvicerController {  
    @RequestMapping("byZero")  
    public int byZero() {  
        return 1/0;  
    }
}
```

我们只需要使用`@ControllerAdvice`配置全局异常处理器，使用`@ExceptionHandler`标明处理的异常：
```java
@ControllerAdvice  
public class ControllerAdvicerConfig {  
    @ExceptionHandler(value = ArithmeticException.class)  
    @ResponseBody  
    public String exception(Exception e) {  
        return this.getClass().getName() + "\r\n" + e.getClass().getName() + "\r\n" + e.getMessage();  
    }  
}
```

上述业务接口触发后，抛出的`ArithmeticException`异常会分发到异常处理器的`exception()`方法中处理，最终返回以下内容：
```
com.example.servletmvc.config.ControllerAdvicerConfig
java.lang.ArithmeticException
/ by zero
```

通过`@ExceptionHandler`定义多个异常处理方法，即可对整个系统的各个异常进行统一处理。

# 2 核心知识
## 2.1 核心成员变量
`ExceptionHandlerExceptionResolver`的核心成员变量如下：
![[ExceptionHandlerExceptionResolver.png]]

`argumentResolvers`和`customArgumentResolvers`是参数解析器，负责将HTTP请求数据解析成异常处理方法的形参对象。

`returnValueHandlers`和`customReturnValueHandlers`是返回值处理器，负责将异常处理方法的返回值进行处理。

`messageConverters`是数据转换的底层工具，一方面从HTTP请求中读取并转换数据成对象，另一方面将对象转成HTTP响应数据。

`contentNegotiationManager`是负责媒体内容的校验，即根据`Content-Tepe`进行不同处理。

`responseBodyAdvice`是`ResponseBodyAdvice`的缓存，即支持在异常处理返回值写到输出流前的切面处理。

`applicationContext`是Spring上下文，可以从中获取容器中内容。

`exceptionHandlerCache`是异常-异常处理方法的缓存。

`exceptionHandlerAdviceCache`是`@ControllerAdvice`全局异常处理器的缓存。

## 2.2 类层次结构
`ExceptionHandlerExceptionResolver`的类层次结构以及核心方法如下：
![[ExceptionHandlerExceptionResolver 1.png]]

实现了`ApplicationContextAware`接口，可以获取Spring容器`applicationContext`。

实现了`InitializingBean`接口，可以在`afterPropertiesSet()`方法中进行初始化。

实现`HandlerExceptionResolver`体系，在处理异常时，会依次调用以下核心方法：
1. `HandlerExceptionResolver#resolveException()`
2. `AbstractHandlerExceptionResolver#resolveException()`
3. `AbstractHandlerExceptionResolver#doResolveException()`
4. `AbstractHandlerMethodExceptionResolver#doResolveException()`
5. `AbstractHandlerMethodExceptionResolver#doResolveHandlerMethodException()`
6. `ExceptionHandlerExceptionResolver#doResolveHandlerMethodException()`
7. `ExceptionHandlerExceptionResolver#getExceptionHandlerMethod()`
8. `ServletInvocableHandlerMethod#invokeAndHandle()`

# 3 初始化流程
`ExceptionHandlerExceptionResolver`的初始化流程与`RequestMappingHandlerMapping`、`RequestMappingHandlerAdapter`类似：
1. 在构造方法中进行`messageConverters`的初始化，会被`WebMvcConfigurationSupport`覆盖。
2. 在`afterPropertiesSet()`中对`exceptionHandlerAdviceCache`、`responseBodyAdvice`、`argumentResolvers`和`returnValueHandlers`等进行初始化。
3. 在`WebMvcConfigurationSupport#handlerExceptionResolver()`进行完整初始化。

## 3.1 构造函数
在构造函数中，会添加默认`messageConverters`，但后续会被`WebMvcConfigurationSupport`中初始化流程所覆盖：
```java
public ExceptionHandlerExceptionResolver() {  
   this.messageConverters = new ArrayList<>();  
   this.messageConverters.add(new ByteArrayHttpMessageConverter());  
   this.messageConverters.add(new StringHttpMessageConverter());  
   if(!shouldIgnoreXml) {  
      try {  
         this.messageConverters.add(new SourceHttpMessageConverter<>());  
      }  
      catch (Error err) {  
         // Ignore when no TransformerFactory implementation is available  
      }  
   }  
   this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());  
}
```

## 3.2 afterPropertiesSet()
只要被Spring容器所管理，实现`InitializingBean`接口，就会自动调用`afterPropertiesSet()`方法。

实际上，`ExceptionHandlerExceptionResolver`并不会成为`bean`交给Spring容器管理。但是在`WebMvcConfigurationSupport`初始化过程中，会手动调用`afterPropertiesSet()`进行默认初始化。

在`ExceptionHandlerExceptionResolver#afterPropertiesSet()`方法中，会对`exceptionHandlerAdviceCache`、`responseBodyAdvice`、`argumentResolvers`和`returnValueHandlers`等进行初始化：
```java
public void afterPropertiesSet() {  
   // Do this first, it may add ResponseBodyAdvice beans  
   initExceptionHandlerAdviceCache();  
  
   if (this.argumentResolvers == null) {  
      List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();  
      this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);  
   }  
   if (this.returnValueHandlers == null) {  
      List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();  
      this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);  
   }  
}
```

在`ExceptionHandlerExceptionResolver#initExceptionHandlerAdviceCache()`方法中，会从Spring容器中获取所有`@ControllerAdvice`标注的`bean`。遍历这些`bean`，将其中`@ExceptionHandler`标注的异常处理方法缓存到`exceptionHandlerAdviceCache`。如果这些`bean`实现了`ResponseBodyAdvice`接口，还会缓存到`responseBodyAdvice`：
```java
private void initExceptionHandlerAdviceCache() {  
   // 获取所有@ControllerAdvice标注的bean
   List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());  
   for (ControllerAdviceBean adviceBean : adviceBeans) {  
      Class<?> beanType = adviceBean.getBeanType();  
      if (beanType == null) {  
         throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);  
      }  
      // 构造异常-处理方法映射
      ExceptionHandlerMethodResolver resolver = new ExceptionHandlerMethodResolver(beanType);  
      // 添加exceptionHandlerAdviceCache缓存
      if (resolver.hasExceptionMappings()) {  
         this.exceptionHandlerAdviceCache.put(adviceBean, resolver);  
      }  
      // 添加responseBodyAdvice缓存
      if (ResponseBodyAdvice.class.isAssignableFrom(beanType)) {  
         this.responseBodyAdvice.add(adviceBean);  
      }  
   }   
}
```

在`getDefaultXxx()`方法中，会添加一系列默认处理器，并且通过`getCustomXxx()`方法获取开发人员自定义的处理器。例如：
```java
protected List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {  
   List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>();  
  
   // Annotation-based argument resolution  
   resolvers.add(new SessionAttributeMethodArgumentResolver());  
   resolvers.add(new RequestAttributeMethodArgumentResolver());  
  
   // Type-based argument resolution  
   resolvers.add(new ServletRequestMethodArgumentResolver());  
   resolvers.add(new ServletResponseMethodArgumentResolver());  
   resolvers.add(new RedirectAttributesMethodArgumentResolver());  
   resolvers.add(new ModelMethodProcessor());  
  
   // Custom arguments  
   if (getCustomArgumentResolvers() != null) {  
      resolvers.addAll(getCustomArgumentResolvers());  
   }  
  
   // Catch-all  
   resolvers.add(new PrincipalMethodArgumentResolver());  
  
   return resolvers;  
}
```

## 3.3 WebMvcConfigurationSupport#handlerExceptionResolver()
在`WebMvcConfigurationSupport#handlerExceptionResolver()`方法中，会创建名为`handlerExceptionResolver`的`bean`，后续会被`DispatcherServlet#initHandlerExceptionResolvers()`方法添加到`DispatcherServlet`中。

`WebMvcConfigurationSupport#handlerExceptionResolver()`方法实际返回的是`HandlerExceptionResolverComposite`对象，它使用了组合模式，内部持有实际`HandlerExceptionResolver`处理器：
```java
public HandlerExceptionResolver handlerExceptionResolver(  
      @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager) {  
   List<HandlerExceptionResolver> exceptionResolvers = new ArrayList<>();  
   configureHandlerExceptionResolvers(exceptionResolvers);  
   if (exceptionResolvers.isEmpty()) {  
      addDefaultHandlerExceptionResolvers(exceptionResolvers, contentNegotiationManager);  
   }  
   extendHandlerExceptionResolvers(exceptionResolvers);  
   HandlerExceptionResolverComposite composite = new HandlerExceptionResolverComposite();  
   composite.setOrder(0);  
   composite.setExceptionResolvers(exceptionResolvers);  
   return composite;  
}
```

通过`configureHandlerExceptionResolvers()`和`extendHandlerExceptionResolvers()`方法使得开发人员可以进行配置或扩展自定义异常处理器，即通过`WebMvcConfigurer`的`configureHandlerExceptionResolvers()`和`extendHandlerExceptionResolvers()`方法。

默认情况下，会使用`WebMvcConfigurationSupport#addDefaultHandlerExceptionResolvers()`添加默认异常处理器，包括`ExceptionHandlerExceptionResolver`、`ResponseStatusExceptionResolver`和`DefaultHandlerExceptionResolver`：
```java
protected final void addDefaultHandlerExceptionResolvers(List<HandlerExceptionResolver> exceptionResolvers,  
      ContentNegotiationManager mvcContentNegotiationManager) {  
   // 添加ExceptionHandlerExceptionResolver处理器
   ExceptionHandlerExceptionResolver exceptionHandlerResolver = createExceptionHandlerExceptionResolver();  
   exceptionHandlerResolver.setContentNegotiationManager(mvcContentNegotiationManager);  
   exceptionHandlerResolver.setMessageConverters(getMessageConverters());  
   exceptionHandlerResolver.setCustomArgumentResolvers(getArgumentResolvers());  
   exceptionHandlerResolver.setCustomReturnValueHandlers(getReturnValueHandlers());  
   if (jackson2Present) {  
      exceptionHandlerResolver.setResponseBodyAdvice(  
            Collections.singletonList(new JsonViewResponseBodyAdvice()));  
   }  
   if (this.applicationContext != null) {  
      exceptionHandlerResolver.setApplicationContext(this.applicationContext);  
   }  
   exceptionHandlerResolver.afterPropertiesSet();  
   exceptionResolvers.add(exceptionHandlerResolver);  
  
   // 添加ResponseStatusExceptionResolver处理器
   ResponseStatusExceptionResolver responseStatusResolver = new ResponseStatusExceptionResolver();  
   responseStatusResolver.setMessageSource(this.applicationContext);  
   exceptionResolvers.add(responseStatusResolver);  
  
   // 添加DefaultHandlerExceptionResolver处理器
   exceptionResolvers.add(new DefaultHandlerExceptionResolver());  
}
```

在添加`ExceptionHandlerExceptionResolver`处理器过程中，除了一些默认配置，开发人员可以通过通过对应的`configureXxx()`或`extendXxx()`或`addXxx()`等方法添加自定义的配置。

在添加`ExceptionHandlerExceptionResolver`处理器过程中，会手动调用`exceptionHandlerResolver.afterPropertiesSet();`进行默认初始化。
# 4 异常处理流程
在`DispatcherServlet#doDispatch()`处理请求过程中抛出异常，会在`DispatcherServlet#processDispatchResult()`方法中进行异常处理：
```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {  
   try {    
      try {  
         // 文件请求处理
         processedRequest = checkMultipart(request);  
         // 请求地址映射
         mappedHandler = getHandler(processedRequest);  
         // 获取处理器适配器
         HandlerAdapter ha = getHandlerAdapter(mappedHandler.getHandler());  
         // 拦截器预处理
         if (!mappedHandler.applyPreHandle(processedRequest, response)) {  
            return;  
         }  
         // 实际处理请求
         mv = ha.handle(processedRequest, response, mappedHandler.getHandler());  
         // 拦截器后处理
         mappedHandler.applyPostHandle(processedRequest, response, mv);  
      }  
      catch (Exception ex) {  
         dispatchException = ex;  
      }  
      catch (Throwable err) {  
         // As of 4.3, we're processing Errors thrown from handler methods as well,  
         // making them available for @ExceptionHandler methods and other scenarios.         dispatchException = new NestedServletException("Handler dispatch failed", err);  
      }  
      // 异常处理
      processDispatchResult(processedRequest, response, mappedHandler, mv, dispatchException);  
   }  
}
```

在`DispatcherServlet#processDispatchResult()`方法中，如果监测到了非`ModelAndViewDefiningException`异常，会调用`DispatcherServlet#processHandlerException()`方法进行异常处理：
```java
private void processDispatchResult(HttpServletRequest request, HttpServletResponse response,  
      @Nullable HandlerExecutionChain mappedHandler, @Nullable ModelAndView mv,  
      @Nullable Exception exception) throws Exception {  
   if (exception != null) {  
      if (exception instanceof ModelAndViewDefiningException) {  
         logger.debug("ModelAndViewDefiningException encountered", exception);  
         mv = ((ModelAndViewDefiningException) exception).getModelAndView();  
      }  
      else {  
         Object handler = (mappedHandler != null ? mappedHandler.getHandler() : null);  
         mv = processHandlerException(request, response, handler, exception);  
         errorView = (mv != null);  
      }  
   }  
}
```

在`DispatcherServlet#processHandlerException()`方法中，会遍历调用异常处理器进行处理：
```java
protected ModelAndView processHandlerException(HttpServletRequest request, HttpServletResponse response,  
      @Nullable Object handler, Exception ex) throws Exception {  
   // Check registered HandlerExceptionResolvers...  
   ModelAndView exMv = null;  
   if (this.handlerExceptionResolvers != null) {  
      for (HandlerExceptionResolver resolver : this.handlerExceptionResolvers) {  
         exMv = resolver.resolveException(request, response, handler, ex);  
         if (exMv != null) {  
            break;  
         }  
      }  
   }  
   // ……
}
```

由于默认初始化添加的是`HandlerExceptionResolverComposite`处理器，这里会进入到该类的`resolveException()`方法，遍历其缓存的实际异常处理器进行处理：
```java
public ModelAndView resolveException(  
      HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {  
  
   if (this.resolvers != null) {  
      for (HandlerExceptionResolver handlerExceptionResolver : this.resolvers) {  
         ModelAndView mav = handlerExceptionResolver.resolveException(request, response, handler, ex);  
         if (mav != null) {  
            return mav;  
         }  
      }  
   }  
   return null;  
}
```

对于`ExceptionHandlerExceptionResolver`处理器，首先会调用其父类`AbstractHandlerExceptionResolver`的`resolveException()`方法：
```java
public ModelAndView resolveException(  
      HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {  
  
   if (shouldApplyTo(request, handler)) {  
      prepareResponse(ex, response);  
      ModelAndView result = doResolveException(request, response, handler, ex);  
      if (result != null) {  
         // Print debug message when warn logger is not enabled.  
         if (logger.isDebugEnabled() && (this.warnLogger == null || !this.warnLogger.isWarnEnabled())) {  
            logger.debug(buildLogMessage(ex, request) + (result.isEmpty() ? "" : " to " + result));  
         }  
         // Explicitly configured warn logger in logException method.  
         logException(ex, request);  
      }  
      return result;  
   }  
   else {  
      return null;  
   }  
}
```

接着会调用`AbstractHandlerMethodExceptionResolver`父类的`doResolveException()`方法，进行`handler`的类型转换：
```java
protected final ModelAndView doResolveException(  
      HttpServletRequest request, HttpServletResponse response, @Nullable Object handler, Exception ex) {    
   HandlerMethod handlerMethod = (handler instanceof HandlerMethod ? (HandlerMethod) handler : null);  
   return doResolveHandlerMethodException(request, response, handlerMethod, ex);  
}
```

最后调用`ExceptionHandlerExceptionResolver#doResolveHandlerMethodException()`进行实际异常处理。主要包括以下步骤：
1. 从缓存中获取异常对应的处理方法。
2. 添加`argumentResolvers`和`returnValueHandlers`。
3. 解析异常，作为请求参数。
4. 调用异常处理方法进行异常处理。

```java
protected ModelAndView doResolveHandlerMethodException(HttpServletRequest request,  
      HttpServletResponse response, @Nullable HandlerMethod handlerMethod, Exception exception) {    
   // 从缓存中获取异常对应的处理方法
   ServletInvocableHandlerMethod exceptionHandlerMethod = getExceptionHandlerMethod(handlerMethod, exception);  
   if (exceptionHandlerMethod == null) {  
      return null;  
   }  
  
   // 添加argumentResolvers和returnValueHandlers
   if (this.argumentResolvers != null) {  
      exceptionHandlerMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);  
   }  
   if (this.returnValueHandlers != null) {  
      exceptionHandlerMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);  
   }  
  
   ServletWebRequest webRequest = new ServletWebRequest(request, response);  
   ModelAndViewContainer mavContainer = new ModelAndViewContainer();  
  
   // 递归添加抛出的异常，作为请求参数
   ArrayList<Throwable> exceptions = new ArrayList<>();  
   try {  
      if (logger.isDebugEnabled()) {  
         logger.debug("Using @ExceptionHandler " + exceptionHandlerMethod);  
      }  
      // Expose causes as provided arguments as well  
      Throwable exToExpose = exception;  
      while (exToExpose != null) {  
         exceptions.add(exToExpose);  
         Throwable cause = exToExpose.getCause();  
         exToExpose = (cause != exToExpose ? cause : null);  
      }  
      Object[] arguments = new Object[exceptions.size() + 1];  
      exceptions.toArray(arguments);  // efficient arraycopy call in ArrayList  
      arguments[arguments.length - 1] = handlerMethod;  
      // 调用异常处理方法进行异常处理
      exceptionHandlerMethod.invokeAndHandle(webRequest, mavContainer, arguments);  
   }  
   catch (Throwable invocationEx) {  
      return null;  
   }  
   if (mavContainer.isRequestHandled()) {  
      return new ModelAndView();  
   }  
}
```

在熟悉`ServletInvocableHandlerMethod#invokeAndHandle()`方法中，会进行参数解析、反射调用处理方法和返回值处理：
```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,  
      Object... providedArgs) throws Exception {  
   // 参数解析，反射调用处理方法
   Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);  
   // 返回值处理
   try {  
      this.returnValueHandlers.handleReturnValue(  
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);  
   }  
   catch (Exception ex) {        
      throw ex;  
   }  
}
```

需要注意的是，在`InvocableHandlerMethod#getMethodArgumentValues()`方法中进行参数解析时，一方面，由于提供了异常对象作为`providedArgs`，可以直接根据类型解析到对应形参对象；另一方面，还可以通过参数解析器从`request`中解析其他数据作为形参对象（跟控制层中一样）：
```java
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer,  
      Object... providedArgs) throws Exception {  
  
   MethodParameter[] parameters = getMethodParameters();  
   if (ObjectUtils.isEmpty(parameters)) {  
      return EMPTY_ARGS;  
   }  
  
   Object[] args = new Object[parameters.length];  
   for (int i = 0; i < parameters.length; i++) {  
      MethodParameter parameter = parameters[i];  
      parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);  
      // 将异常对象解析到对应的形参中
      args[i] = findProvidedArgument(parameter, providedArgs);  
      if (args[i] != null) {  
         continue;  
      }  
      if (!this.resolvers.supportsParameter(parameter)) {  
         throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));  
      }  
      try {  
         args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);  
      }  
      catch (Exception ex) {  
         // Leave stack trace for later, exception may actually be resolved and handled...  
         if (logger.isDebugEnabled()) {  
            String exMsg = ex.getMessage();  
            if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {  
               logger.debug(formatArgumentError(parameter, exMsg));  
            }  
         }  
         throw ex;  
      }  
   }  
   return args;  
}
```

自此，我们完成了`ExceptionHandlerExceptionResolver`的异常处理流程。