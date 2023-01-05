> 今天梳理一下`DispatcherServlet`的组成结构，了解其各个核心功能。

`DispatcherServlet`只是一个普通的`Servlet`，它也会接收Java Web服务器的`request`和`response`参数，从`request`中获取请求信息，向`response`返回响应信息。
`DispatcherServlet`也是一个特殊的`Servlet`，为了适用日常Web应用开发，它需要兼容各种情况的业务流程：
1. 如果是个文件上传请求，它需要对文件输入流进行处理，并及时清除临时文件。
2. 它需要映射请求地址和对应的处理方法。
3. 它需要将HTTP请求参数解析成对应Java对象。
4. 它需要校验请求参数的合法性。
5. 它需要将响应Java对象解析成对应格式的HTTP响应。
6. 它需要对异常进行统一处理。
7. ……

`DispatcherServlet`的类图如下：
![[DispatcherServlet.png]]
其中，每个成员变量集成了某种业务的功能，这些功能组合起来，能够支持绝大部分日常业务场景。
它还提供了多个扩展点，便于开发人员在业务流程中的某个节点进行扩展，增强Spring MVC功能以适应自己的系统。
# `MultipartResolver`
![[MultipartResolver.png]]
`MultipartResolver`提供了文件上传业务的功能：
1. 首先，它会对`request`参数进行校验，如果请求头`Content-Type`以`multipart/`开头，说明当前是文件上传请求。
2. 如果是文件上传请求，`DispatcherServlet`会调用`MultipartResolver`成员变量，对文件请求进行处理。核心处理逻辑是，读取文件输入流，将文件临时保存到本地，然后将本地临时文件的信息封装成`MultipartHttpServletRequest`对象返回。开发人员在业务中读取到的实际上是本地临时文件，而不是网络中的文件二进制数据。
3. 在业务处理完成后，`DispatcherServlet`还会调用`MultipartResolver`成员变量，及时将本地临时文件删除，避免资源浪费。
# `HandlerMapping`
![[HandlerMapping.png]]
`HandlerMapping`提供了请求地址和对应处理方法的映射功能：
1. 在项目启动时，`HandlerMapping`会对整个项目进行扫描，将开发人员定义的请求地址和处理方法一一对应，并且缓存起来。
2. 当`DispatcherServlet`接收到请求时，会根据当前`request`的请求地址等信息，从`HandlerMapping`中获取对应的处理方法，便于后续调用。
`DispatcherServlet`持有`HandlerMapping`的列表，支持各种形式的请求地址-处理方法映射方式。例如`@Controller`就是最常使用的一种方式，对应的是`RequestMappingHandlermapping`。
`HandlerMapping`除了保存请求地址和处理方法映射关系，它还会存储拦截器`HandlerInterceptor`列表。
拦截器类似于`Filter`，会在处理方法前进行预处理，在处理方法后进行后处理，会在请求完成后进行最后处理，是Spring MVC提供的一个增强点。
例如，跨域资源共享（CORS）功能，就可以通过拦截器进行实现。
# `HandlerAdapter`
![[HandlerAdapter.png]]
`HandlerAdapter`是`DispatcherServlet`最核心的一个成员变量，它会实际调用开发人员定义的请求处理方法：
1. 在项目启动时，`HandlerAdapter`会加载配置的参数解析器、返回值处理器、类型转换器等。
2. `HandlerMapping`和`HandlerAdapter`是一一对应的。当`DispatcherServlet`处理请求时，如果该请求地址-处理方法由某个`HandlerMapping`保存，会使用对应的`HandlerAdapter`进行处理。
	1. `HandlerAdapter`会对`request`进行预处理，比如将请求参数解析成对应处理方法的形参对象，对请求参数进行规则校验。
	2. `HandlerAdapter`会使用反射方式，传递解析后的参数进行调用处理方法。
	3. `HandlerAdapter`会按照定义好的HTTP响应格式，将处理方法返回的Java对象解析成对应的数据格式进行响应。
`DispatcherServlet`持有`HandlerAdapter`的列表，与`HandlerMapping`列表一一对应。例如`@Controller`使用的`RequestMappingHandlermapping`，对应的是`RequestMappingHandlerAdapter`。
`HandlerAdapter`支持自定义参数解析器、返回值处理器、类型转换器等。例如`@RequestBody`、`@RequestParam`、`@Validated`和`@ResponseBody`等注解，都是通过这些解析器/处理器/转换器进行实现的。
# `HandlerExceptionResolver`
![[HandlerExceptionResolver.png]]
`HandlerExceptionResolver`提供了全局异常处理的功能：
1. 在项目启动时，`HandlerExceptionResolver`会扫描整个项目，加载异常处理的处理方法。
2. 在`DispatcherServlet`处理请求过程中，包括`MultipartResolver`文件处理、`HandlerMapping`获取请求处理方法、拦截器的方法执行、`HandlerAdapter`执行请求处理方法过程，只要抛出了异常，都会最终交由`HandlerExceptionResolver`进行全局异常处理。
3. 如果`HandlerExceptionResolver`不能处理该异常，或者在处理异常过程中抛出了新异常，都会直接将异常返回给Java Web服务器。
`DispatcherServlet`持有`HandlerExceptionResolver`的列表，支持各种形式的全局异常处理方式。例如`@ControllerAdvice`就是最常使用的一种方式，对应的是`ExceptionHandlerExceptionResolver`。
# 其他
以上介绍的是前后端分离模式下，所使用的核心功能。
`DispatcherServlet`还支持其他业务流程的支持，对应模块为：
- `LocaleResolver`：国际化解析器。
- `ThemeResolver`：主题解析器。
- `RequestToViewNameTranslator`：请求-视图翻译器。
- `FlashMapManager`：重定向管理器。
- `ViewResolver`：视图解析器。
这些模块只在特殊场景才会使用，这里不做过多介绍。以后有机会可以对这些模块进行详细说明。

# 今日小结
本文对`DispatcherServlet`的核心功能进行了简单的梳理，但是详细总结了各个核心功能的处理流程，对`DispatcherServlet`有了整体的概念，对后续深入学习有着重要的指导作用。

