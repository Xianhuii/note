`ExceptionHandlerExceptionResolver`是Spring Web MVC中使用最简单、最常用的异常处理器之一，可以进行全局异常统一处理。

在项目初始化时，`ExceptionHandlerExceptionResolver`对`@ControllerAdvice`和`@ExceptionHandler`标注的异常处理方法进行缓存，构筑异常-处理方法的映射。

在处理请求时，如果业务中抛出异常到`DispatcherServlet`，`ExceptionHandlerExceptionResolver`会根据异常对象找到对应处理方法，进行统一异常处理。

# 1 基本使用
例如，在业务中会抛出`java.lang.ArithmeticException`异常：
```java
@RestController  
@RequestMapping("ControllerAdvicerConfig")  
public class ControllerAdvicerController {  
    @RequestMapping("byZero")  
    public int byZero() {  
        return 1/0;  
    }
}
```

我们只需要使用`@ControllerAdvice`配置全局异常处理器，使用`@ExceptionHandler`标明处理的异常：
```java
@ControllerAdvice  
public class ControllerAdvicerConfig {  
    @ExceptionHandler(value = ArithmeticException.class)  
    @ResponseBody  
    public String exception(Exception e) {  
        return this.getClass().getName() + "\r\n" + e.getClass().getName() + "\r\n" + e.getMessage();  
    }  
}
```

上述业务接口触发后，抛出的`ArithmeticException`异常会分发到异常处理器的`exception()`方法中处理，最终返回以下内容：
```
com.example.servletmvc.config.ControllerAdvicerConfig
java.lang.ArithmeticException
/ by zero
```

通过`@ExceptionHandler`定义多个异常处理方法，即可对整个系统的各个异常进行统一处理。

# 2 核心知识
## 2.1 核心成员变量
`ExceptionHandlerExceptionResolver`的核心成员变量如下：
![[ExceptionHandlerExceptionResolver.png]]

`argumentResolvers`和`customArgumentResolvers`是参数解析器，负责将HTTP请求数据解析成异常处理方法的形参对象。

`returnValueHandlers`和`customReturnValueHandlers`是返回值处理器，负责将异常处理方法的返回值进行处理。

`messageConverters`是数据转换的底层工具，一方面从HTTP请求中读取并转换数据成对象，另一方面将对象转成HTTP响应数据。

`contentNegotiationManager`是负责媒体内容的校验，即根据`Content-Tepe`进行不同处理。

`responseBodyAdvice`是`ResponseBodyAdvice`的缓存，即支持在异常处理返回值写到输出流前的切面处理。

`applicationContext`是Spring上下文，可以从中获取容器中内容。

`exceptionHandlerCache`是异常-异常处理方法的缓存。

`exceptionHandlerAdviceCache`是`@ControllerAdvice`全局异常处理器的缓存。

## 2.2 类层次结构

# 3 初始化流程

# 4 异常处理流程
