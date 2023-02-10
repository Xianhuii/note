SpringBoot为Web应用提供了内置Web服务器，我们不用再额外下载`Tomcat`、`Jetty`、`Undertow`等服务器。

`spring-boot-autoconfigure`中提供了自动配置内置Web服务器的功能，只要添加了相关依赖，就会配置对应的Web服务器。

对于`spring-boot-starter-web`：
- `spring-boot-starter-tomcat`（默认）：内置Tomcat服务器。
- `spring-boot-starter-jetty`：内置Jetty服务器。
- `spring-boot-starter-undertow`：内置Undertow服务器。

对于`spring-boot-starter-webflux`：
- `spring-boot-starter-reactor-netty`（默认）：使用Netty监听网络请求。
- `spring-boot-starter-tomcat`：内置Tomcat服务器。
- `spring-boot-starter-jetty`：内置Jetty服务器。
- `spring-boot-starter-undertow`：内置Undertow服务器。
