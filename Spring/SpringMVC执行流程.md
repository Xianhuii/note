SpringMVC框架是基于`Servlet`规范搭建起来的，它本质上只是实现了`Servlet`接口。

1. 文件请求处理：`DispatcherServlet#checkMultipart`
2. 获取`HandlerExecutionChain`：`DispatcherServlet#getHandler`
3. 获取`HandlerAdapter`：`DispatcherServlet#getHandlerAdapter`
4. `HandlerExecutionChain`预处理：`HandlerExecutionChain#applyPreHandle`
5. `HandlerAdapter`处理：`HandlerAdapter#handle`
6. `HandlerExecutionChain`后处理：`HandlerExecutionChain#applyPostHandle`
7. 结果处理：`DispatcherServlet#processDispatchResult`

## 1 文件请求处理：`DispatcherServlet#checkMultipart`
文件请求处理不一定是在`DispatcherServlet#checkMultipart`中处理，可能在过滤器中就处理好了。
文件请求处理方法：`MultipartResolver#resolveMultipart`
实际文件处理其实已经在web服务器如tomcat中处理好了，这里只是将`HttpServletRequest`包装成`MulpartHttpServletRequest`，方便后续业务处理。
默认会先将文件临时保存到本地（`org.apache.catalina.connector.Request#parseParts`和`org.apache.tomcat.util.http.fileupload.FileUploadBase#parseRequest），业务中读取的文件其实是服务器临时地址中的文件。
`spring.servlet.multipart.resolve-lazily=false`：不延迟处理文件，默认在文件请求处理时就将文件临时保存到本地。
`spring.servlet.multipart.resolve-lazily=true`：延迟处理文件，在文件请求处理时不会将文件临时保存到本地。只有调用`MultipartHttpServletRequest`获取文件相关方法时，才会保存到本地。不调用相关方法则不会保存。
在`DispatcherServlet#doDispatch`方法的最后，SpringMVC会删除这些临时文件。

## 2 获取`HandlerExecutionChain`：`DispatcherServlet#getHandler`
`HandlerExecutionChain`是请求处理执行链，它包含实际处理器`handler`（Controller中的方法），以及一系列拦截器`interceptor`。
`hander`实际存储在`HandlerMapping`实例中。`DispatcherServlet`默认使用`RequestMappingHandlerMapping`、`BeanNameUrlHandlerMapping`和`RouterFunctionMapping`，它们内部存储的handler都不一样，并且有对应不同的`HandlerAdapter`进行实际处理。
我们最常使用的是`RequestMappingHandlerMapping`，它会缓存接口地址→`HandlerMethod`的映射。
`BeanNameUrlHandlerMapping`需要控制层实现`org.springframework.web.servlet.mvc.Controller`接口，内部缓存`beanName`→`bean`（`org.springframework.web.servlet.mvc.Controller`实现类）的映射。
而`RouterFunctionMapping`是另一种风格的方式，它会缓存`RouterFunction`→`bean`（`org.springframework.web.servlet.function.HandlerFunction`实现类）的映射。

## 3 获取`HandlerAdapter`：`DispatcherServlet#getHandlerAdapter`
`HandlerAdapter`的作用是对上述`handler`进行增强处理，包括参数类型转换、返回值处理等。
`HandlerMapping`和`HandlerAdapter`之间是一一对应的关系，不同`HandlerMapping`会返回不同类型的handler，不同类型的handler也只能由对应的`HandlerAdapter`进行处理。例如`RequestMappingHandlerMapping`返回`HandlerMethod`类型的handler，只能由`RequestMappingHandlerAdapter`进行处理。

## 4 `HandlerExecutionChain`预处理：`HandlerExecutionChain#applyPreHandle`
