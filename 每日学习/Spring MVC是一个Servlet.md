昨天学习了Java Web服务器是如何处理请求的，可以知道服务器会将请求交给`Servlet`处理。
简单来说，Java Web服务器是一个接收HTTP请求的应用软件。
就好比在手机代办事项里创建一个个代办事项，我们也可以在Java Web服务器里面创建一个个`Servlet`，用来处理对应的请求。
![[Untitled Diagram.png]]
在使用Spring MVC后，只需要使用`@Controller`和`@RequestMapping`注解，就可以定义一个`接口`，用来接收前端请求。
例如，我们定义如下接口：
```java
@RestController
public class TestController {
	@RequestMapping("/test")
	public String test() {
		return "Hello Spring MVC";
	}
}
```
启动项目后，请求`/test`地址，可以得到响应：
```
Hello Spring MVC
```

自从使用Spring MVC之后，我们再也不用创建`Servlet`了。
似乎Spring MVC超脱了Java Servlet规范。其实不然，Spring MVC本身就是一个`Servlet`，叫做`DispatcherServlet`。
![[每日学习/attachments/DispatcherServlet.png]]
`DispatcherServlet`实现了`Servlet`接口，它也可以接收从Java Web服务器传递过来的`request`和`response`。
为了便于日常开发使用，`DispatcherServlet`会通过反射的方式，直接将`request`中的请求参数解析成对应的Java对象，也可以直接将业务处理返回的Java对象解析成对应格式的数据进行响应。
当然，`DispatcherServlet`的能力不仅仅是转换数据格式，它的成员变量（通常称为核心组件）可以满足各种常见的业务场景：
1. 文件上传
2. 请求地址映射
3. 参数解析
4. 参数校验
5. 异常统一处理
6. ……

相对于原始的`Servlet`开发，一个请求对应着一个`Servlet`实现类。
使用Spring MVC后，一个Web项目通常只需要一个`Dispatcher`映射所有请求即可，它会管理整个项目的所有接口。
![[Untitled Diagram (1).png]]