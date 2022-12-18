从2017年初开始自学Java，到现在工作一年半，恍然间已经在Java世界里畅游了大概5年。
作为一名Java后端程序员，如今日常工作就是写写接口，用来接收前端的请求，然后返回处理结果。
回想当初刚开始学习时，还需要自己定义`Servlet`实现类。实现`service()`方法，手动从`request`中解析出请求参数，经过业务处理后，再通过`response`将结果返回给前端。
![[Servlet.png]]
学习和使用后Spring MVC后，直接在形参前添加`@RequestParam`或`@RequestBody`注解，就可以直接将前端请求参数转换成我们需要的对象。直接在响应参数前添加`@ResponseBody`注解，就可以直接将返回值转换成JSON格式发送给前端。
后来，阅读了Java Servlet规范、Spring MVC源码和Tomcat源码，才发现Java领域Web服务器的本质其实就是`Servlet`。
在Java领域的Web服务器，比如Tomcat、Jetty或Undertow，它们都是根据Java Servlet规范实现的。
就好比我们会根据产品经理的需求进行业务实现。这些Web服务器也会根据Java Servlet规范进行开发。
Web服务器都会按照Java Servlet规范的流程运行：
1. 监听端口
2. 接受到请求后创建`Socket`连接
3. 从`Socket`连接中获取`InputStream`和`OutputStream`对象
4. 从`InputStream`中解析HTTP数据，封装成`request`
5. 将`OutputStream`封装成`response`
6. 将`request`和`resposne`作为参数传递给`Servlet`
在Java领域，所有Web服务都按照这个基本流程运行。理解这一点，我们日常开发会更加得心应手，对前后端交互也会更有底气。