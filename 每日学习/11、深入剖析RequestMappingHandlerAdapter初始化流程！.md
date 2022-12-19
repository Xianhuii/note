`RequestMappingHandlerAdapter`是最常用的`HandlerAdapter`实现类，它和`RequestMappingHandlerMapping`是一对，用来处理`@Controller`和`@RequestMapping`标注的API接口。

在处理请求过程中，`DispatcherServlet`会从`RequestMappingHandlerMapping`中获取对应的`HandlerMethod`，然后交给`RequestMappingHandlerAdapter`进行处理。


# 1 初始化核心功能组件

# 2 初始化bean
