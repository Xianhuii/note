# 初始化
`org.springframework.web.servlet.DispatcherServlet#initHandlerAdapters`：
```java
/**  
 * Initialize the HandlerAdapters used by this class. * <p>If no HandlerAdapter beans are defined in the BeanFactory for this namespace,  
 * we default to SimpleControllerHandlerAdapter. */private void initHandlerAdapters(ApplicationContext context) {  
   this.handlerAdapters = null;  
  
   if (this.detectAllHandlerAdapters) {  
      // Find all HandlerAdapters in the ApplicationContext, including ancestor contexts.  
      Map<String, HandlerAdapter> matchingBeans =  
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerAdapter.class, true, false);  
      if (!matchingBeans.isEmpty()) {  
         this.handlerAdapters = new ArrayList<>(matchingBeans.values());  
         // We keep HandlerAdapters in sorted order.  
         AnnotationAwareOrderComparator.sort(this.handlerAdapters);  
      }  
   }  
   else {  
      try {  
         HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);  
         this.handlerAdapters = Collections.singletonList(ha);  
      }  
      catch (NoSuchBeanDefinitionException ex) {  
         // Ignore, we'll add a default HandlerAdapter later.  
      }  
   }  
  
   // Ensure we have at least some HandlerAdapters, by registering  
   // default HandlerAdapters if no other adapters are found.   if (this.handlerAdapters == null) {  
      this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);  
      if (logger.isTraceEnabled()) {  
         logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +  
               "': using default strategies from DispatcherServlet.properties");  
      }  
   }  
}
```
# RequestMappingHandlerAdapter
## 1 初始化
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#requestMappingHandlerAdapter`：
```java
@Bean  
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
`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#afterPropertiesSet`：
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
## 2 获取HandlerAdapter
`HandlerAdapter`和`HanderMapping`一一对应，`HandlerAdapter`根据`HandlerMapping`返回的`handler`类型进行判断。
`org.springframework.web.servlet.DispatcherServlet#getHandlerAdapter`：
```java
/**  
 * Return the HandlerAdapter for this handler object. * @param handler the handler object to find an adapter for  
 * @throws ServletException if no HandlerAdapter can be found for the handler. This is a fatal error.  
 */protected HandlerAdapter getHandlerAdapter(Object handler) throws ServletException {  
   if (this.handlerAdapters != null) {  
      for (HandlerAdapter adapter : this.handlerAdapters) {  
         if (adapter.supports(handler)) {  
            return adapter;  
         }  
      }  
   }  
   throw new ServletException("No adapter for handler [" + handler +  
         "]: The DispatcherServlet configuration needs to include a HandlerAdapter that supports this handler");  
}
```
例如，`org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter#supports`：
```java
/**  
 * This implementation expects the handler to be an {@link HandlerMethod}.  
 * @param handler the handler instance to check  
 * @return whether this adapter can adapt the given handler */@Override  
public final boolean supports(Object handler) {  
   return (handler instanceof HandlerMethod && supportsInternal((HandlerMethod) handler));  
}
```
## 3 执行handler
`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#handleInternal`：
```java
protected ModelAndView handleInternal(HttpServletRequest request,  
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {  
  
   ModelAndView mav;  
   checkRequest(request);  
  
   // Execute invokeHandlerMethod in synchronized block if required.  
   if (this.synchronizeOnSession) {  
      HttpSession session = request.getSession(false);  
      if (session != null) {  
         Object mutex = WebUtils.getSessionMutex(session);  
         synchronized (mutex) {  
            mav = invokeHandlerMethod(request, response, handlerMethod);  
         }  
      }  
      else {  
         // No HttpSession available -> no mutex necessary  
         mav = invokeHandlerMethod(request, response, handlerMethod);  
      }  
   }  
   else {  
      // No synchronization on session demanded at all...  
      mav = invokeHandlerMethod(request, response, handlerMethod);  
   }  
  
   if (!response.containsHeader(HEADER_CACHE_CONTROL)) {  
      if (getSessionAttributesHandler(handlerMethod).hasSessionAttributes()) {  
         applyCacheSeconds(response, this.cacheSecondsForSessionAttributeHandlers);  
      }  
      else {  
         prepareResponse(response);  
      }  
   }  
  
   return mav;  
}
```

`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter#invokeHandlerMethod`：
```java
/**  
 * Invoke the {@link RequestMapping} handler method preparing a {@link ModelAndView}  
 * if view resolution is required. * @since 4.2 * @see #createInvocableHandlerMethod(HandlerMethod)  
 */@Nullable  
protected ModelAndView invokeHandlerMethod(HttpServletRequest request,  
      HttpServletResponse response, HandlerMethod handlerMethod) throws Exception {  
  
   ServletWebRequest webRequest = new ServletWebRequest(request, response);  
   try {  
      WebDataBinderFactory binderFactory = getDataBinderFactory(handlerMethod);  
      ModelFactory modelFactory = getModelFactory(handlerMethod, binderFactory);  
  
      ServletInvocableHandlerMethod invocableMethod = createInvocableHandlerMethod(handlerMethod);  
      if (this.argumentResolvers != null) {  
         invocableMethod.setHandlerMethodArgumentResolvers(this.argumentResolvers);  
      }  
      if (this.returnValueHandlers != null) {  
         invocableMethod.setHandlerMethodReturnValueHandlers(this.returnValueHandlers);  
      }  
      invocableMethod.setDataBinderFactory(binderFactory);  
      invocableMethod.setParameterNameDiscoverer(this.parameterNameDiscoverer);  
  
      ModelAndViewContainer mavContainer = new ModelAndViewContainer();  
      mavContainer.addAllAttributes(RequestContextUtils.getInputFlashMap(request));  
      modelFactory.initModel(webRequest, mavContainer, invocableMethod);  
      mavContainer.setIgnoreDefaultModelOnRedirect(this.ignoreDefaultModelOnRedirect);  
  
      AsyncWebRequest asyncWebRequest = WebAsyncUtils.createAsyncWebRequest(request, response);  
      asyncWebRequest.setTimeout(this.asyncRequestTimeout);  
  
      WebAsyncManager asyncManager = WebAsyncUtils.getAsyncManager(request);  
      asyncManager.setTaskExecutor(this.taskExecutor);  
      asyncManager.setAsyncWebRequest(asyncWebRequest);  
      asyncManager.registerCallableInterceptors(this.callableInterceptors);  
      asyncManager.registerDeferredResultInterceptors(this.deferredResultInterceptors);  
  
      if (asyncManager.hasConcurrentResult()) {  
         Object result = asyncManager.getConcurrentResult();  
         mavContainer = (ModelAndViewContainer) asyncManager.getConcurrentResultContext()[0];  
         asyncManager.clearConcurrentResult();  
         LogFormatUtils.traceDebug(logger, traceOn -> {  
            String formatted = LogFormatUtils.formatValue(result, !traceOn);  
            return "Resume with async result [" + formatted + "]";  
         });  
         invocableMethod = invocableMethod.wrapConcurrentResult(result);  
      }  
  
      invocableMethod.invokeAndHandle(webRequest, mavContainer);  
      if (asyncManager.isConcurrentHandlingStarted()) {  
         return null;  
      }  
  
      return getModelAndView(mavContainer, modelFactory, webRequest);  
   }  
   finally {  
      webRequest.requestCompleted();  
   }  
}
```

`org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod#invokeAndHandle`：
```java
/**  
 * Invoke the method and handle the return value through one of the * configured {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers}.  
 * @param webRequest the current request  
 * @param mavContainer the ModelAndViewContainer for this request  
 * @param providedArgs "given" arguments matched by type (not resolved)  
 */public void invokeAndHandle(ServletWebRequest webRequest, ModelAndViewContainer mavContainer,  
      Object... providedArgs) throws Exception {  
  
   Object returnValue = invokeForRequest(webRequest, mavContainer, providedArgs);  
   setResponseStatus(webRequest);  
  
   if (returnValue == null) {  
      if (isRequestNotModified(webRequest) || getResponseStatus() != null || mavContainer.isRequestHandled()) {  
         disableContentCachingIfNecessary(webRequest);  
         mavContainer.setRequestHandled(true);  
         return;  
      }  
   }   else if (StringUtils.hasText(getResponseStatusReason())) {  
      mavContainer.setRequestHandled(true);  
      return;  
   }  
  
   mavContainer.setRequestHandled(false);  
   Assert.state(this.returnValueHandlers != null, "No return value handlers");  
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

解析形参`org.springframework.web.method.support.InvocableHandlerMethod#getMethodArgumentValues`：
```java
protected Object[] getMethodArgumentValues(NativeWebRequest request, @Nullable ModelAndViewContainer mavContainer, Object... providedArgs) throws Exception {  
    MethodParameter[] parameters = this.getMethodParameters();  
    if (ObjectUtils.isEmpty(parameters)) {  
        return EMPTY_ARGS;  
    } else {  
        Object[] args = new Object[parameters.length];  
  
        for(int i = 0; i < parameters.length; ++i) {  
            MethodParameter parameter = parameters[i];  
            parameter.initParameterNameDiscovery(this.parameterNameDiscoverer);  
            args[i] = findProvidedArgument(parameter, providedArgs);  
            if (args[i] == null) {  
                if (!this.resolvers.supportsParameter(parameter)) {  
                    throw new IllegalStateException(formatArgumentError(parameter, "No suitable resolver"));  
                }  
  
                try {  
                    args[i] = this.resolvers.resolveArgument(parameter, mavContainer, request, this.dataBinderFactory);  
                } catch (Exception var10) {  
                    if (logger.isDebugEnabled()) {  
                        String exMsg = var10.getMessage();  
                        if (exMsg != null && !exMsg.contains(parameter.getExecutable().toGenericString())) {  
                            logger.debug(formatArgumentError(parameter, exMsg));  
                        }  
                    }  
  
                    throw var10;  
                }  
            }  
        }  
  
        return args;  
    }  
}
```
实际执行Controller方法，`org.springframework.web.method.support.InvocableHandlerMethod#doInvoke`：
```java
/**  
 * Invoke the handler method with the given argument values. */@Nullable  
protected Object doInvoke(Object... args) throws Exception {  
   Method method = getBridgedMethod();  
   try {  
      if (KotlinDetector.isSuspendingFunction(method)) {  
         return CoroutinesUtils.invokeSuspendingFunction(method, getBean(), args);  
      }  
      return method.invoke(getBean(), args);  
   }  
   catch (IllegalArgumentException ex) {  
      assertTargetBean(method, getBean(), args);  
      String text = (ex.getMessage() != null ? ex.getMessage() : "Illegal argument");  
      throw new IllegalStateException(formatInvokeError(text, args), ex);  
   }  
   catch (InvocationTargetException ex) {  
      // Unwrap for HandlerExceptionResolvers ...  
      Throwable targetException = ex.getTargetException();  
      if (targetException instanceof RuntimeException) {  
         throw (RuntimeException) targetException;  
      }  
      else if (targetException instanceof Error) {  
         throw (Error) targetException;  
      }  
      else if (targetException instanceof Exception) {  
         throw (Exception) targetException;  
      }  
      else {  
         throw new IllegalStateException(formatInvokeError("Invocation failure", args), targetException);  
      }  
   }}
```
返回值处理，`org.springframework.web.method.support.HandlerMethodReturnValueHandlerComposite#handleReturnValue`：
```java
/**  
 * Iterate over registered {@link HandlerMethodReturnValueHandler HandlerMethodReturnValueHandlers} and invoke the one that supports it.  
 * @throws IllegalStateException if no suitable {@link HandlerMethodReturnValueHandler} is found.  
 */@Override  
public void handleReturnValue(@Nullable Object returnValue, MethodParameter returnType,  
      ModelAndViewContainer mavContainer, NativeWebRequest webRequest) throws Exception {  
  
   HandlerMethodReturnValueHandler handler = selectHandler(returnValue, returnType);  
   if (handler == null) {  
      throw new IllegalArgumentException("Unknown return value type: " + returnType.getParameterType().getName());  
   }  
   handler.handleReturnValue(returnValue, returnType, mavContainer, webRequest);  
}
```