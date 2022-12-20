之前我们总结过`DispatcherServlet`处理请求的流程，它会从`HandlerMapping`中获取`handler`，然后交给`HandlerAdapter`处理。

`RequestMappingHandlerMapping`和`RequestMappingHandlerAdapter`是一对，用来处理`@Controller`和`@RequestMapping`标注的API接口。

之前的文章详细总结了`RequestMappingHandlerMapp`的相关知识，本文开始介绍`RequestMappingHandlerAdapter`的初始化流程。

# 1 初始化核心功能组件
## 1.1 HttpMessageConverter
`RequestMappingHandlerAdapter`会使用`HttpMessageConverter<T>`对消息进行类型转换：
1. 读取`HttpInputMessage`消息，转换成对应的类型`T`数据。
2. 将类型`T`的数据，输出到`HttpOutputMessage`中。

在`RequestMappingHandlerAdapter`的构造函数中，默认会添加很多`HttpMessageConverter`实现类：
```java
public RequestMappingHandlerAdapter() {  
   this.messageConverters = new ArrayList<>(4);  
   this.messageConverters.add(new ByteArrayHttpMessageConverter());  
   this.messageConverters.add(new StringHttpMessageConverter());  
   if (!shouldIgnoreXml) {  
      try {  
         this.messageConverters.add(new SourceHttpMessageConverter<>());  
      }  
      catch (Error err) {  
         // Ignore when no TransformerFactory implementation is available  
      }  
   }  
   this.messageConverters.add(new AllEncompassingFormHttpMessageConverter());  
}
```


# 2 初始化bean
