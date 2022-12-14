# 1 HandlerMapping介绍
`HandlerMapping`是Spring MVC的核心组件之一，用来保存`request`-`handler`之间的映射。
简单来说，`request`指的是请求地址（还包括请求方法等），`handler`指的是Controller中对应的方法。
例如，在日常开发时，我们会定义Controller来接收请求：
```java
@RestController
public class TestController {
	@RequestMapping("/hello")
	public String hello() {
		return "Hello HandlerMapping`";
	}
}
```
这里的请求地址`/hello`表示`request`，而`TestController#hello()`就是对应的`handler`。
`HandlerMapping`在初始化的时候，会扫描整个项目，缓存所有Controller的`request`-`handler`映射。
当接收到请求时，会根据请求地址等信息从`HandlerMapping`中找到对应的`handler`，从而执行对应的业务逻辑。
# 2 DispatcherServlet中使用HandlerMapping
`DispatcherServlet`是Spring MVC的核心，它持有`HandlerMapping`作为成员变量：
```java
private List<HandlerMapping> handlerMappings;
```
在初始化时会加载`HandlerMapping`作为成员变量，在处理请求时会从`HandlerMapping`的缓存中找到对应的`handler`。
## 2.1 初始化HandlerMapping
在项目启动时，`DispatcherServlet`会进行`HandlerMapping`初始化。
加载`HandlerMapping`可能存在三种情况：
1. 从Spring容器中获取所有类型为`HandlerMapping`的bean，作为`handerMappings`成员变量。
2. 从Spring容器中获取名为`handlerMapping`的bean，作为`handerMappings`成员变量。
3. 从`DispatcherServlet.properties`配置文件获取默认`HandlerMapping`实现类的全限定类名，实例化并作为`handerMappings`成员变量。
`org.springframework.web.servlet.DispatcherServlet#initHandlerMappings`源码：
```java
private void initHandlerMappings(ApplicationContext context) {  
   this.handlerMappings = null;  
  
   if (this.detectAllHandlerMappings) {  
      // 1、Find all HandlerMappings in the ApplicationContext, including ancestor contexts.  
      Map<String, HandlerMapping> matchingBeans =  
            BeanFactoryUtils.beansOfTypeIncludingAncestors(context, HandlerMapping.class, true, false);  
      if (!matchingBeans.isEmpty()) {  
         this.handlerMappings = new ArrayList<>(matchingBeans.values());  
         // We keep HandlerMappings in sorted order.  
         AnnotationAwareOrderComparator.sort(this.handlerMappings);  
      }  
   }  
   else {  
      try {  
	      // 2、从Spring容器中获取名为`handlerMapping`的bean
         HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);  
         this.handlerMappings = Collections.singletonList(hm);  
      }  
      catch (NoSuchBeanDefinitionException ex) {  
         // Ignore, we'll add a default HandlerMapping later.  
      }  
   }  
  
   // 3、Ensure we have at least one HandlerMapping, by registering  
   // a default HandlerMapping if no other mappings are found.   
   if (this.handlerMappings == null) {  
      this.handlerMappings = getDefaultStrategies(context, HandlerMapping.class);  
      if (logger.isTraceEnabled()) {  
         logger.trace("No HandlerMappings declared for servlet '" + getServletName() +  
               "': using default strategies from DispatcherServlet.properties");  
      }  
   }  
  
   for (HandlerMapping mapping : this.handlerMappings) {  
      if (mapping.usesPathPatterns()) {  
         this.parseRequestPath = true;  
         break;  
      }  
   }  
}
```
`this.detectAllHandlerMappings`默认为`true`，所以默认会从Spring容器中获取所有类型为`HandlerMapping`的bean，作为`handerMappings`成员变量。

如果通过前两种方式都没有添加请求映射器，会从`DispatcherServlet.properties`文件中添加默认请求映射器：
```properties
org.springframework.web.servlet.HandlerMapping=org.springframework.web.servlet.handler.BeanNameUrlHandlerMapping,\  
   org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping,\  
   org.springframework.web.servlet.function.support.RouterFunctionMapping
```
添加默认请求映射器会按照如下步骤进行：
1. 读取配置文件中默认请求映射器的全限定类名
2. 实例化请求映射器，作为`bean`对象交给Spring容器管理
3. 赋值给`DispatcherServlet#handlerMappings`作为请求映射器
`org.springframework.web.servlet.DispatcherServlet#getDefaultStrategies`源码如下：
```java
protected <T> List<T> getDefaultStrategies(ApplicationContext context, Class<T> strategyInterface) {  
   if (defaultStrategies == null) {  
      try {  
         // Load default strategy implementations from properties file.  
         // This is currently strictly internal and not meant to be customized         // by application developers.         ClassPathResource resource = new ClassPathResource(DEFAULT_STRATEGIES_PATH, DispatcherServlet.class);  
         defaultStrategies = PropertiesLoaderUtils.loadProperties(resource);  
      }  
      catch (IOException ex) {  
         throw new IllegalStateException("Could not load '" + DEFAULT_STRATEGIES_PATH + "': " + ex.getMessage());  
      }  
   }  
  
   String key = strategyInterface.getName();  
   String value = defaultStrategies.getProperty(key);  
   if (value != null) {  
      String[] classNames = StringUtils.commaDelimitedListToStringArray(value);  
      List<T> strategies = new ArrayList<>(classNames.length);  
      for (String className : classNames) {  
         try {  
            Class<?> clazz = ClassUtils.forName(className, DispatcherServlet.class.getClassLoader());  
            Object strategy = createDefaultStrategy(context, clazz);  
            strategies.add((T) strategy);  
         }  
         catch (ClassNotFoundException ex) {  
            throw new BeanInitializationException(  
                  "Could not find DispatcherServlet's default strategy class [" + className +  
                  "] for interface [" + key + "]", ex);  
         }  
         catch (LinkageError err) {  
            throw new BeanInitializationException(  
                  "Unresolvable class definition for DispatcherServlet's default strategy class [" +  
                  className + "] for interface [" + key + "]", err);  
         }  
      }  
      return strategies;  
   }  
   else {  
      return Collections.emptyList();  
   }  
}
```
## 2.2 获取HandlerExecutionChain
`DispatcherServlet`会根据请求，从对应的`HandlerMapping`中获取`HandlerExecutionChain`。`org.springframework.web.servlet.DispatcherServlet#getHandler`：
```java
/**  
 * Return the HandlerExecutionChain for this request. * <p>Tries all handler mappings in order.  
 * @param request current HTTP request  
 * @return the HandlerExecutionChain, or {@code null} if no handler could be found  
 */@Nullable  
protected HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {  
   if (this.handlerMappings != null) {  
      for (HandlerMapping mapping : this.handlerMappings) {  
         HandlerExecutionChain handler = mapping.getHandler(request);  
         if (handler != null) {  
            return handler;  
         }  
      }  
   }  
   return null;  
}
```
在这个过程中，`org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandler`会完成以下几个功能：
1. 从`HandlerMapping`实现类中获取映射的handler。
2. 添加配置拦截器
3. 添加跨域拦截器
```java
/**  
 * Look up a handler for the given request, falling back to the default * handler if no specific one is found. * @param request current HTTP request  
 * @return the corresponding handler instance, or the default handler  
 * @see #getHandlerInternal  
 */@Override  
@Nullable  
public final HandlerExecutionChain getHandler(HttpServletRequest request) throws Exception {  
	// 1、从HandlerMapping实现类中获取映射的handler
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
	// 2、添加配置的拦截器
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

`org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandlerInternal`是一个抽象方法，由子类实现，并制定各自的映射规则：
```java
protected abstract Object getHandlerInternal(HttpServletRequest request) throws Exception;
```
`org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandlerExecutionChain`方法会添加配置好的拦截器：
```java
protected HandlerExecutionChain getHandlerExecutionChain(Object handler, HttpServletRequest request) {  
   HandlerExecutionChain chain = (handler instanceof HandlerExecutionChain ?  
         (HandlerExecutionChain) handler : new HandlerExecutionChain(handler));  
  
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
这里的拦截器来Spring Boot添加的默认拦截器，以及用户自定义拦截器：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void addInterceptors(InterceptorRegistry registry) {  
        // 添加自定义拦截器  
    }  
}
```
跨域拦截器会根据是否添加跨域配置进行判断，这里的跨域配置来自于`WebMvcConfigurer`/`@CrossOrigin`/`CorsConfigurationSource`。

## 2.3 执行HandlerExecutionChain
`HandlerExecutionChain`中包含`handler`和`interceptorList`。
`DispatcherServlet`会按顺序依次执行：
1. `interceptorList`的`preHandle`方法
2. 使用`handlerApdater`调用`handler`
3. `interceptorList`的`postHandle`方法
4. `interceptorList`的`afterCompletion`方法
# 3 RequestMappingHandlerMapping
`RequestMappingHandlerMapping`是针对`@Controller`和`@RequestMapping`的实现类，是日常项目中使用最多的请求地址映射器。
## 3.1 初始化：注册bean
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#requestMappingHandlerMapping`：
```java
@Bean  
@SuppressWarnings("deprecation")  
public RequestMappingHandlerMapping requestMappingHandlerMapping(  
      @Qualifier("mvcContentNegotiationManager") ContentNegotiationManager contentNegotiationManager,  
      @Qualifier("mvcConversionService") FormattingConversionService conversionService,  
      @Qualifier("mvcResourceUrlProvider") ResourceUrlProvider resourceUrlProvider) {  
  
   RequestMappingHandlerMapping mapping = createRequestMappingHandlerMapping();  
   mapping.setOrder(0);  
   mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));  
   mapping.setContentNegotiationManager(contentNegotiationManager);  
   mapping.setCorsConfigurations(getCorsConfigurations());  
  
   PathMatchConfigurer pathConfig = getPathMatchConfigurer();  
   if (pathConfig.getPatternParser() != null) {  
      mapping.setPatternParser(pathConfig.getPatternParser());  
   }  
   else {  
      mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());  
      mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());  
  
      Boolean useSuffixPatternMatch = pathConfig.isUseSuffixPatternMatch();  
      if (useSuffixPatternMatch != null) {  
         mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);  
      }  
      Boolean useRegisteredSuffixPatternMatch = pathConfig.isUseRegisteredSuffixPatternMatch();  
      if (useRegisteredSuffixPatternMatch != null) {  
         mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);  
      }  
   }  
   Boolean useTrailingSlashMatch = pathConfig.isUseTrailingSlashMatch();  
   if (useTrailingSlashMatch != null) {  
      mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);  
   }  
   if (pathConfig.getPathPrefixes() != null) {  
      mapping.setPathPrefixes(pathConfig.getPathPrefixes());  
   }  
  
   return mapping;  
}
```

SpringMVC拦截器`HandlerInterceptor`初始化：
```java
mapping.setInterceptors(getInterceptors(conversionService, resourceUrlProvider));
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#getInterceptors`：
```java
/**  
 * Provide access to the shared handler interceptors used to configure * {@link HandlerMapping} instances with.  
 * <p>This method cannot be overridden; use {@link #addInterceptors} instead.  
 */protected final Object[] getInterceptors(  
      FormattingConversionService mvcConversionService,  
      ResourceUrlProvider mvcResourceUrlProvider) {  
  
   if (this.interceptors == null) {  
      InterceptorRegistry registry = new InterceptorRegistry();  
      addInterceptors(registry);  
      registry.addInterceptor(new ConversionServiceExposingInterceptor(mvcConversionService));  
      registry.addInterceptor(new ResourceUrlProviderExposingInterceptor(mvcResourceUrlProvider));  
      this.interceptors = registry.getInterceptors();  
   }  
   return this.interceptors.toArray();  
}
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addInterceptors`作为扩展点，支持添加自定义拦截器：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void addInterceptors(InterceptorRegistry registry) {  
	    // 添加自定义拦截器
     }  
}
```

SpringMVC内容协商管理器`ContentNegotiationManager`初始化：
```java
mapping.setContentNegotiationManager(contentNegotiationManager);
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#mvcContentNegotiationManager`：
```java
/**  
 * Return a {@link ContentNegotiationManager} instance to use to determine  
 * requested {@linkplain MediaType media types} in a given request.  
 */@Bean  
public ContentNegotiationManager mvcContentNegotiationManager() {  
   if (this.contentNegotiationManager == null) {  
      ContentNegotiationConfigurer configurer = new ContentNegotiationConfigurer(this.servletContext);  
      configurer.mediaTypes(getDefaultMediaTypes());  
      configureContentNegotiation(configurer);  
      this.contentNegotiationManager = configurer.buildContentNegotiationManager();  
   }  
   return this.contentNegotiationManager;  
}
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#configureContentNegotiation`作为扩展点，支持添加自定义内容协商管理器：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void configureContentNegotiation(ContentNegotiationConfigurer configurer) {  
        // 添加自定义内容协商管理器  
    }  
}
```

SpringMVC跨域配置`CorsConfiguration`初始化：
```java
mapping.setCorsConfigurations(getCorsConfigurations());
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#getCorsConfigurations`：
```java
/**  
 * Return the registered {@link CorsConfiguration} objects,  
 * keyed by path pattern. * @since 4.2 */protected final Map<String, CorsConfiguration> getCorsConfigurations() {  
   if (this.corsConfigurations == null) {  
      CorsRegistry registry = new CorsRegistry();  
      addCorsMappings(registry);  
      this.corsConfigurations = registry.getCorsConfigurations();  
   }  
   return this.corsConfigurations;  
}
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#addCorsMappings`作为扩展点，支持添加自定义跨域配置：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
  
    @Override  
    public void addCorsMappings(CorsRegistry registry) {  
        // 添加自定义跨域配置
    }
}
```

SpringMVC地址解析器相关初始化：
```java
PathMatchConfigurer pathConfig = getPathMatchConfigurer();  
if (pathConfig.getPatternParser() != null) {  
   mapping.setPatternParser(pathConfig.getPatternParser());  
}  
else {  
   mapping.setUrlPathHelper(pathConfig.getUrlPathHelperOrDefault());  
   mapping.setPathMatcher(pathConfig.getPathMatcherOrDefault());  
  
   Boolean useSuffixPatternMatch = pathConfig.isUseSuffixPatternMatch();  
   if (useSuffixPatternMatch != null) {  
      mapping.setUseSuffixPatternMatch(useSuffixPatternMatch);  
   }  
   Boolean useRegisteredSuffixPatternMatch = pathConfig.isUseRegisteredSuffixPatternMatch();  
   if (useRegisteredSuffixPatternMatch != null) {  
      mapping.setUseRegisteredSuffixPatternMatch(useRegisteredSuffixPatternMatch);  
   }  
}  
Boolean useTrailingSlashMatch = pathConfig.isUseTrailingSlashMatch();  
if (useTrailingSlashMatch != null) {  
   mapping.setUseTrailingSlashMatch(useTrailingSlashMatch);  
}  
if (pathConfig.getPathPrefixes() != null) {  
   mapping.setPathPrefixes(pathConfig.getPathPrefixes());  
}
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#getPathMatchConfigurer`：
```java
/**  
 * Callback for building the {@link PathMatchConfigurer}.  
 * Delegates to {@link #configurePathMatch}. * @since 4.1 */protected PathMatchConfigurer getPathMatchConfigurer() {  
   if (this.pathMatchConfigurer == null) {  
      this.pathMatchConfigurer = new PathMatchConfigurer();  
      configurePathMatch(this.pathMatchConfigurer);  
   }  
   return this.pathMatchConfigurer;  
}
```
`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#configurePathMatch`作为扩展点，支持添加自定义地址解析器：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void configurePathMatch(PathMatchConfigurer configurer) {  
        // 添加自定义地址解析器  
    }  
}
```
## 3.2 初始化：请求地址映射扫描
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#afterPropertiesSet`，实现`InitializingBean`接口：
```java
/**  
 * Detects handler methods at initialization. * @see #initHandlerMethods */@Override  
public void afterPropertiesSet() {  
   initHandlerMethods();  
}
```
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#initHandlerMethods`：
```java
/**  
 * Scan beans in the ApplicationContext, detect and register handler methods. 
 * @see #getCandidateBeanNames() 
 * @see #processCandidateBean 
 * @see #handlerMethodsInitialized 
 */
 protected void initHandlerMethods() { 
	// 1、遍历所有bean 
   for (String beanName : getCandidateBeanNames()) {  
      if (!beanName.startsWith(SCOPED_TARGET_NAME_PREFIX)) {  
		// 2、请求地址映射处理
         processCandidateBean(beanName);  
      }  
   }   
   handlerMethodsInitialized(getHandlerMethods());  
}
```
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#processCandidateBean`：
```java
/**  
 * Determine the type of the specified candidate bean and call 
 * {@link #detectHandlerMethods} if identified as a handler type.  
 * <p>This implementation avoids bean creation through checking  
 * {@link org.springframework.beans.factory.BeanFactory#getType}  
 * and calling {@link #detectHandlerMethods} with the bean name.  
 * @param beanName the name of the candidate bean  
 * @since 5.1 
 * @see #isHandler 
 * @see #detectHandlerMethods 
 */
 protected void processCandidateBean(String beanName) {  
   Class<?> beanType = null;  
   try {  
		// 1、获取bean的类对象
      beanType = obtainApplicationContext().getType(beanName);  
   }  
   catch (Throwable ex) {  
      // An unresolvable bean type, probably from a lazy bean - let's ignore it.  
      if (logger.isTraceEnabled()) {  
         logger.trace("Could not resolve type for bean '" + beanName + "'", ex);  
      }  
   }   
   // 2、判断当前HandlerMapping实现类能否处理当前bean
   if (beanType != null && isHandler(beanType)) { 
	   // 3、解析&映射请求地址 
      detectHandlerMethods(beanName);  
   }  
}
```
`isHandler()`由子类实现，用来判断该类对象是否需要进行请求地址解析。例如，`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#isHandler`：
```java
/**  
 * {@inheritDoc}  
 * <p>Expects a handler to have either a type-level @{@link Controller}  
 * annotation or a type-level @{@link RequestMapping} annotation.  
 */@Override  
protected boolean isHandler(Class<?> beanType) {  
   return (AnnotatedElementUtils.hasAnnotation(beanType, Controller.class) ||  
         AnnotatedElementUtils.hasAnnotation(beanType, RequestMapping.class));  
}
```
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#detectHandlerMethods`：
```java
/**  
 * Look for handler methods in the specified handler bean. * @param handler either a bean name or an actual handler instance  
 * @see #getMappingForMethod */protected void detectHandlerMethods(Object handler) {  
   Class<?> handlerType = (handler instanceof String ?  
         obtainApplicationContext().getType((String) handler) : handler.getClass());  
  
   if (handlerType != null) {  
      Class<?> userType = ClassUtils.getUserClass(handlerType); 
      // 1、遍历Handler类的方法
      Map<Method, T> methods = MethodIntrospector.selectMethods(userType,  
            (MethodIntrospector.MetadataLookup<T>) method -> {  
               try {  
	               // 2、构造请求地址映射
                  return getMappingForMethod(method, userType);  
               }  
               catch (Throwable ex) {  
                  throw new IllegalStateException("Invalid mapping on handler class [" +  
                        userType.getName() + "]: " + method, ex);  
               }  
            });  
      if (logger.isTraceEnabled()) {  
         logger.trace(formatMappings(userType, methods));  
      }  
      else if (mappingsLogger.isDebugEnabled()) {  
         mappingsLogger.debug(formatMappings(userType, methods));  
      }  
      methods.forEach((method, mapping) -> {  
	      // 3、获取实际能够执行的方法
         Method invocableMethod = AopUtils.selectInvocableMethod(method, userType); 
         // 4、保存请求地址映射信息 
         registerHandlerMethod(handler, invocableMethod, mapping);  
      });  
   }  
}
```
`org.springframework.core.MethodIntrospector#selectMethods(java.lang.Class<?>, org.springframework.core.MethodIntrospector.MetadataLookup<T>)`：
```java
/**  
 * Select methods on the given target type based on the lookup of associated metadata. * <p>Callers define methods of interest through the {@link MetadataLookup} parameter,  
 * allowing to collect the associated metadata into the result map. * @param targetType the target type to search methods on  
 * @param metadataLookup a {@link MetadataLookup} callback to inspect methods of interest,  
 * returning non-null metadata to be associated with a given method if there is a match, * or {@code null} for no match  
 * @return the selected methods associated with their metadata (in the order of retrieval), * or an empty map in case of no match */public static <T> Map<Method, T> selectMethods(Class<?> targetType, final MetadataLookup<T> metadataLookup) {  
   final Map<Method, T> methodMap = new LinkedHashMap<>();  
   Set<Class<?>> handlerTypes = new LinkedHashSet<>();  
   Class<?> specificHandlerType = null;  
  
   if (!Proxy.isProxyClass(targetType)) {  
      specificHandlerType = ClassUtils.getUserClass(targetType);  
      handlerTypes.add(specificHandlerType);  
   }  
   handlerTypes.addAll(ClassUtils.getAllInterfacesForClassAsSet(targetType));  
  
   for (Class<?> currentHandlerType : handlerTypes) {  
      final Class<?> targetClass = (specificHandlerType != null ? specificHandlerType : currentHandlerType);  
  
      ReflectionUtils.doWithMethods(currentHandlerType, method -> {  
         Method specificMethod = ClassUtils.getMostSpecificMethod(method, targetClass);  
         T result = metadataLookup.inspect(specificMethod);  
         if (result != null) {  
            Method bridgedMethod = BridgeMethodResolver.findBridgedMethod(specificMethod);  
            if (bridgedMethod == specificMethod || metadataLookup.inspect(bridgedMethod) == null) {  
               methodMap.put(specificMethod, result);  
            }  
         }      }, ReflectionUtils.USER_DECLARED_METHODS);  
   }  
  
   return methodMap;  
}
```
`getMappingForMethod()`由子类实现，用来指定不同的解析规则。例如，`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#getMappingForMethod`：
```java
/**  
 * Uses method and type-level @{@link RequestMapping} annotations to create  
 * the RequestMappingInfo. * @return the created RequestMappingInfo, or {@code null} if the method  
 * does not have a {@code @RequestMapping} annotation.  
 * @see #getCustomMethodCondition(Method)  
 * @see #getCustomTypeCondition(Class)  
 */@Override  
@Nullable  
protected RequestMappingInfo getMappingForMethod(Method method, Class<?> handlerType) {  
	// 1、获取方法级别的地址信息
   RequestMappingInfo info = createRequestMappingInfo(method);  
   if (info != null) {  
	   // 2、获取类级别的地址信息
      RequestMappingInfo typeInfo = createRequestMappingInfo(handlerType);  
      if (typeInfo != null) {  
	      // 3、合并完整地址信息
         info = typeInfo.combine(info);  
      }  
      String prefix = getPathPrefix(handlerType);  
      if (prefix != null) {  
         info = RequestMappingInfo.paths(prefix).options(this.config).build().combine(info);  
      }  
   }   return info;  
}
```
`org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping#createRequestMappingInfo(org.springframework.web.bind.annotation.RequestMapping, org.springframework.web.servlet.mvc.condition.RequestCondition<?>)`：
```java
/**  
 * Create a {@link RequestMappingInfo} from the supplied  
 * {@link RequestMapping @RequestMapping} annotation, which is either  
 * a directly declared annotation, a meta-annotation, or the synthesized * result of merging annotation attributes within an annotation hierarchy. */protected RequestMappingInfo createRequestMappingInfo(  
      RequestMapping requestMapping, @Nullable RequestCondition<?> customCondition) {  
  // 从注解中获取信息，封装成对象
   RequestMappingInfo.Builder builder = RequestMappingInfo  
         .paths(resolveEmbeddedValuesInPatterns(requestMapping.path()))  
         .methods(requestMapping.method())  
         .params(requestMapping.params())  
         .headers(requestMapping.headers())  
         .consumes(requestMapping.consumes())  
         .produces(requestMapping.produces())  
         .mappingName(requestMapping.name());  
   if (customCondition != null) {  
      builder.customCondition(customCondition);  
   }  
   return builder.options(this.config).build();  
}
```
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#registerHandlerMethod`：
```java
/**  
 * Register a handler method and its unique mapping. Invoked at startup for * each detected handler method. * @param handler the bean name of the handler or the handler instance  
 * @param method the method to register  
 * @param mapping the mapping conditions associated with the handler method  
 * @throws IllegalStateException if another method was already registered  
 * under the same mapping */protected void registerHandlerMethod(Object handler, Method method, T mapping) {  
   this.mappingRegistry.register(mapping, handler, method);  
}
```
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.MappingRegistry#register`：
```java
public void register(T mapping, Object handler, Method method) {  
   this.readWriteLock.writeLock().lock();  
   try {  
      HandlerMethod handlerMethod = createHandlerMethod(handler, method);  
      validateMethodMapping(handlerMethod, mapping);  
  
      Set<String> directPaths = AbstractHandlerMethodMapping.this.getDirectPaths(mapping);  
      for (String path : directPaths) {  
         this.pathLookup.add(path, mapping);  
      }  
  
      String name = null;  
      if (getNamingStrategy() != null) {  
         name = getNamingStrategy().getName(handlerMethod, mapping);  
         addMappingName(name, handlerMethod);  
      }  
		// @CrossOrigin配置信息初始化
      CorsConfiguration corsConfig = initCorsConfiguration(handler, method, mapping);  
      if (corsConfig != null) {  
         corsConfig.validateAllowCredentials();  
         this.corsLookup.put(handlerMethod, corsConfig);  
      }  
  
      this.registry.put(mapping,  
            new MappingRegistration<>(mapping, handlerMethod, directPaths, name, corsConfig != null));  
   }  
   finally {  
      this.readWriteLock.writeLock().unlock();  
   }  
}
```
## 3.3 处理请求：请求地址映射
`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping#getHandlerInternal`：
```java
/**  
 * Look up a handler method for the given request. 
 */
@Override  
@Nullable  
protected HandlerMethod getHandlerInternal(HttpServletRequest request) throws Exception {  
	// 1、解析请求地址
   String lookupPath = initLookupPath(request);  
   this.mappingRegistry.acquireReadLock();  
   try {  
	   // 2、根据请求地址找映射方法
      HandlerMethod handlerMethod = lookupHandlerMethod(lookupPath, request);  
      return (handlerMethod != null ? handlerMethod.createWithResolvedBean() : null);  
   }  
   finally {  
      this.mappingRegistry.releaseReadLock();  
   }  
}
```