# 1 初始化
`org.springframework.web.servlet.DispatcherServlet#initHandlerExceptionResolvers`：
```java
/**  
 * Initialize the HandlerExceptionResolver used by this class. * <p>If no bean is defined with the given name in the BeanFactory for this namespace,  
 * we default to no exception resolver. */private void initHandlerExceptionResolvers(ApplicationContext context) {  
   this.handlerExceptionResolvers = null;  
  
   if (this.detectAllHandlerExceptionResolvers) {  
      // Find all HandlerExceptionResolvers in the ApplicationContext, including ancestor contexts.  
      Map<String, HandlerExceptionResolver> matchingBeans = BeanFactoryUtils  
            .beansOfTypeIncludingAncestors(context, HandlerExceptionResolver.class, true, false);  
      if (!matchingBeans.isEmpty()) {  
         this.handlerExceptionResolvers = new ArrayList<>(matchingBeans.values());  
         // We keep HandlerExceptionResolvers in sorted order.  
         AnnotationAwareOrderComparator.sort(this.handlerExceptionResolvers);  
      }  
   }  
   else {  
      try {  
         HandlerExceptionResolver her =  
               context.getBean(HANDLER_EXCEPTION_RESOLVER_BEAN_NAME, HandlerExceptionResolver.class);  
         this.handlerExceptionResolvers = Collections.singletonList(her);  
      }  
      catch (NoSuchBeanDefinitionException ex) {  
         // Ignore, no HandlerExceptionResolver is fine too.  
      }  
   }  
  
   // Ensure we have at least some HandlerExceptionResolvers, by registering  
   // default HandlerExceptionResolvers if no other resolvers are found.   if (this.handlerExceptionResolvers == null) {  
      this.handlerExceptionResolvers = getDefaultStrategies(context, HandlerExceptionResolver.class);  
      if (logger.isTraceEnabled()) {  
         logger.trace("No HandlerExceptionResolvers declared in servlet '" + getServletName() +  
               "': using default strategies from DispatcherServlet.properties");  
      }  
   }  
}
```

`org.springframework.web.servlet.config.annotation.WebMvcConfigurationSupport#handlerExceptionResolver`：
```java
/**  
 * Returns a {@link HandlerExceptionResolverComposite} containing a list of exception  
 * resolvers obtained either through {@link #configureHandlerExceptionResolvers} or * through {@link #addDefaultHandlerExceptionResolvers}. * <p><strong>Note:</strong> This method cannot be made final due to CGLIB constraints. * Rather than overriding it, consider overriding {@link #configureHandlerExceptionResolvers} * which allows for providing a list of resolvers. */@Bean  
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