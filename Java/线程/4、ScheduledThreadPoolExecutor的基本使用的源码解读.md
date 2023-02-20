# 1 基本使用
`ScheduledThreadPoolExecutor`是一种特殊的线程池，它可以执行`延迟任务`和`定时任务`。

首先，通常会在全局范围内创建线程池对象，可以是静态变量，或者Spring单例对象：
```java
ThreadFactory threadFactory = Executors.defaultThreadFactory();  
RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();  
// 1、创建线程池  
ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(  
        3,  
        threadFactory,  
        rejectedExecutionHandler);
```

然后，在对应业务场景中，创建任务，并且提交到线程池：
```java
// 2、创建任务  
        Runnable task = new Runnable() {  
            @Override  
            public void run() {  
                System.out.println(Thread.currentThread().getName() + ": run()");  
                try {  
                    Thread.sleep(500);  
                } catch (InterruptedException e) {  
                    e.printStackTrace();  
                }  
            }  
        };  
        // 执行延时任务  
        scheduledThreadPoolExecutor.schedule(task, 1, TimeUnit.SECONDS);  
        // 执行定时任务  
        scheduledThreadPoolExecutor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
```

在整个线程池使用结束后，需要主动关闭线程池：
```java
scheduledThreadPoolExecutor.shutdown();  
// 或
scheduledThreadPoolExecutor.shutdownNow();
```

# 2 核心源码