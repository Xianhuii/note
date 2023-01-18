![[ApplicationContext.png]]

# 1 AnnotationConfigApplicationContext
```java
AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();  
context.register(AppConfig.class);  
context.scan("applicationcontext");  
context.refresh();  
AppConfig bean = context.getBean(AppConfig.class);  
System.out.println(bean);
```

## 1.1 构造函数
```java
public AnnotationConfigApplicationContext() {  
   this.reader = new AnnotatedBeanDefinitionReader(this);  
   this.scanner = new ClassPathBeanDefinitionScanner(this);  
}
```

# 2 AnnotationConfigServletWebServerApplicationContext
