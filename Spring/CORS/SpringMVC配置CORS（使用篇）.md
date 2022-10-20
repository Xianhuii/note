# 1 `CorsFilter`
通过配置`CorsFilter`，可以在过滤器级别对跨域请求进行处理。
```java
@Configuration  
public class CorsFilterConfig {  
    @Bean  
    public CorsFilter corsFilter() {  
        // 1、创建CorsConfigurationSource配置源，使用spring-web内置实现  
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();  
        // 2、创建CorsConfiguration配置，并注册到配置源（可创建并注册多个配置）  
        CorsConfiguration config = new CorsConfiguration();  
        config.setAllowCredentials(true);  
        config.setAllowedOriginPatterns(Collections.singletonList("*"));  
        config.addAllowedHeader("*");  
        config.addAllowedMethod("*");  
        source.registerCorsConfiguration("/**", config);  
        // 3、创建CorsFilter过滤器，设置配置源  
        return new CorsFilter(source);  
    }  
}
```
# 2 