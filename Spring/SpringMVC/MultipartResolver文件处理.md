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
`DispatcherServlet`中会按照顺序分别调用这些方法进行文件上传处理，部分源码如下：
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
上传文件的，这体现了对多态的使用。