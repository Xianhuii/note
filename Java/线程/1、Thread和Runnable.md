# 1 创建线程
我们可以通过继承`Thread`类来创建一个线程：
```java
Thread thread = new Thread() {  
    @Override  
    public void run() {  
        System.out.println("run()");  
    }  
};  
thread.start();  
System.out.println("main()");
```

也可以通过实现`Runnable`接口来创建一个线程：
```java
Thread thread = new Thread(new Runnable() {  
    @Override  
    public void run() {  
        System.out.println("run()");  
    }  
});  
thread.start();  
System.out.println("main()");
```

实际上，从执行流程上来看，这两种方式并没有太大的区别：
1. 通过`java.lang.Thread#start()`方法，调用本地方法启动线程。
2. 线程会执行`java.lang.Thread#run()`方法中的代码。

`java.lang.Thread#run()`源码如下：
```java
public void run() {  
    if (target != null) {  
        target.run();  
    }  
}
```

对于继承`Threa`类方式，我们重写了run()方法中的逻辑。

对于实现`Runnable`接口方式，Thread#run()方法会调用`target.run()`方法，转而执行我们重写的逻辑。这实际上是对代理模式的应用：
![[Runnable.png]]

# 2 实现Runnable接口的优点


# 3 Thread核心源码


