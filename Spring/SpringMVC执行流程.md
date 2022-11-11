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
文件请求处理方法：`MultipartResolver#resolveMultipart`，