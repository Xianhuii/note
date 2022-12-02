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

# 3 RequestMappingHandlerMapping
