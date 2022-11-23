`org.springframework.web.multipart.MultipartResolver`是Spring-Web针对[RFC1867](https://www.ietf.org/rfc/rfc1867.txt)实现的多文件上传解决策略。
# 1 使用场景
前端上传文件时，无论是使用比较传统的表单，还是使用`FormData`对象，其本质都是发送一个`multipart/form-data`请求。
例如，前端模拟上传代码如下：
```javascript
var formdata = new FormData();
formdata.append("key1", "value1");
formdata.append("key2", "value2");
formdata.append("file1", fileInput.files[0], "/d:/Downloads/rfc1867.pdf");
formdata.append("file2", fileInput.files[0], "/d:/Downloads/rfc1314.pdf");

var requestOptions = {
  method: 'POST',
  body: formdata,
  redirect: 'follow'
};

fetch("http://localhost:10001/file/upload", requestOptions)
  .then(response => response.text())
  .then(result => console.log(result))
  .catch(error => console.log('error', error));
```
实际会发送如下HTTP请求：
```HTTP
POST /file/upload HTTP/1.1
Host: localhost:10001
Content-Length: 536
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary7MA4YWxkTrZu0gW

----WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="key1"

value1
----WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="key2"

value2
----WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file1"; filename="/d:/Downloads/rfc1867.pdf"
Content-Type: application/pdf

(data)
----WebKitFormBoundary7MA4YWxkTrZu0gW
Content-Disposition: form-data; name="file2"; filename="/d:/Downloads/rfc1314.pdf"
Content-Type: application/pdf

(data)
----WebKitFormBoundary7MA4YWxkTrZu0gW

```
在后端可以通过`MultipartHttpServletRequest`接收文件：
```Java
@RestController  
@RequestMapping("file")  
public class FileUploadController {   
    @RequestMapping("/upload")  
    public String upload(MultipartHttpServletRequest request) {  
        // 获取非文件参数  
        String value1 = request.getParameter("key1");  
        System.out.println(value1); // value1  
        String value2 = request.getParameter("key2");  
        System.out.println(value2); // value2  
        // 获取文件  
        MultipartFile file1 = request.getFile("file1");  
        System.out.println(file1 != null ? file1.getOriginalFilename() : "null"); // rfc1867.pdf  
        MultipartFile file2 = request.getFile("file2");  
        System.out.println(file2 != null ? file2.getOriginalFilename() : "null"); // rfc1314.pdf  
        return "Hello MultipartResolver!";  
    }  
}
```
# 2 MultipartResolver接口
`org.springframework.web.multipart.MultipartResolver`是Spring-Web根据[RFC1867](https://www.ietf.org/rfc/rfc1867.txt)规范实现的多文件上传的策略接口。
同时，`MultipartResolver`是Spring对文件上传处理流程在接口层次的抽象。
也就是说，当涉及到文件上传时，Spring都会使用MultipartResolver`接口进行处理，而不涉及具体实现类。
`MultipartResolver`接口源码如下：
```java
public interface MultipartResolver {  
	/**
	* 判断当前HttpServletRequest请求是否是文件请求
	*/
    boolean isMultipart(HttpServletRequest request);  
	/**
	*  将当前HttpServletRequest请求的数据（文件和普通参数）封装成MultipartHttpServletRequest对象
	*/
    MultipartHttpServletRequest resolveMultipart(HttpServletRequest request) throws MultipartException;  
	/**
	*  清除文件上传产生的临时资源（如服务器本地临时文件）
	*/
    void cleanupMultipart(MultipartHttpServletRequest request);  
}
```

`DispatcherServlet`中持有`MultipartResolver`成员变量：
```java
public class DispatcherServlet extends FrameworkServlet {  
   /** Well-known name for the MultipartResolver object in the bean factory for this namespace. */  
   public static final String MULTIPART_RESOLVER_BEAN_NAME = "multipartResolver";
   /** MultipartResolver used by this servlet. */  
	@Nullable  
	private MultipartResolver multipartResolver;
}
```
`DispatcherServlet`在初始化时，会从Spring容器中获取名为`multipartResolver`的对象（该对象是`MultipartResolver`实现类），作为文件上传解析器：
```java
/**  
 * Initialize the MultipartResolver used by this class. * <p>If no bean is defined with the given name in the BeanFactory for this namespace,  
 * no multipart handling is provided. */
private void initMultipartResolver(ApplicationContext context) {  
   try {  
      this.multipartResolver = context.getBean(MULTIPART_RESOLVER_BEAN_NAME, MultipartResolver.class);  
      if (logger.isTraceEnabled()) {  
         logger.trace("Detected " + this.multipartResolver);  
      }  
      else if (logger.isDebugEnabled()) {  
         logger.debug("Detected " + this.multipartResolver.getClass().getSimpleName());  
      }  
   }  
   catch (NoSuchBeanDefinitionException ex) {  
      // Default is no multipart resolver.  
      this.multipartResolver = null;  
      if (logger.isTraceEnabled()) {  
         logger.trace("No MultipartResolver '" + MULTIPART_RESOLVER_BEAN_NAME + "' declared");  
      }  
   }  
}
```
需要注意的是，如果Spring容器中不存在名为`multipartResolver`的对象，`DispatcherServlet`并不会额外指定默认的文件解析器。此时，`DispatcherServlet`不会对文件上传请求进行处理。也就是说，尽管当前请求是文件请求，也不会被处理成`MultipartHttpServletRequest`，如果我们在控制层进行强制类型转换，会抛异常。

`DispatcherServlet`在处理业务时，会按照顺序分别调用这些方法进行文件上传处理，相关核心源码如下：
```java
protected void doDispatch(HttpServletRequest request, HttpServletResponse response) throws Exception {  
   HttpServletRequest processedRequest = request;
   boolean multipartRequestParsed = false;
   try {
		// 判断&封装文件请求
         processedRequest = checkMultipart(request);  
         multipartRequestParsed = (processedRequest != request); 
         // 请求处理……
   }  
   finally {   
         // 清除文件上传产生的临时资源
         if (multipartRequestParsed) {  
            cleanupMultipart(processedRequest);  
         }  
   }  
}
```
在`checkMultipart()`方法中，会进行判断、封装文件请求：
```java
/**  
 * Convert the request into a multipart request, and make multipart resolver available. * <p>If no multipart resolver is set, simply use the existing request.  
 * @param request current HTTP request  
 * @return the processed request (multipart wrapper if necessary) * @see MultipartResolver#resolveMultipart  
 */
 protected HttpServletRequest checkMultipart(HttpServletRequest request) throws MultipartException {  
   if (this.multipartResolver != null && this.multipartResolver.isMultipart(request)) {  
      if (WebUtils.getNativeRequest(request, MultipartHttpServletRequest.class) != null) {  
         if (DispatcherType.REQUEST.equals(request.getDispatcherType())) {  
            logger.trace("Request already resolved to MultipartHttpServletRequest, e.g. by MultipartFilter");  
         }  
      }  
      else if (hasMultipartException(request)) {  
         logger.debug("Multipart resolution previously failed for current request - " +  
               "skipping re-resolution for undisturbed error rendering");  
      }  
      else {  
         try {  
            return this.multipartResolver.resolveMultipart(request);  
         }  
         catch (MultipartException ex) {  
            if (request.getAttribute(WebUtils.ERROR_EXCEPTION_ATTRIBUTE) != null) {  
               logger.debug("Multipart resolution failed for error dispatch", ex);  
               // Keep processing error dispatch with regular request handle below  
            }  
            else {  
               throw ex;  
            }  
         }  
      }  
   }  
   // If not returned before: return original request.  
   return request;  
}
```
总的来说，`DispatcherServlet`处理文件请求会经过以下步骤：
1. 判断当前HttpServletRequest请求是否是文件请求
	1. 是：将当前`HttpServletRequest`请求的数据（文件和普通参数）封装成`MultipartHttpServletRequest`对象
	2. 不是：不处理
2. `DispatcherServlet`对原始`HttpServletRequest`或`MultipartHttpServletRequest`对象进行业务处理
3. 业务处理完成，清除文件上传产生的临时资源

Spring提供了两个`MultipartResolver`实现类：
- `org.springframework.web.multipart.support.StandardServletMultipartResolver`：根据Servlet 3.0+ Part Api实现
- `org.springframework.web.multipart.commons.CommonsMultipartResolver`：根据Apache Commons FileUpload实现

在Spring Boot 2.0+中，默认会在`org.springframework.boot.autoconfigure.web.servlet.MultipartAutoConfiguration`中创建`StandardServletMultipartResolver`作为默认文件解析器：
```java
@AutoConfiguration  
@ConditionalOnClass({ Servlet.class, StandardServletMultipartResolver.class, MultipartConfigElement.class })  
@ConditionalOnProperty(prefix = "spring.servlet.multipart", name = "enabled", matchIfMissing = true)  
@ConditionalOnWebApplication(type = Type.SERVLET)  
@EnableConfigurationProperties(MultipartProperties.class)  
public class MultipartAutoConfiguration {  
  
   private final MultipartProperties multipartProperties;  
  
   public MultipartAutoConfiguration(MultipartProperties multipartProperties) {  
      this.multipartProperties = multipartProperties;  
   }  
  
   @Bean  
   @ConditionalOnMissingBean({ MultipartConfigElement.class, CommonsMultipartResolver.class })  
   public MultipartConfigElement multipartConfigElement() {  
      return this.multipartProperties.createMultipartConfig();  
   }  
  
   @Bean(name = DispatcherServlet.MULTIPART_RESOLVER_BEAN_NAME)  
   @ConditionalOnMissingBean(MultipartResolver.class)  
   public StandardServletMultipartResolver multipartResolver() {  
      StandardServletMultipartResolver multipartResolver = new StandardServletMultipartResolver();  
      multipartResolver.setResolveLazily(this.multipartProperties.isResolveLazily());  
      return multipartResolver;  
   }  
}
```

当需要指定其他文件解析器时，只需要引入相关依赖，然后配置一个名为`multipartResolver`的`bean`对象：
```java
@Bean  
public MultipartResolver multipartResolver() {  
    MultipartResolver multipartResolver = ...;  
    return multipartResolver;  
}
```

接下来，我们分别详细介绍两种实现类的使用和原理。
# 3 StandardServletMultipartResolver解析器
顾名思义，`StandardServletMultipartResolver`是根据标准Servlet 3.0实现的解析器。
在Servlet 3.0中定义了`javax.servlet.http.Part`，用来表示`multipart/form-data`请求体中的表单数据或文件：
```java
public interface Part {  
	public InputStream getInputStream() throws IOException;  
	public String getContentType();  
	public String getName();  
	public String getSubmittedFileName();  
	public long getSize();  
	public void write(String fileName) throws IOException;  
	public void delete() throws IOException;  
	public String getHeader(String name);  
	public Collection<String> getHeaders(String name);  
	public Collection<String> getHeaderNames();  
}
```
在`javax.servlet.http.HttpServletRequest`，提供了获取`multipart/form-data`请求体各个部分的方法：
```java
public interface HttpServletRequest extends ServletRequest {    
    /**  
     * Return a collection of all uploaded Parts.     
     *     
     * @return A collection of all uploaded Parts.    
     * @throws IOException  
     *             if an I/O error occurs  
     * @throws IllegalStateException  
     *             if size limits are exceeded or no multipart configuration is  
     *             provided     * @throws ServletException  
     *             if the request is not multipart/form-data  
     * @since Servlet 3.0     
     */   
	public Collection<Part> getParts() throws IOException, ServletException;  
  
    /**  
     * Gets the named Part or null if the Part does not exist. Triggers upload     
     * of all Parts.    
     *     
     * @param name The name of the Part to obtain  
     *     
     * @return The named Part or null if the Part does not exist     * @throws IOException  
     *             if an I/O error occurs  
     * @throws IllegalStateException  
     *             if size limits are exceeded  
     * @throws ServletException  
     *             if the request is not multipart/form-data  
     * @since Servlet 3.0     
     */    
	public Part getPart(String name) throws IOException, ServletException;  
}
```
