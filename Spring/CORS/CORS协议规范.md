# 1 什么是CORS？
CORS是Cross-Origin Resource Sharing的缩写，意思是**跨域资源共享**。
CORS是HTTP协议对**浏览器**中**AJAX请求**的规范和限制。
例如，钓鱼网站（A）获取了正规网站（B）的用户token后，A模拟AJAX请求对B进行操作。由于CORS的存在，浏览器会禁止该AJAX请求。
