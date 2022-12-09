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
