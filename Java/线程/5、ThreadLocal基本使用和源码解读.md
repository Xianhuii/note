# 1 基本使用
`ThreadLocal`的作用是保存线程本地变量，在多线程在CPU时间分片交替执行过程中，每个线程只能获取到它自己的数据。

`ThreadLocal`的使用非常简单：
1. 创建`ThreadLocal`。
2. 线程执行逻辑前期，保存本地变量。
3. 线程执行逻辑后期，获取本地变量。
4. 线程处理完成之前，手动清除本地变量。

需要注意的是，这里的线程执行逻辑前期和后期，都是相对于线程本地变量而言的。

`ThreadLocal`本身必须是独立于线程之外的，通常会将它设置成静态变量，随着类加载而初始化：
```java
public static final ThreadLocal<String> threadName = new ThreadLocal<>();
```

在线程执行逻辑前期，可以保存线程本地变量：
```java
threadName.set(Thread.currentThread().getName());
```

在线程执行逻辑后期，可以获取线程本地变量：
```java
System.out.println("ThreadLocal: " + threadName.get());
```

在线程处理完成之前，需要手动清除线程本地变量，避免内存泄漏：
```java
threadName.remove();
```

举个简单的例子：
```java
public class ThreadLocalDemo {  
    // 1、创建ThreadLocal
    public static final ThreadLocal<String> threadName = new ThreadLocal<>();  
  
    public static void main(String[] args) {  
        Runnable task = new Runnable() {  
            @Override  
            public void run() {  
                // 2、保存线程本地变量  
                threadName.set(Thread.currentThread().getName());  
  
                // ……复杂的业务调用  
  
                // 3、获取线程本地变量  
                synchronized (threadName) {  
                    System.out.println("ThreadLocal: " + threadName.get());  
                    System.out.println("Thread.currentThread().getName()" + Thread.currentThread().getName());  
                }  
                // 4、线程执行完成前，手动移除本地变量  
                threadName.remove();  
            }  
        };  
        ThreadPoolExecutor threadPoolExecutor = threadPoolExecutor();  
        for (int i = 0; i < 10; i++) {  
            threadPoolExecutor.submit(task);  
        }  
        // 4、关闭线程池  
        threadPoolExecutor.shutdown();  
    }  
  
    public static ThreadPoolExecutor threadPoolExecutor() {  
        // ……创建线程池  
        return threadPoolExecutor;  
    }  
}
```

可能的执行结果如下，每个线程只能获取它自己保存的本地变量：
```
ThreadLocal: pool-1-thread-1
Thread.currentThread().getName()pool-1-thread-1
ThreadLocal: pool-1-thread-5
Thread.currentThread().getName()pool-1-thread-5
ThreadLocal: pool-1-thread-6
Thread.currentThread().getName()pool-1-thread-6
ThreadLocal: pool-1-thread-3
Thread.currentThread().getName()pool-1-thread-3
ThreadLocal: pool-1-thread-2
Thread.currentThread().getName()pool-1-thread-2
ThreadLocal: pool-1-thread-4
Thread.currentThread().getName()pool-1-thread-4
ThreadLocal: main
Thread.currentThread().getName()main
ThreadLocal: pool-1-thread-6
Thread.currentThread().getName()pool-1-thread-6
ThreadLocal: pool-1-thread-5
Thread.currentThread().getName()pool-1-thread-5
ThreadLocal: pool-1-thread-1
Thread.currentThread().getName()pool-1-thread-1
```

# 2 理论


# 3 源码
## 3.1 set
`java.lang.ThreadLocal#set()`方法：
```java
public void set(T value) {  
    Thread t = Thread.currentThread();  
    ThreadLocalMap map = getMap(t);  
    if (map != null)  
        map.set(this, value);  
    else  
        createMap(t, value);  
}
```

## 3.2 get
`java.lang.ThreadLocal#get()`方法：
```java
public T get() {  
    Thread t = Thread.currentThread();  
    ThreadLocalMap map = getMap(t);  
    if (map != null) {  
        ThreadLocalMap.Entry e = map.getEntry(this);  
        if (e != null) {  
            @SuppressWarnings("unchecked")  
            T result = (T)e.value;  
            return result;  
        }  
    }  
    return setInitialValue();  
}
```

## 3.3 remove
`java.lang.ThreadLocal#remove()`方法：
```java
public void remove() {  
    ThreadLocalMap m = getMap(Thread.currentThread());  
    if (m != null)  
        m.remove(this);  
}
```