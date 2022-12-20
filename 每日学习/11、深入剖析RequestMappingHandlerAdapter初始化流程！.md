之前我们总结过`DispatcherServlet`处理请求的流程，它会从`HandlerMapping`中获取`handler`，然后交给`HandlerAdapter`处理。

`RequestMappingHandlerMapping`和`RequestMappingHandlerAdapter`是一对，用来处理`@Controller`和`@RequestMapping`标注的API接口。

之前的文章详细总结了`RequestMappingHandlerMapp`的相关知识，本文开始介绍`RequestMappingHandlerAdapter`的初始化流程。

# 1 初始化核心功能组件
## 1.1 messageConverters
`messageConverters`成员变量是`RequestMappingHandlerAdapter`类型的列表。

`RequestMappingHandlerAdapter`会使用`HttpMessageConverter<T>`，根据HTTP请求的`Content-Type`，对HTTP请求数据进行类型转换：
1. 读取`HttpInputMessage`消息，转换成对应的类型`T`数据。
2. 将类型`T`的数据，输出到`HttpOutputMessage`中。

### 1.1.1 构造函数
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

默认添加的`HttpMessageConverter`实现类、支持的`Content-Type`以及转换数据类型`T`如下：
- `ByteArrayHttpMessageConverter`：`application/octet-stream`和`*/*`：`byte[]`
- `StringHttpMessageConverter`：`text/plain`和`*/*`：`String`
- `SourceHttpMessageConverter`：`application/xml`、`text/xml`和`application/*-xml`：`javax.xml.transform.Source`
- `AllEncompassingFormHttpMessageConverter`：会根据是否引入相关依赖来添加相应`HttpMessageConverter`，可以支持`application/x-www-form-urlencoded`、`multipart/form-data`、`multipart/mixed`等`Content-Type`，转换类型为`MultiValueMap<String, Object>`。

### 1.1.2 WebMvcConfigurationSupport#requestMappingHandlerAdapter
虽然`HttpMessageConverter`必定在构造函数中初始化，但是在日常项目中（Spring Boot），会使用`setMessageConverters()`方法覆盖上述默认值：
```java
public void setMessageConverters(List<HttpMessageConverter<?>> messageConverters) {  
   this.messageConverters = messageConverters;  
}
```

在`WebMvcConfigurationSupport#requestMappingHandlerAdapter`初始化`requestMappingHandlerAdapter`过程中，会将构造函数初始化的默认值覆盖掉：
```java
RequestMappingHandlerAdapter adapter = createRequestMappingHandlerAdapter();  
adapter.setMessageConverters(getMessageConverters());
```

我们实际使用的`HttpMessageConverter`来源于`WebMvcConfigurationSupport#getMessageConverters`方法：
```java
protected final List<HttpMessageConverter<?>> getMessageConverters() {  
   if (this.messageConverters == null) {  
      this.messageConverters = new ArrayList<>();  
      // 1、从WebMvcConfigurer#configureMessageConverters方法添加
      configureMessageConverters(this.messageConverters);  
      if (this.messageConverters.isEmpty()) {  
	      // 2、添加默认配置
         addDefaultHttpMessageConverters(this.messageConverters);  
      }  
      // 3、从WebMvcConfigurer#extendMessageConverters方法添加
      extendMessageConverters(this.messageConverters);  
   }  
   return this.messageConverters;  
}
```

从`WebMvcConfigurationSupport#getMessageConverters`方法的源码中，我们可以得到许多有用的信息。

在默认情况下，只要引入相关依赖，就可以通过`addDefaultHttpMessageConverters()`方法添加默认的`HttpMessageConverter`：
- `ByteArrayHttpMessageConverter`
- `StringHttpMessageConverter`
- `ResourceHttpMessageConverter`
- `ResourceRegionHttpMessageConverter`
- `SourceHttpMessageConverter`
- `AllEncompassingFormHttpMessageConverter`
- `AtomFeedHttpMessageConverter`
- `RssChannelHttpMessageConverter`
- `MappingJackson2XmlHttpMessageConverter`
- `Jaxb2RootElementHttpMessageConverter`
- `KotlinSerializationJsonHttpMessageConverter`
- `MappingJackson2HttpMessageConverter`
- `GsonHttpMessageConverter`
- `JsonbHttpMessageConverter`
- `MappingJackson2SmileHttpMessageConverter`
- `MappingJackson2CborHttpMessageConverter`

我们可以通过`WebMvcConfigurer`来添加自定义的`HttpMessageConverter`：
```java
@Configuration  
@EnableWebMvc  
public class WebMvcConfig implements WebMvcConfigurer {  
    @Override  
    public void configureMessageConverters(List<HttpMessageConverter<?>> converters) {  
          
    }  
  
    @Override  
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {  
          
    }  
}
```

这两种添加方式也有很大的区别，需要额外注意：
1. 使用`configureMessageConverters()`添加自定义`HttpMessageConverter`后，将不会调用`addDefaultHttpMessageConverters()`方法添加默认的`HttpMessageConverter`，这意味着很多默认功能都不能实现。
2. 使用`extendMessageConverters`可以在默认`HttpMessageConverter`的基础上，额外添加自定义的`HttpMessageConverter`，可以继续使用默认的消息转换功能。

## 1.2 initBinderAdviceCache、modelAttributeAdviceCache和requestResponseBodyAdvice
`initBinderAdviceCache`和`modelAttributeAdviceCache`都是`Map<ControllerAdviceBean, Set<Method>>`类型的成员变量。

`requestResponseBodyAdvice`的数据类型是`List<Object>`，它实际会存储`ControllerAdviceBean`的元素。

initBinderAdviceCache、modelAttributeAdviceCache和requestResponseBodyAdvice会在对应的节点，调用`@ControllerAdvice`标注的`bean`中对应的方法。

### 1.2.1 RequestMappingHandlerAdapter#afterPropertiesSet


# 2 初始化bean
