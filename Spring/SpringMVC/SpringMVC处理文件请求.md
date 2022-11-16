# 1 判断文件请求
通过`org.springframework.web.multipart.MultipartResolver#isMultipart`方法可以判断是否是文件请求。
不同`MultipartResolver`实现类的判断方式不同。
例如SpringMVC默认的`StandardServletMultipartResolver`会判断请求头：
```java
public boolean isMultipart(HttpServletRequest request) {  
    return StringUtils.startsWithIgnoreCase(request.getContentType(), this.strictServletCompliance ? "multipart/form-data" : "multipart/");  
}
```
而`CommonsMultipartResolver`会判断请求方法和请求体：
```java
public boolean isMultipart(HttpServletRequest request) {  
    return this.supportedMethods != null ? this.supportedMethods.contains(request.getMethod()) && FileUploadBase.isMultipartContent(new ServletRequestContext(request)) : ServletFileUpload.isMultipartContent(request);  
}
```
# 2 文件请求处理
通过`org.springframework.web.multipart.MultipartResolver#resolveMultipart`方法可以解析文件请求。
`StandardServletMultipartResolver`：
```java
public MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException {  
    return new StandardMultipartHttpServletRequest(request, this.resolveLazily);  
}
```
`CommonsMultipartResolver`：
```java
public MultipartHttpServletRequest resolveMultipart(final HttpServletRequest request) throws MultipartException {  
   Assert.notNull(request, "Request must not be null");  
   if (this.resolveLazily) {  
      return new DefaultMultipartHttpServletRequest(request) {  
         @Override  
         protected void initializeMultipart() {  
            MultipartParsingResult parsingResult = parseRequest(request);  
            setMultipartFiles(parsingResult.getMultipartFiles());  
            setMultipartParameters(parsingResult.getMultipartParameters());  
            setMultipartParameterContentTypes(parsingResult.getMultipartParameterContentTypes());  
         }  
      };  
   }  
   else {  
      MultipartParsingResult parsingResult = parseRequest(request);  
      return new DefaultMultipartHttpServletRequest(request, parsingResult.getMultipartFiles(),  
            parsingResult.getMultipartParameters(), parsingResult.getMultipartParameterContentTypes());  
   }  
}
```
本质上，它们都只是将`HttpServletRequest`封装成`MultipartHttpServletRequest`实现类。
`MultipartHttpServletRequest`提供了了获取文件的方法。
需要注意的是，解析文件请求的核心实际上在于`org.springframework.web.multipart.support.StandardMultipartHttpServletRequest#parseRequest`和`org.springframework.web.multipart.commons.CommonsMultipartResolver#parseRequest`方法。它们会处理请求体中的实际内容（比如保存到服务器本地）。而`resolveLazily`参数可以设置是否延迟处理。比如`resolveLazily`为`true`时，只有在业务中实际获取文件信息才会进行解析。

