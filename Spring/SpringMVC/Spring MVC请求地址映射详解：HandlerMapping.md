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
## 2.1 加载HandlerMapping
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
## 2.2 请求地址映射


# 3 RequestMappingHandlerMapping
