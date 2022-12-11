上篇文章总结了`DispatcherServlet`的核心功能，今天趁热打铁，系统梳理`DispatcherServlet`处理请求的流程。

`DispatcherServlet`处理请求的核心方法是`doDispatch()`。在处理过程中，会协同使用各组件的功能，共同完成对请求的处理。

