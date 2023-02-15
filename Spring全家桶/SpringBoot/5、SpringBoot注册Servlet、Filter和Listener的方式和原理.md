# 1 实战
在Spring Boot项目中，如果使用内嵌Web服务器，可以很方便地注册`Servlet`、`Filter`和`Listener`等组件。

总的来说，包括以下方式：
- 开启`@ServletCompnentScan`功能，扫描标注`@WebServlet`、`@WebFilter`或`WebListener`的bean。
- 创建继承`RegistrationBean`的bean，自定义注册的组件。
- 

# 2 源码
## 2.1 创建内嵌Web服务器的节点
在`ServletWebServerApplicationContext`的`onRefresh()`阶段，会创建Web服务器：
```java
protected void onRefresh() {  
   // 调用父类的onRefresh()方法
   super.onRefresh();  
   try {  
      // 创建web服务器
      createWebServer();  
   }  
   catch (Throwable ex) {  
      // 抛异常
   }  
}
```

在`ServletWebServerApplicationContext#createWebServer()`方法中，会创建并初始化web服务器：
```java
private void createWebServer() {  
   WebServer webServer = this.webServer;  
   ServletContext servletContext = getServletContext();  
   if (webServer == null && servletContext == null) {  
      // 获取factory
      ServletWebServerFactory factory = getWebServerFactory();  
      // 传入getSelfInitializer()，创建web服务器
      this.webServer = factory.getWebServer(getSelfInitializer());  
   }  
   else if (servletContext != null) {  
      // 初始化servletContext
      getSelfInitializer().onStartup(servletContext);  
   }  
}
```

`ServletWebServerApplicationContext#getSelfInitializer()`是一个方法引用（匿名对象）：
```java
private org.springframework.boot.web.servlet.ServletContextInitializer getSelfInitializer() {  
   return this::selfInitialize;  
}
```

会在后续服务器初始化调用`ServletContextInitializer#onStartup()`方法时，触发`ServletWebServerApplicationContext#selfInitialize()`方法：
```java
private void selfInitialize(ServletContext servletContext) throws ServletException {  
   // 遍历调用ServletContextInitializer#onStartup()
   for (ServletContextInitializer beans : getServletContextInitializerBeans()) {  
      beans.onStartup(servletContext);  
   }  
}
```

对于内嵌Tomcat，它的初始化流程如下：
1. `TomcatServletWebServerFactory#getWebServer()`和`TomcatWebServer#TomcatWebServer()`方法创建服务器。
2. 在创建服务器过程中，将`selfInitialize`添加到`TomcatStarter`中。
3. `TomcatWebServer#initialize()`方法初始化服务器。
4. `Tomcat#start()`和`LifecycleBase#start()`启动服务器。
5. `StandardContext#startInternal()`启动上下文。
6. `javax.servlet.ServletContainerInitializer#onStartup()`初始化容器。

`org.springframework.boot.web.embedded.tomcat.TomcatStarter`实现了`javax.servlet.ServletContainerInitializer#onStartup()`，所以会直接触发前者的`onStartup()`方法：
```java
public void onStartup(Set<Class<?>> classes, ServletContext servletContext) throws ServletException {  
   try {  
      // 遍历所有ServletContextInitializer
      for (ServletContextInitializer initializer : this.initializers) {  
         initializer.onStartup(servletContext);  
      }  
   }  
   catch (Exception ex) {   
   }  
}
```

`org.springframework.boot.web.servlet.ServletContextInitializer`实现了`javax.servlet.ServletContainerInitializer#onStartup()`。如果我们在容器中注册了对应bean，会接触发前者的`onStartup()`方法：
```java
public void onStartup(@Nullable Set<Class<?>> webAppInitializerClasses, ServletContext servletContext)  
      throws ServletException {  
  
   List<WebApplicationInitializer> initializers = Collections.emptyList();  
  
   if (webAppInitializerClasses != null) {  
      initializers = new ArrayList<>(webAppInitializerClasses.size());  
      for (Class<?> waiClass : webAppInitializerClasses) {  
         // Be defensive: Some servlet containers provide us with invalid classes,  
         // no matter what @HandlesTypes says...         if (!waiClass.isInterface() && !Modifier.isAbstract(waiClass.getModifiers()) &&  
               WebApplicationInitializer.class.isAssignableFrom(waiClass)) {  
            try {  
               initializers.add((WebApplicationInitializer)  
                     ReflectionUtils.accessibleConstructor(waiClass).newInstance());  
            }  
            catch (Throwable ex) {  
               throw new ServletException("Failed to instantiate WebApplicationInitializer class", ex);  
            }  
         }  
      }  
   }  
  
   if (initializers.isEmpty()) {  
      return;  
   }  
  
   AnnotationAwareOrderComparator.sort(initializers);  
   for (WebApplicationInitializer initializer : initializers) {  
      initializer.onStartup(servletContext);  
   }  
}
```