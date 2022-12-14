上篇文章里，我们讲解了`RequestMappingHandlerMapping`请求地址映射的初始化流程，理解了`@Controller`和`@RequestMapping`是如何被加载到缓存中的。

今天我们来进一步学习，在接收到请求时，`RequestMappingHandlerMapping`是如何进行请求地址映射的。

