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

