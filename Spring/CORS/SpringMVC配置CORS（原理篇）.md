全局配置org.springframework.web.servlet.handler.AbstractHandlerMapping持有：
- private CorsConfigurationSource corsConfigurationSource;  
- private CorsProcessor corsProcessor = new DefaultCorsProcessor();

局部配置org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.MappingRegistry：
- private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

过滤器配置org.springframework.web.filter.CorsFilter：
- private final CorsConfigurationSource configSource;  
- private CorsProcessor processor = new DefaultCorsProcessor();

只有配置了全局配置或局部配置，才会添加executionChain对请求进行拦截，`org.springframework.web.servlet.handler.AbstractHandlerMapping#getHandler`：
```
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
```
如果全局配置和局部配置都没有添加，就不会进行CORS处理。也就是说，虽然是跨域请求，服务端也会进入到`Controller`层执行相关代码，只是返回给浏览器不会添加CORS响应头字段。