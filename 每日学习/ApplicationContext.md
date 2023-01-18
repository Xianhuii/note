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
使用默认无参构造函数，会初始化`AnnotatedBeanDefinitionReader`和`ClassPathBeanDefinitionScanner`对象，用于读取依赖配置：
```java
public AnnotationConfigApplicationContext() {  
   this.reader = new AnnotatedBeanDefinitionReader(this);  
   this.scanner = new ClassPathBeanDefinitionScanner(this);  
}
```

## 1.2 register
`AnnotationConfigApplicationContext#register()`会调用内部的`AnnotatedBeanDefinitionReader`对象

# 2 AnnotationConfigServletWebServerApplicationContext
