# 请求地址映射
`org.springframework.web.servlet.DispatcherServlet#getHandler`：
```java
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

# 初始化
`org.springframework.web.servlet.DispatcherServlet#initHandlerMappings`：
```java
private void initHandlerMappings(ApplicationContext context) {  
   this.handlerMappings = null;  
  
   if (this.detectAllHandlerMappings) {  
      // Find all HandlerMappings in the ApplicationContext, including ancestor contexts.  
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
         HandlerMapping hm = context.getBean(HANDLER_MAPPING_BEAN_NAME, HandlerMapping.class);  
         this.handlerMappings = Collections.singletonList(hm);  
      }  
      catch (NoSuchBeanDefinitionException ex) {  
         // Ignore, we'll add a default HandlerMapping later.  
      }  
   }  
  
   // Ensure we have at least one HandlerMapping, by registering  
   // a default HandlerMapping if no other mappings are found.   if (this.handlerMappings == null) {  
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
`DispatcherServlet`的`detectAllHandlerMappings`成员变量表示是否加载所有`HandlerMapping`的bean：
- `true`（默认值）：添加所有类型为`HandlerMapping`的bean作为请求映射器
- `false`：只添加名为`handlerMapping`的bean作为请求映射器

如果通过上述两种方式都没有添加请求映射器，会从`DispatcherServlet.properties`文件中添加默认请求映射器：
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

# RequestMappingHandlerMapping
## 1 注册bean
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

## 2 初始化：请求地址映射
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
   }   handlerMethodsInitialized(getHandlerMethods());  
}
```
## 3 搜索：请求地址映射
