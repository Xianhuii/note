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

## 2.2 类层次结构

# 3 初始化流程

# 4 异常处理流程
