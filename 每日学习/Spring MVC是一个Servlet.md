昨天学习了Java Web服务器是如何处理请求的，可以知道服务器会将请求交给`Servlet`处理。
简单来说，Java Web服务器是一个接收HTTP请求的应用软件。
就好比在手机代办事项里创建一个个代办事项，我们也可以在Java Web服务器里面创建一个个`Servlet`，用来处理对应的请求。
在使用Spring MVC后，我们再也不用创建`Servlet`。只需要使用`@Controller`和`@RequestMapping`注解，就可以定义一个`接口`，用来接收前端请求。
例如，我们定义如下接口：
```java
@RestController
public class TestController {
	@RequestMapping("/test")
	public String test() {
		return "test()接口响应";
	}
}
```
启动项目后，请求`/test`地址，可以得到响应：
```
test()接口响应
```
为什么Spring MVC不需要`Servlet`了？
实际上，Spring MVC本身就是一个`Servlet`。

![[每日学习/attachments/DispatcherServlet.png]]