上篇文章总结了`DispatcherServlet`的核心功能，今天趁热打铁，系统梳理`DispatcherServlet`处理请求的流程。

`DispatcherServlet`处理请求的核心方法是`doDispatch()`。在处理过程中，会协同使用各组件的功能，共同完成对请求的处理。

以下是`doDispatch()`的执行流程图：
![[DispatcherServlet处理请求流程.png]]

# 1 文件请求预处理

# 2. 获取请求处理器执行链

# 3 获取处理器适配器

# 4 拦截器预处理

# 5 处理器适配器处理请求

# 6 拦截器后处理

# 7 结果处理

# 8 文件请求后处理
