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
`RequestMappingHandlerAdapter`中有许多成员变量，在请求处理过程中起着重要的作用。

## 0.1 argumentResolvers
`argumentResolvers`是参数解析器，`RequestMappingHandlerAdapter`使用`argumentResolvers`进行参数解析。

简单来说，就是将HTTP请求中的数据，转换成`handler`方法中的形参对象。

`argumentResolvers`使用了组合模式，它的类型是`HandlerMethodArgumentResolverComposite`，其内部缓存`HandlerMethodArgumentResolver`对象，用来进行参数解析。

`HandlerMethodArgumentResolverComposite`中包含`argumentResolvers`和`argumentResolverCache`两个成员变量。在初始化时，会将所有配置的参数解析器缓存到`argumentResolvers`中。第一次解析参数时，会遍历`argumentResolvers`获取对应参数解析器，并缓存到`argumentResolverCache`中，后续再次解析该参数可直接从键值对中获取，提高效率。

![[HandlerMethodArgumentResolverComposite.png]]

实际进行参数解析的是`HandlerMethodArgumentResolver`实现类。它们使用了策略模式，通过`supportsParameter()`方法获取支持的参数解析器，通过`resolveArgument()`方法进行参数解析。

![[HandlerMethodArgumentResolver.png]]

## 0.2 customArgumentResolvers
`customArgumentResolvers`是用于缓存开发人员自定义的参数解析器，即通过`WebMvcConfigurer#addArgumentResolvers()`方法添加的解析器。

在`RequestMappingHandlerAdapter`初始化时，会将`customArgumentResolvers`中的自定义参数解析器添加到`argumentResolvers`中。

## 0.3 returnValueHandlers
`returnValueHandlers`是返回值处理器，它可以对控制层业务返回值进行处理。

例如，对`@ResponseBody`标注的返回值进行JSON格式化，并写到输出流。

`returnValueHandlers`使用了组合模式，它的类型是`HandlerMethodReturnValueHandlerComposite`，其内部缓存`HandlerMethodReturnValueHandler`对象，用来进行返回值处理。

![[HandlerMethodReturnValueHandlerComposite 1.png]]

## 0.4 customReturnValueHandlers
`customReturnValueHandlers`是用于缓存开发人员自定义的参数解析器，即通过`WebMvcConfigurer#addReturnValueHandlers()`方法添加的解析器。

在`RequestMappingHandlerAdapter`初始化时，会将`customReturnValueHandlers`中的自定义参数解析器添加到`returnValueHandlers`中。

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
在`RequestMappingHandlerAdapter#afterPropertiesSet()`方法中，会对`argumentResolvers`、`initBinderArgumentResolvers`和`returnValueHandlers`等进行初始化：
```java
public void afterPropertiesSet() {  
   // Do this first, it may add ResponseBody advice beans  
   initControllerAdviceCache();  
  
   if (this.argumentResolvers == null) {  
      List<HandlerMethodArgumentResolver> resolvers = getDefaultArgumentResolvers();  
      this.argumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);  
   }  
   if (this.initBinderArgumentResolvers == null) {  
      List<HandlerMethodArgumentResolver> resolvers = getDefaultInitBinderArgumentResolvers();  
      this.initBinderArgumentResolvers = new HandlerMethodArgumentResolverComposite().addResolvers(resolvers);  
   }  
   if (this.returnValueHandlers == null) {  
      List<HandlerMethodReturnValueHandler> handlers = getDefaultReturnValueHandlers();  
      this.returnValueHandlers = new HandlerMethodReturnValueHandlerComposite().addHandlers(handlers);  
   }  
}
```
### 1.2.1 initControllerAdviceCache
在`RequestMappingHandlerAdapter#initControllerAdviceCache()`方法中，会从容器中获取所有`@ControllerAdvice`标注的`bean`。然后缓存这些`bean`中标注`@RequestMapping&@ModelAttribute`（`modelAttributeAdviceCache`）和`@InitBinder`（`initBinderAdviceChache`）等注解的方法，并且直接缓存实现`RequestBodyAdvice`或`ResponseBodyAdvice`的`bean`（`requestResponseBodyAdvice`）。
```java
private void initControllerAdviceCache() {  
   if (getApplicationContext() == null) {  
      return;  
   }  
  
   List<ControllerAdviceBean> adviceBeans = ControllerAdviceBean.findAnnotatedBeans(getApplicationContext());  
  
   List<Object> requestResponseBodyAdviceBeans = new ArrayList<>();  
  
   for (ControllerAdviceBean adviceBean : adviceBeans) {  
      Class<?> beanType = adviceBean.getBeanType();  
      if (beanType == null) {  
         throw new IllegalStateException("Unresolvable type for ControllerAdviceBean: " + adviceBean);  
      }  
      Set<Method> attrMethods = MethodIntrospector.selectMethods(beanType, MODEL_ATTRIBUTE_METHODS);  
      if (!attrMethods.isEmpty()) {  
         this.modelAttributeAdviceCache.put(adviceBean, attrMethods);  
      }  
      Set<Method> binderMethods = MethodIntrospector.selectMethods(beanType, INIT_BINDER_METHODS);  
      if (!binderMethods.isEmpty()) {  
         this.initBinderAdviceCache.put(adviceBean, binderMethods);  
      }  
      if (RequestBodyAdvice.class.isAssignableFrom(beanType) || ResponseBodyAdvice.class.isAssignableFrom(beanType)) {  
         requestResponseBodyAdviceBeans.add(adviceBean);  
      }  
   }  
  
   if (!requestResponseBodyAdviceBeans.isEmpty()) {  
      this.requestResponseBodyAdvice.addAll(0, requestResponseBodyAdviceBeans);  
   }  
}
```

### 1.2.2 getDefaultXxx()方法
通过`getDefaultArgumentResolvers()`、`getDefaultInitBinderArgumentResolvers()`和`getDefaultResurnValueHandlers()`方法分别对`argumentResolvers`、`initBinderArgumentResolvers`和`returnValueHandlers`进行初始化。

在这些`getDefaultXxx()`方法中，一方面会按一定顺序添加一系列默认的处理器对象，另一方面会通过`getCustomXxx()`方法获取开发人员自定义的处理器对象（可通过`WevMvcConfigurer`添加）。

例如，`RequestMappingHandlerAdapter#getDefaultArgumentResolvers()`方法会添加一系列默认的参数解析器，并且通过`getCustomArgumentResolvers()`方法获取开发人员自定义的参数解析器：
```java
private List<HandlerMethodArgumentResolver> getDefaultArgumentResolvers() {  
   List<HandlerMethodArgumentResolver> resolvers = new ArrayList<>(30);  
  
   // Annotation-based argument resolution  
   resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), false));  
   resolvers.add(new RequestParamMapMethodArgumentResolver());  
   resolvers.add(new PathVariableMethodArgumentResolver());  
   resolvers.add(new PathVariableMapMethodArgumentResolver());  
   resolvers.add(new MatrixVariableMethodArgumentResolver());  
   resolvers.add(new MatrixVariableMapMethodArgumentResolver());  
   resolvers.add(new ServletModelAttributeMethodProcessor(false));  
   resolvers.add(new RequestResponseBodyMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));  
   resolvers.add(new RequestPartMethodArgumentResolver(getMessageConverters(), this.requestResponseBodyAdvice));  
   resolvers.add(new RequestHeaderMethodArgumentResolver(getBeanFactory()));  
   resolvers.add(new RequestHeaderMapMethodArgumentResolver());  
   resolvers.add(new ServletCookieValueMethodArgumentResolver(getBeanFactory()));  
   resolvers.add(new ExpressionValueMethodArgumentResolver(getBeanFactory()));  
   resolvers.add(new SessionAttributeMethodArgumentResolver());  
   resolvers.add(new RequestAttributeMethodArgumentResolver());  
  
   // Type-based argument resolution  
   resolvers.add(new ServletRequestMethodArgumentResolver());  
   resolvers.add(new ServletResponseMethodArgumentResolver());  
   resolvers.add(new HttpEntityMethodProcessor(getMessageConverters(), this.requestResponseBodyAdvice));  
   resolvers.add(new RedirectAttributesMethodArgumentResolver());  
   resolvers.add(new ModelMethodProcessor());  
   resolvers.add(new MapMethodProcessor());  
   resolvers.add(new ErrorsMethodArgumentResolver());  
   resolvers.add(new SessionStatusMethodArgumentResolver());  
   resolvers.add(new UriComponentsBuilderMethodArgumentResolver());  
   if (KotlinDetector.isKotlinPresent()) {  
      resolvers.add(new ContinuationHandlerMethodArgumentResolver());  
   }  
  
   // Custom arguments  
   if (getCustomArgumentResolvers() != null) {  
      resolvers.addAll(getCustomArgumentResolvers());  
   }  
  
   // Catch-all  
   resolvers.add(new PrincipalMethodArgumentResolver());  
   resolvers.add(new RequestParamMethodArgumentResolver(getBeanFactory(), true));  
   resolvers.add(new ServletModelAttributeMethodProcessor(true));  
  
   return resolvers;
```

## 1.3 WebMvcConfigurationSupport
在`WebMvcConfigurationSupport#requestMappingHandlerAdapter()`中，会完成`requestMappingHandlerAdapter`的`bean`的创建，对`contentNegotiationManager`、`messageConverters`、`webBindingInitializer`、`customArgumentResolvers`和`customReturnValueHandlers`等基础成员变量，以及异步请求的`taskExecutor`、`asyncRequestTimeout`、`callableInterceptors`和`deferredResultInterceptors`等成员变量进行初始化：
```java
public RequestMappingHandlerAdapter requestMappingHandlerAdapter(  
      @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,  
      @Qualifier("mvcConversionService") FormattingConversionService conversionService,  
      @Qualifier("mvcValidator") Validator validator) {  
  
   RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();  
   adapter.setContentNegotiationManager(contentNegotiationManager);  
   adapter.setMessageConverters(getMessageConverters());  
   adapter.setWebBindingInitializer(getConfigurableWebBindingInitializer(conversionService, validator));  
   adapter.setCustomArgumentResolvers(getArgumentResolvers());  
   adapter.setCustomReturnValueHandlers(getReturnValueHandlers());  
  
   if (jackson2Present) {  
      adapter.setRequestBodyAdvice(Collections.singletonList(new JsonViewRequestBodyAdvice()));  
      adapter.setResponseBodyAdvice(Collections.singletonList(new JsonViewResponseBodyAdvice()));  
   }  
  
   AsyncSupportConfigurer configurer = getAsyncSupportConfigurer();  
   if (configurer.getTaskExecutor() != null) {  
      adapter.setTaskExecutor(configurer.getTaskExecutor());  
   }  
   if (configurer.getTimeout() != null) {  
      adapter.setAsyncRequestTimeout(configurer.getTimeout());  
   }  
   adapter.setCallableInterceptors(configurer.getCallableInterceptors());  
   adapter.setDeferredResultInterceptors(configurer.getDeferredResultInterceptors());  
  
   return adapter;  
}
```

在初始化过程中，一方面会为这些成员添加一系列默认对象，另一方面会从`WebMvcConfigurer`中获取开发人员自定义的对象。

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
## 3.1 RequestResponseBodyMethodProcessor
`RequestResponseBodyMethodProcessor`是前后端分离项目中使用最多的`HandlerMethodArgumentResolver`实现类，它可以处理`@RequestBody`标注的形参。

### 3.1.1 supportsParameter()方法
`RequestResponseBodyMethodProcessor#supportsParameter()`方法会判断形参上是否标注`@RequestBody`注解：
```java
public boolean supportsParameter(MethodParameter parameter) {  
   return parameter.hasParameterAnnotation(RequestBody.class);  
}
```

### 3.1.2 resolveArgument()方法
`RequestResponseBodyMethodProcessor#resolveArgument()`方法会从输入流中读取数据，转换成形参对象，并且对其进行数据校验：
```java
public Object resolveArgument(MethodParameter parameter, @Nullable ModelAndViewContainer mavContainer,  
      NativeWebRequest webRequest, @Nullable WebDataBinderFactory binderFactory) throws Exception {  
  
   parameter = parameter.nestedIfOptional();  
   // 从输入流中读取数据，并构造成形参对象
   Object arg = readWithMessageConverters(webRequest, parameter, parameter.getNestedGenericParameterType());  
   String name = Conventions.getVariableNameForParameter(parameter);  
  
   if (binderFactory != null) {  
      WebDataBinder binder = binderFactory.createBinder(webRequest, arg, name);  
      if (arg != null) {  
         // 数据校验
         validateIfApplicable(binder, parameter);  
         if (binder.getBindingResult().hasErrors() && isBindExceptionRequired(binder, parameter)) {  
            throw new MethodArgumentNotValidException(parameter, binder.getBindingResult());  
         }  
      }  
      if (mavContainer != null) {  
         mavContainer.addAttribute(BindingResult.MODEL_KEY_PREFIX + name, binder.getBindingResult());  
      }  
   }  
  
   return adaptArgumentIfNecessary(arg, parameter);  
}
```

`AbstractMessageConverterMethodArgumentResolver#readWithMessageConverters()`方法会根据`Content-Type`从输入流读取数据，并创建成形参对象：
```java
protected <T> Object readWithMessageConverters(HttpInputMessage inputMessage, MethodParameter parameter,  
      Type targetType) throws IOException, HttpMediaTypeNotSupportedException, HttpMessageNotReadableException {  
  
   // 获取请求Content-Type
   MediaType contentType;  
   boolean noContentType = false;  
   try {  
      contentType = inputMessage.getHeaders().getContentType();  
   }  
   catch (InvalidMediaTypeException ex) {  
      throw new HttpMediaTypeNotSupportedException(ex.getMessage());  
   }  
   if (contentType == null) {  
      noContentType = true;  
      contentType = MediaType.APPLICATION_OCTET_STREAM;  
   }  
  
   // 获取形参类型
   Class<?> contextClass = parameter.getContainingClass();  
   Class<T> targetClass = (targetType instanceof Class ? (Class<T>) targetType : null);  
   if (targetClass == null) {  
      ResolvableType resolvableType = ResolvableType.forMethodParameter(parameter);  
      targetClass = (Class<T>) resolvableType.resolve();  
   }  
  
   HttpMethod httpMethod = (inputMessage instanceof HttpRequest ? ((HttpRequest) inputMessage).getMethod() : null);  
   Object body = NO_VALUE;  
  
   // 根据Content-Type使用对应messageConverter读取并转换数据
   EmptyBodyCheckingHttpInputMessage message = null;  
   try {  
      message = new EmptyBodyCheckingHttpInputMessage(inputMessage);  
  
      for (HttpMessageConverter<?> converter : this.messageConverters) {  
         Class<HttpMessageConverter<?>> converterType = (Class<HttpMessageConverter<?>>) converter.getClass();  
         GenericHttpMessageConverter<?> genericConverter =  
               (converter instanceof GenericHttpMessageConverter ? (GenericHttpMessageConverter<?>) converter : null);  
         // 根据Content-Type获取对应的messageConverter
         if (genericConverter != null ? genericConverter.canRead(targetType, contextClass, contentType) :  
               (targetClass != null && converter.canRead(targetClass, contentType))) {  
            if (message.hasBody()) {  
               // RequestBodyAdvice#beforeBodyRead()处理
               HttpInputMessage msgToUse =  
                     getAdvice().beforeBodyRead(message, parameter, targetType, converterType);  
               // 读取并转换数据
               body = (genericConverter != null ? genericConverter.read(targetType, contextClass, msgToUse) :  
                     ((HttpMessageConverter<T>) converter).read(targetClass, msgToUse));  
               // RequestBodyAdvice#afterBodyRead()处理
               body = getAdvice().afterBodyRead(body, msgToUse, parameter, targetType, converterType);  
            }  
            else {  
               body = getAdvice().handleEmptyBody(null, message, parameter, targetType, converterType);  
            }  
            break;  
         }  
      }  
   }  
   catch (IOException ex) {  
      throw new HttpMessageNotReadableException("I/O error while reading input message", ex, inputMessage);  
   }  
   finally {  
      if (message != null && message.hasBody()) {  
         closeStreamIfNecessary(message.getBody());  
      }  
   }  
  
   if (body == NO_VALUE) {  
      if (httpMethod == null || !SUPPORTED_METHODS.contains(httpMethod) ||  
            (noContentType && !message.hasBody())) {  
         return null;  
      }  
      throw new HttpMediaTypeNotSupportedException(contentType,  
            getSupportedMediaTypes(targetClass != null ? targetClass : Object.class));  
   }  
  
   MediaType selectedContentType = contentType;  
   Object theBody = body;  
   LogFormatUtils.traceDebug(logger, traceOn -> {  
      String formatted = LogFormatUtils.formatValue(theBody, !traceOn);  
      return "Read \"" + selectedContentType + "\" to [" + formatted + "]";  
   });  
  
   return body;  
}
```

`AbstractMessageConverterMethodArgumentResolver#validateIfApplicable()`方法会对标注`javax.validation.Valid`、`org.springframework.validation.annotation.Validated`以及以`Valid`开头的自定义注解进行参数校验：
```java
protected void validateIfApplicable(WebDataBinder binder, MethodParameter parameter) {  
   Annotation[] annotations = parameter.getParameterAnnotations();  
   for (Annotation ann : annotations) {  
      Object[] validationHints = ValidationAnnotationUtils.determineValidationHints(ann);  
      if (validationHints != null) {  
         binder.validate(validationHints);  
         break;  
      }  
   }  
}
```

# 4 HandlerMethodReturnValueHandler实现类
## 4.1 RequestResponseBodyMethodProcessor
`RequestResponseBodyMethodProcessor`是前后端分离项目中使用最多的`HandlerMethodReturnValueHandler`实现类，它可以处理`@ResponseBody`标注的返回值。

### 4.1.1 supportsReturnType()方法
`RequestResponseBodyMethodProcessor#supportsReturnType()`方法会判断类或方法上是否标注`@RequestBody`注解：
```java
public boolean supportsReturnType(MethodParameter returnType) {  
   return (AnnotatedElementUtils.hasAnnotation(returnType.getContainingClass(), ResponseBody.class) ||  
         returnType.hasMethodAnnotation(ResponseBody.class));  
}
```

### 4.1.2 handleReturnValue()方法
`RequestResponseBodyMethodProcessor#handleReturnValue()`方法会根据响应的`Content-Type`，将返回值格式化成对应数据格式，写道输出流进行响应：
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

实际业务在`AbstractMessageConverterMethodProcessor#writeWithMessageConverters()`方法，
```java
protected <T> void writeWithMessageConverters(@Nullable T value, MethodParameter returnType,  
      ServletServerHttpRequest inputMessage, ServletServerHttpResponse outputMessage)  
      throws IOException, HttpMediaTypeNotAcceptableException, HttpMessageNotWritableException {  
  
   Object body;  
   Class<?> valueType;  
   Type targetType;  
  
   // 如果返回值是CharSequence类型，valueType和targetType都设置成String类型
   if (value instanceof CharSequence) {  
      body = value.toString();  
      valueType = String.class;  
      targetType = String.class;  
   }  
   // 如果返回值不是CharSequence，valueType设置成对应返回值类型，targetType会设置成解析泛型后的返回值类型
   else {  
      body = value;  
      valueType = getReturnValueType(body, returnType);  
      targetType = GenericTypeResolver.resolveType(getGenericType(returnType), returnType.getContainingClass());  
   }  
   // 如果是返回值继承自Resource
   if (isResourceType(value, returnType)) {  
      outputMessage.getHeaders().set(HttpHeaders.ACCEPT_RANGES, "bytes");  
      if (value != null && inputMessage.getHeaders().getFirst(HttpHeaders.RANGE) != null &&  
            outputMessage.getServletResponse().getStatus() == 200) {  
         Resource resource = (Resource) value;  
         try {  
            List<HttpRange> httpRanges = inputMessage.getHeaders().getRange();  
            outputMessage.getServletResponse().setStatus(HttpStatus.PARTIAL_CONTENT.value());  
            body = HttpRange.toResourceRegions(httpRanges, resource);  
            valueType = body.getClass();  
            targetType = RESOURCE_REGION_LIST_TYPE;  
         }  
         catch (IllegalArgumentException ex) {  
            outputMessage.getHeaders().set(HttpHeaders.CONTENT_RANGE, "bytes */" + resource.contentLength());  
            outputMessage.getServletResponse().setStatus(HttpStatus.REQUESTED_RANGE_NOT_SATISFIABLE.value());  
         }  
      }  
   }  
  
   // 获取响应的Content-Type
   MediaType selectedMediaType = null;  
   MediaType contentType = outputMessage.getHeaders().getContentType();  
   boolean isContentTypePreset = contentType != null && contentType.isConcrete();  
   if (isContentTypePreset) {  
      if (logger.isDebugEnabled()) {  
         logger.debug("Found 'Content-Type:" + contentType + "' in response");  
      }  
      selectedMediaType = contentType;  
   }  
   else {  
      HttpServletRequest request = inputMessage.getServletRequest();  
      List<MediaType> acceptableTypes;  
      try {  
         acceptableTypes = getAcceptableMediaTypes(request);  
      }  
      catch (HttpMediaTypeNotAcceptableException ex) {  
         int series = outputMessage.getServletResponse().getStatus() / 100;  
         if (body == null || series == 4 || series == 5) {  
            if (logger.isDebugEnabled()) {  
               logger.debug("Ignoring error response content (if any). " + ex);  
            }  
            return;  
         }  
         throw ex;  
      }  
      List<MediaType> producibleTypes = getProducibleMediaTypes(request, valueType, targetType);  
  
      if (body != null && producibleTypes.isEmpty()) {  
         throw new HttpMessageNotWritableException(  
               "No converter found for return value of type: " + valueType);  
      }  
      List<MediaType> mediaTypesToUse = new ArrayList<>();  
      for (MediaType requestedType : acceptableTypes) {  
         for (MediaType producibleType : producibleTypes) {  
            if (requestedType.isCompatibleWith(producibleType)) {  
               mediaTypesToUse.add(getMostSpecificMediaType(requestedType, producibleType));  
            }  
         }  
      }  
      if (mediaTypesToUse.isEmpty()) {  
         if (logger.isDebugEnabled()) {  
            logger.debug("No match for " + acceptableTypes + ", supported: " + producibleTypes);  
         }  
         if (body != null) {  
            throw new HttpMediaTypeNotAcceptableException(producibleTypes);  
         }  
         return;  
      }  
  
      MediaType.sortBySpecificityAndQuality(mediaTypesToUse);  
  
      for (MediaType mediaType : mediaTypesToUse) {  
         if (mediaType.isConcrete()) {  
            selectedMediaType = mediaType;  
            break;  
         }  
         else if (mediaType.isPresentIn(ALL_APPLICATION_MEDIA_TYPES)) {  
            selectedMediaType = MediaType.APPLICATION_OCTET_STREAM;  
            break;  
         }  
      }  
  
      if (logger.isDebugEnabled()) {  
         logger.debug("Using '" + selectedMediaType + "', given " +  
               acceptableTypes + " and supported " + producibleTypes);  
      }  
   }  
   // 根据响应Content-Type格式化返回值，并写到输出流
   if (selectedMediaType != null) {  
      selectedMediaType = selectedMediaType.removeQualityValue();  
      for (HttpMessageConverter<?> converter : this.messageConverters) {  
         GenericHttpMessageConverter genericConverter = (converter instanceof GenericHttpMessageConverter ?  
               (GenericHttpMessageConverter<?>) converter : null);  
         // 根据响应Content-Type获取对应的messageConverter
         if (genericConverter != null ?  
               ((GenericHttpMessageConverter) converter).canWrite(targetType, valueType, selectedMediaType) :  
               converter.canWrite(valueType, selectedMediaType)) {  
            // ResponseBodyAdvice的beforeBodyWrite()处理
            body = getAdvice().beforeBodyWrite(body, returnType, selectedMediaType,  
                  (Class<? extends HttpMessageConverter<?>>) converter.getClass(),  
                  inputMessage, outputMessage);  
            if (body != null) {  
               Object theBody = body;  
               LogFormatUtils.traceDebug(logger, traceOn ->  
                     "Writing [" + LogFormatUtils.formatValue(theBody, !traceOn) + "]");  
               addContentDispositionHeader(inputMessage, outputMessage);  
               // 通过messageConverter格式化返回值，并写到输出流
               if (genericConverter != null) {  
                  genericConverter.write(body, targetType, selectedMediaType, outputMessage);  
               }  
               else {  
                  ((HttpMessageConverter) converter).write(body, selectedMediaType, outputMessage);  
               }  
            }  
            else {  
               if (logger.isDebugEnabled()) {  
                  logger.debug("Nothing to write: null body");  
               }  
            }  
            return;  
         }  
      }  
   }  
}
```

