之前的文章介绍了`DispatcherServlet`的各种核心功能，以及处理请求的流程。
今天要介绍的是，`DispatcherServlet`是怎么初始化各核心功能的？
换句话说，`DispatcherServlet`作为一个`Servlet`，它是在什么时候将`MultipartResolver`、`HandlerMapping`、`HandlerAdapter`以及`HandlerExceptionResolver`等添加为自己的成员变量的？
# initStrategies()方法
首先，在`DispatcherServlet`中已经定义好了一个初始化方法：`initStrategies()`。
在该方法中会对各个功能模块进行初始化：
```java
protected void initStrategies(ApplicationContext context) {  
   initMultipartResolver(context);  
   initLocaleResolver(context);  
   initThemeResolver(context);  
   initHandlerMappings(context);  
   initHandlerAdapters(context);  
   initHandlerExceptionResolvers(context);  
   initRequestToViewNameTranslator(context);  
   initViewResolvers(context);  
   initFlashMapManager(context);  
}
```
`initStrategies()`方法会将Spring上下文传递给各个子方法，方便各个子方法从容器中获取各自需要的`bean`对象。
在`initXxx()`方法中会对各自功能模块进行初始化。简单来说，就是从容器中获取对应的`bean`对象，设置为`DispatcherServlet`对应的成员变量。

这些`initXxx()`方法的初始化流程大同小异。

对于列表类型的成员变量（`HandlerMapping`、`HandlerAdapter`、`HandlerExceptionResolver`和`ViewResolver`），通过`dectectAllXxx`成员变量可以设置从容器中获取的是单个还是多个`bean`对象进行初始化：
- `true`：根据类型（如`HandlerMapping`）从容器中获取多个`bean`对象，进行初始化。
- `false`：根据名称（如`handlerMapping`）从容器中获取单个`bean`对象，进行初始化。

对于对象类型的成员变量（`MultipartResolver`、`LocaleResolver`、`ThemeResolver`、`RequestToViewNameTranslator`和`FlashMapManager`），它们只能根据名称（如`multipartResolver`）从容器中获取单个`bean`对象，进行初始化。

对于某些必须的功能模块（`LocaleResolver`、`ThemeResolver`、`HandlerMapping`、`HandlerAdapter`、`HandlerExceptionResolver`、`RequestToViewNameTranslator`、`ViewResolver`和`FlashMapManager`），如果从容器中获取不到对应的`bean`对象，会从`DispatcherServlet.properties`文件中加载默认的功能对象。

例如，`initHandlerAdapters`源码如下：
```java
private void initHandlerAdapters(ApplicationContext context) {  
   this.handlerAdapters = null;  
  
   if (this.detectAllHandlerAdapters) {  
      // 1、从容器中加载所有类型为HandlerAdapter
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
      // 2、从容器中加载名为handlerAdapter的HandlerAdapter
         HandlerAdapter ha = context.getBean(HANDLER_ADAPTER_BEAN_NAME, HandlerAdapter.class);  
         this.handlerAdapters = Collections.singletonList(ha);  
      }  
      catch (NoSuchBeanDefinitionException ex) {  
      }  
   }  
  
   // 3、从DispatcherServlet.propeties中获取默认的HandlerAdapter
      this.handlerAdapters = getDefaultStrategies(context, HandlerAdapter.class);  
      if (logger.isTraceEnabled()) {  
         logger.trace("No HandlerAdapters declared for servlet '" + getServletName() +  
               "': using default strategies from DispatcherServlet.properties");  
      }  
   }  
}
```

`DispatcherServlet.properties`中配置了默认的功能模块实现类，作为兜底。格式为`功能模块全限定类名=功能模块实现类`。例如：
```properties
org.springframework.web.servlet.HandlerAdapter=org.springframework.web.servlet.mvc.HttpRequestHandlerAdapter,\  
   org.springframework.web.servlet.mvc.SimpleControllerHandlerAdapter,\  
   org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter,\  
   org.springframework.web.servlet.function.support.HandlerFunctionAdapter
```
# onRefresh()方法
`DispatcherServlet`中实现类`FrameworkServlet`的`onRefresh()`方法，会对所有功能模块进行初始化：
```java
protected void onRefresh(ApplicationContext context) {  
   initStrategies(context);  
}
```
`onRefresh()`方法会在容器刷新时调用。
在Spring Boot项目中，如果在`onRefresh()`中添加断点，会发现在项目启动时不会进入该断点，说明项目启动时并不会初始化`DispatcherServlet`的各个功能模块。
在第一次请求时，才会触发该方法。