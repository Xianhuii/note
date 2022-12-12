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
对于某些必须的功能模块（`LocaleResolver`、`ThemeResolver`、`HandlerMapping`、`HandlerAdapter`、`HandlerExceptionResolver`、`View`）