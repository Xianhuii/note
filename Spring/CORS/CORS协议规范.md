# 1 什么是CORS？
CORS是Cross-Origin Resource Sharing的缩写，意思是**跨域资源共享**。
本质上，CORS是HTTP协议对**浏览器**中**不同网站**间**AJAX请求**的规范和限制。
Web世界里有无数个网站，每个网站都有自己的”门牌号“：`协议://域名:端口`。
网站是部署在服务器某个端口上的应用进程，通过监听端口来接收外界的访问。外界通过域名（IP地址）来找到对应服务器，通过协议/端口来找到对应应用进程。
因此，协议/域名/端口可以定位网站在哪一个服务器，哪一个端口上。这三个只要有一个不同，说明该应用程序是不同的进程，就是不同的网站。
而跨域指的就是跨越网站。
当网站A想要通过AJAX请求获取网站B的数据（资源）时，就会发生跨域请求。因为它们是不同进程，不能保证对方的安全性。
如同陌生人想去你家拿东西或者放东西，楼下保安必须校验陌生人的身份信息。
浏览器就是Web世界里的保安，它根据CORS协议来核实AJAX请求是否被目标网站所允许。
```plantuml
A -> 浏览器 : AJAX请求
浏览器 -> 浏览器 : CORS预处理
浏览器 -> B : HTTP请求
B -> 浏览器 : HTTP响应
浏览器 -> 浏览器 : CORS校验
浏览器 -> A : AJAX响应
```
# 2 如何辨别跨域请求？
在CORS预处理阶段，浏览器会为每个AJAX请求添加以下请求头：
- Host：目标网站的“门牌号”
- Origin：当前网站的“门牌号”
例如，我们在`https://www.baidu.com/`网站打开控制台，输入以下代码：
```
fetch("http://localhost:8080")
```
这次请求的两个请求头为：
```
Host: localhost:8080
Origin: https://www.baidu.com
```
当`Host`和`Origin`不一致时，浏览器就判断这个是跨域请求，它就会根据CORS协议采取一定的校验措施。
# 3 简单请求&复杂请求&实际请求&预检请求
## 3.1 简单请求
出于性能和安全的考虑，浏览器将用户的CORS请求分为简单请求和复杂请求两种类型，分别进行不同的校验流程。
满足以下条件的为简单请求：
- 请求方法为：`GET`或`HEAD`或`POST`
- 请求头为：
	- 浏览器用户代理自动设置的请求头：`Connection`、`User-Agent`等
	- [forbidden header name](https://fetch.spec.whatwg.org/#forbidden-header-name)：`Content-Length`、`Cookie`等
	- [CORS-safelisted request-header](https://fetch.spec.whatwg.org/#cors-safelisted-request-header)：`accept`、`accept-language`等
	- `Content-Type`值为：`text/plain`或 `multipart/form-data`或`application/x-www-form-urlencoded`
对于简单请求，浏览器会直接将该请求发送给服务器：
```plantuml
A -> 浏览器 : AJAX请求
浏览器 -> 浏览器 : CORS预处理
浏览器 -> B : HTTP请求
B -> 浏览器 : HTTP响应
浏览器 -> 浏览器 : CORS校验
浏览器 -> A : AJAX响应
```
## 3.2 复杂请求
不满足以上简单请求条件的就是复杂请求，比如：
- 请求头`Content-Type`为`application/json`
- 自定义请求头
对于复杂请求，浏览器会先发送一个轻量级的预检请求，询问目标网站是否允许访问。如果允许，才会发送实际请求：
```plantuml
A -> 浏览器 : AJAX请求
浏览器 -> 浏览器 : CORS预处理
浏览器 -> B : HTTP预检请求
B -> 浏览器 : HTTP预检响应
浏览器 -> 浏览器 : CORS校验
浏览器 -> 浏览器 : CORS预处理
浏览器 -> B : HTTP实际请求
B -> 浏览器 : HTTP实际响应
浏览器 -> 浏览器 : CORS校验
浏览器 -> A : AJAX响应
```
理论上，浏览器会为复杂请求发送两次请求。但实际各浏览器的实现有所不同，有些浏览器对待复杂请求和简单请求一样，只发送一次请求；有些浏览器虽然会发送预检请求，但不会检测预检请求结果，始终会发送第二次实际请求。
## 3.3 预检请求
在发送复杂请求前，浏览器会先发送一个轻量级的预检请求，询问网站是否允许访问。
预检请求的请求方法是`OPTIONS`，它在请求头中携带实际请求的请求方法和请求头等信息，而不会携带实际请求的数据：
- `Origin`：当前网站的“门牌号”
- `Access-Control-Request-Method`：实际请求的请求方法，如`POST`
- `Access-Control-Request-Headers`：实际请求的请求头，如`Content-Type`等，多个用`,`分隔
预检请求实际上是在询问服务器：“来自`origin`，携带`Access-Control-Request-Headers`请求头，请求方法是`Access-Control-Request-Method`的请求能不能访问？”。
## 3.4 实际请求
从服务器的角度来看，简单请求和复杂请求没有区别，都是会调用服务接口的实际请求。
而预检请求会在拦截器或预处理阶段就被系统自动处理，并不会调用到实际接口。
# 4 CORS预处理&校验
## 4.1 预处理
在发送跨域请求之前，浏览器会对请求进行一些处理，这样服务器才能判断这个请求是不是跨域请求，需不需要进行跨域处理。
对于简单请求和复杂请求，除了基本的请求方法和请求头，会额外添加标识当前网站地址的请求头：
- `Origin`：当前网站的`协议://域名:端口`
对于预检请求（`OPTIONS`方法），还会添加标识后续复杂请求信息的请求头：
- `Origin`：当前网站的“门牌号”
- `Access-Control-Request-Method`：实际请求的请求方法，如`POST`
- `Access-Control-Request-Headers`：实际请求的请求头，如`Content-Type`等，多个用`,`分隔
## 4.2 服务器校验
服务器如果有配置跨域，会按照以下几个方面对跨域请求进行处理：
1. 校验是否是跨域请求：`Origin`请求头与服务器进程的`协议://域名:端口`是否一致
2. 如果是跨域实际请求，会添加以下跨域响应头（如果有配置值）：
	1. 设置`Access-Control-Allow-Origin`响应头
	2. 设置`Access-Control-Expose-Headers`响应头
	3. 设置`Access-Control-Allow-Credentials`响应头
	4. 设置`Access-Control-Max-Age`响应头
3. 如果是跨域预检请求，除了上述响应头，会额外添加以下响应头（如果有配置值）：
	1. 设置`Access-Control-Allow-Methods`响应头
	2. 设置`Access-Control-Allow-Headers`响应头
## 4.3 浏览器校验
