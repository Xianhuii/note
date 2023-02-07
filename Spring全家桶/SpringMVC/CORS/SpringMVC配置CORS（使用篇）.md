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
# 2 `WebMvcConfigurer`
使用`WebMvcConfigurer`中的`addCorsMappings()`方法，在启动项目时该配置会被自动添加到`org.springframework.web.servlet.handler.AbstractHandlerMapping#corsConfigurationSource`。
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void addCorsMappings(CorsRegistry registry) {  
        registry.addMapping("/api1/**")  
                .allowedOrigins("https://domain1.com")  
                .allowedMethods("PUT", "DELETE")  
                .allowedHeaders("header1", "header2", "header3")  
                .exposedHeaders("header1", "header2").  
                allowCredentials(true).maxAge(3600);  
        registry.addMapping("/api2/**")  
                .allowedOrigins("https://domain2.com")  
                .allowedMethods("PUT", "DELETE")  
                .allowedHeaders("header1", "header2", "header3")  
                .exposedHeaders("header1", "header2").  
                allowCredentials(true).maxAge(3600);  
    }  
}
```
# 3 `@CrossOrigin`
`@CrossOrigin`可以加到`Controller`类上，为内部所有接口添加统一的配置：
```java
@CrossOrigin  
@RestController  
public class CorsController {
}
```
`@CrossOrigin`也可以加到`Controller`类内部具体某个方法上，指定为这个接口添加配置：
```java
@RestController  
public class CorsController {  
    @CrossOrigin  
    @RequestMapping("CrossOrigin")  
    public String CrossOrigin() {
        return "Hello Cors!";  
    }  
}
```
`@CrossOrigin`配置会在项目启动时，被添加到`org.springframework.web.servlet.handler.AbstractHandlerMethodMapping.MappingRegistry#corsLookup`。