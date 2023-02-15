# 1 实战
在Spring Boot项目中，如果使用内嵌Web服务器，可以很方便地注册`Servlet`、`Filter`和`Listener`等组件。

总的来说，包括以下方式：
- 开启`@ServletCompnentScan`功能，扫描标注`@WebServlet`、`@WebFilter`或`WebListener`的bean。
- 创建继承`RegistrationBean`的bean，自定义注册的组件。
- 

# 2 源码
## 2.1 创建内嵌Web服务器的节点
