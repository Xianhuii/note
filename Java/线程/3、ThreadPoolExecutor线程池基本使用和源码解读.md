# 1 使用

# 2 理论

# 3 源码
![[ThreadPoolExecutor.png]]

## 3.1 提交任务
### 3.1.1 Runnable任务
`AbstractExecutorService#submit()`提交任务：
```java
public Future<?> submit(Runnable task) {  
    // 空值校验
    if (task == null) throw new NullPointerException();  
    // 创建任务
    RunnableFuture<Void> ftask = newTaskFor(task, null);  
    // 执行任务
    execute(ftask);  
    return ftask;  
}
```

`AbstractExecutorService#newTaskFor()`创建ftask任务：
```java
protected <T> RunnableFuture<T> newTaskFor(Runnable runnable, T value) {  
    return new FutureTask<T>(runnable, value);  
}
```

`ThreadPoolExecutor#execute()`执行任务：
```java
public void execute(Runnable command) {  
    // 控制校验
    if (command == null) throw new NullPointerException();  
    int c = ctl.get();  
    // 1、工作线程数 < corePoolSize：新建线程，并执行
    if (workerCountOf(c) < corePoolSize) {  
        if (addWorker(command, true))  
            return;  
        c = ctl.get();  
    }  
    // 2、工作线程数 > corePoolSize && 线程池未停止 && 任务队列未满：添加到任务队列
    if (isRunning(c) && workQueue.offer(command)) {  
        int recheck = ctl.get();  
        // 线程池未停止 && 从任务队列中移除任务：执行拒绝策略
        if (! isRunning(recheck) && remove(command))  
            reject(command);  
        // 工作线程数 == 0：新建线程，并执行
        else if (workerCountOf(recheck) == 0)  
            addWorker(null, false);  
    }  
    // 2、工作线程数 > corePoolSize && 任务队列已满：新建额外线程，并执行
    else if (!addWorker(command, false))  
        // 最大线程数已满：执行拒绝策略
        reject(command);  
}
```
