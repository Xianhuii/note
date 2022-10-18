# 1 什么是CORS？
CORS是Cross-Origin Resource Sharing的缩写，意思是**跨域资源共享**。
互联网世界里有无数个网站，每个网站都有自己的”门牌号“：`协议://域名:端口`。
当网站A想要通过AJAX请求获取网站B的数据时，就会发生跨域请求。
```plantuml
A -> B : hello
```
本质上，CORS是HTTP协议对**浏览器**中**不同网站**间**AJAX请求**的规范和限制。
