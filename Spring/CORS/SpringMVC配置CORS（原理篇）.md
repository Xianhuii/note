全局配置org.springframework.web.servlet.handler.AbstractHandlerMapping持有：
- private CorsConfigurationSource corsConfigurationSource;  
- private CorsProcessor corsProcessor = new DefaultCorsProcessor();

局部配置org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.MappingRegistry：
- private final Map<HandlerMethod, CorsConfiguration> corsLookup = new ConcurrentHashMap<>();

过滤器配置org.springframework.web.filter.CorsFilter：
- private final CorsConfigurationSource configSource;  
- private CorsProcessor processor = new DefaultCorsProcessor();