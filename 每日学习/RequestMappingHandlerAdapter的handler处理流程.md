![[RequestMappingHandlerAdapter.png]]

`RequestMappingHandlerAdapter`是日常项目中使用最多的`HandlerAdapter`实现类。

它还有一个抽象父类`AbstractHandlerMethodAdapter`，顾名思义，是专门用来处理`HandlerMethod`类型的`handler`。具体可以看`AbstractHandlerMethodAdapter#supports`方法：
```java
public final boolean supports(Object handler) {  
   return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));  
}
```

通过之前的学习可以知道，`RequestMappingHandlerMapping`获取的`handler`就是`HandlerMethod`类型的。

`RequestMappingHandlerMapping`和`RequestMappingHandlerAdapter`就像一对孪生兄弟：
1. `RequestMappingHandlerMapping`负责根据`request`找到映射的`handler`
2. `RequestMappingHandlerAdapter`负责根据`handler`执行对应的方法

我们先总结`RequestMappingHandlerAdapter`处理`handler`的核心流程：
1. 将`request`和`response`封装成`ServletWebRequest`对象。
2. 将`handler`封装成`ServletInvocableHandlerMethod`对象`invocableMethod`。
3. 为`invocableMethod`设置`argumentResolvers`、`returnValueHandlers`、`dataBinderFactory`和`parameterNameDiscoverer`等工具。
4. 解析请求参数。
5. 执行方法。
6. 处理返回值。

实际上，`RequestMappingHandlerAdapter`处理`handler`过程中还有许多细节，比如前后端不分离项目的视图相关处理（没有必要花费时间深入学习），异步请求的相关处理（会另外写文章）。

# 0 预备知识
`RequestMappingHandlerAdapter`中有许多重要的成员变量，

# 1 初始化流程
在`RequestMappingHandlerAdapter`内部，有两个方法用于初始化。一个是构造函数，另一个是实现`org.springframework.beans.factory.InitializingBean`的`afterPropertiesSet()`方法。

在Spring Boot中，会在`WebMvcConfigurationSupport`中进行完整的初始化。

## 1.1 构造函数
构造函数中主要是对`messageConverters`进行初始化，添加一些必备的消息转换器。实际上，`WebMvcConfigurationSupport`中会进行覆盖，因此不过多描述：
```java
public RequestMappingHandlerAdapter() {  
   this.messageConverters = new ArrayList<>(4);  
   this.messageConverters.add(new ByteArrayHttpMessageConverter());  
   this.messageConverters.add(new StringHttpMessageConverter());  
   if (!shouldIgnoreXml) {  
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

## 1.2 afterPropertiesSet()


## 1.3 WebMvcConfigurationSupport


# 2 同步请求处理流程
首先，`DispatcherServlet`会调用`HandlerAdapter`接口的`handle()`方法。

`AbstractHandlerMethodAdapter`对`handle()`方法的实现只是做了一个类型转换：
```java
public final ModelAndView handle(HttpServletRequest request, HttpServletResponse response, Object handler)  
      throws Exception {  
   return handleInternal(request, response, (HandlerMethod) handler);  
}
```

`AbstractHandlerMethodAdapter#handleInternal()`是一个抽象方法，会由子类具体去实现。

`RequestMappingHandlerAdapter#handlerInternal()`方法中会进行一些请求判断和缓存处理（省略），它的核心是在`invokeHandlerMethod()`方法：
```java
protected ModelAndView handleInternal(HttpServletRequest request,  
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {  
  
   ModelAndView mav;  
   
   mav = invokeHandlerMethod(request, response, handlerMethod);
  
   return mav;  
}
```

## 2.1 预处理：添加处理器
在`RequestMappingHandlerAdapter#invokeHandlerMethod()`方法中，会进行如下处理：
1. 将`request`和`response`封装成`ServletWebRequest`对象，便于后续处理。
2. 将`handler`封装成`ServletInvocableHandlerMethod`对象`invocableMethod`。
3. 为`invocableMethod`设置`argumentResolvers`（参数解析）、`returnValueHandlers`（返回值处理）、`dataBinderFactory`（数据绑定和校验）和`parameterNameDiscoverer`（形参名字解析）等组件，用作后续方法处理的工具。这些组件都来自`RequestMappingHandlerAdapter`的成员变量。
4. 最后会调用`invocableMethod`的`invokeAndHandle()`方法进行实际处理。

`RequestMappingHandlerAdapter#invokeHandlerMethod()`具体源码如下：
```java
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,  
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception { 
   // 1、将`request`和`response`封装成`ServletWebRequest`对象
   ServletWebRequest webRequest = new ServletWebRequest(request, response);  
   try {  
      WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);  
      ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory); 
      // 2、将`handler`封装成`ServletInvocableHandlerMethod`对象`invocableMethod` 
      ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);  
      // 3、为`invocableMethod`设置`argumentResolvers`、`returnValueHandlers`、`dataBinderFactory`和`parameterNameDiscoverer`等工具
      if (this.argumentResolvers != null) {  
         invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);  
      }  
      if (this.returnValueHandlers != null) {  
         invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);  
      }  
      invocableMethod.setDataBinderFactory(binderFactory);  
      invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);  
      // 4、处理请求
      invocableMethod.invokeAndHandle(webRequest, mavContainer);    
      return getModelAndView(mavContainer, modelFactory, webRequest);  
   }  
   finally {  
      webRequest.requestCompleted();  
   }  
}
```

`ServletInvocableHandlerMethod#invokeAndHandle()`方法会调用请求，并且对返回值进行处理：
```java
public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {  
   // 1、调用请求
   Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);  
   // 省略相关代码
   // 2、返回值处理
   try {  
      this.returnValueHandlers.handleReturnValue(  
            returnValue, getReturnValueType(returnValue), mavContainer, webRequest);  
   }  
   catch (Exception ex) {  
      if (logger.isTraceEnabled()) {  
         logger.trace(formatErrorForReturnValue(returnValue), ex);  
      }  
      throw ex;  
   }  
}
```

## 2.2 形参对象解析
在`InvocableHandlerMethod#invokeForRequest()`方法中，会进行参数解析（将`request`中的数据解析成`handler`方法的形参对象），然后通过反射调用对应方法，获取返回值：
```java
public Object invokeForRequest(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {  
   // 1、参数解析
   Object[] args = getMethodArgumentValues(request, mavContainer, providedArgs);  
   // 2、调用方法
   return doInvoke(args);  
}
```

在`InvocableHandlerMethod#getMethodArgumentValues()`方法中，会通过反射获取`handler`方法的形参，然后使用`resolvers`对一个个形参进行解析。

根据形参的类型不同（HttpServletRequest等），形参上标注的注解不同（`@RequestBody`等），会调用不同的解析器实现类进行处理。

根据解析器实现类的不同，在解析过程中，会进行数据绑定、消息转换和参数校验：
```java
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {  
   // 1、获取方法的形参信息
   MethodParameter[] parameters = getMethodParameters();  
   if (ObjectUtils.isEmpty(parameters)) {  
      return EMPTY_ARGS;  
   }  
  
   Object[] args = new Object[parameters.length];  
   // 遍历方法形参
   for (int i = 0; i < parameters.length; i++) {  
      MethodParameter parameter = parameters[i];  
      parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);  
      args[i] = findProvidedArgument(parameter, providedArgs);  
      if (args[i] != null) {  
         continue;  
      }  
      if (!this.resolvers.supportsParameter(parameter)) {  
         throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));  
      }  
      try {  
         // 2、形参解析
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

## 2.3 执行方法
回到`InvocableHandlerMethod#invokeForRequest()`方法，解析方法形参后，会调用`InvocableHandlerMethod#doInvoke()`方法，通过反射调用方法，并传入`handler`对应的控制层`bean`作为触发对象，以及上述形参对象：
```java
protected Object doInvoke(Object... args) throws Exception {  
   Method method = getBridgedMethod();  
   try {  
      return method.invoke(getBean(), args);  
   }  
   catch (IllegalArgumentException ex) {  
      // 省略相关代码
   }  
}
```

## 2.4 返回值处理
回到`ServletInvocableHandlerMethod#invokeAndHandle()`方法，此时获取了`handler`方法执行完成的返回值，会调用`HandlerMethodReturnValueHandlerComposite#handleReturnValue()`方法对返回值进行处理。首先会根据返回值信息`MethodParameter`对象查找支持的返回值处理器`HandlerMethodReturnValueHandler`，然后使用该处理器对返回值进行处理：
```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,  
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {  
   // 1、查找返回值处理器
   HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);  

   // 2、返回值处理
   handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);  
}
```

在`HandlerMethodReturnValueHandlerComposite#selectHandler`方法中，会遍历`returnValueHandlers`，调用其`HandlerMethodReturnValueHandler#supportsReturnType`实现方法找到对应返回值处理器。：
```java
private HandlerMethodReturnValueHandler selectHandler(@Nullable Object value, MethodParameter returnType) {  
   boolean isAsyncValue = isAsyncReturnValue(value, returnType);  
   for (HandlerMethodReturnValueHandler handler : this.returnValueHandlers) {  
      if (isAsyncValue && !(handler instanceof AsyncHandlerMethodReturnValueHandler)) {  
         continue;  
      }  
      if (handler.supportsReturnType(returnType)) {  
         return handler;  
      }  
   }  
   return null;  
}
```

找到返回值处理器后，就可以通过其`handleReturnValue()`方法对返回值进行处理。

举个有实战意义的例子，`@ResponseBody`的`HandlerMethodReturnValueHandler`实现类是`RequestResponseBodyMethodProcessor`。

`RequestResponseBodyMethodProcessor`的`supportsReturnType()`方法会判断返回值是否标有`ResponseBody`注解：
```java
public boolean supportsReturnType(MethodParameter returnType) {  
   return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||  
         returnType.hasMethodAnnotation(ResponseBody.class));  
}
```

`RequestResponseBodyMethodProcessor`的`handleReturnValue()`方法会根据返回的`Content-Type`对返回值进行对应格式化，并写入到输出流中：
```java
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,  
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest)  
      throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {  
  
   mavContainer.setRequestHandled(true);  
   ServletServerHttpRequest inputMessage = createInputMessage(webRequest);  
   ServletServerHttpResponse outputMessage = createOutputMessage(webRequest);  
  
   // Try even with null return value. ResponseBodyAdvice could get involved.  
   writeWithMessageConverters(returnValue, returnType, inputMessage, outputMessage);  
}
```

至此，我们走完了`RequestMappingHandlerAdapter`对同步请求的完整处理流程（前后端分离）。简单来说，会经过一下主要步骤：
1. 初始化请求处理的工具：`argumentResolvers`、`returnValueHandlers`、`binderFactory`和`parameterNameDiscoverer`等。
2. 解析形参对象
3. 执行方法
4. 返回值处理

实际上`RequestMappingHandlerAdapter`中还会对异步请求进行处理，这部分我们会在之后的文章进行详细介绍。

# 3 HandlerMethodArgumentResolver实现类

# 4 HandlerMethodReturnValueHandler实现类