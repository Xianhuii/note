# 1 使用

# 2 理论

# 3 源码
![[ThreadPoolExecutor.png]]

## 3.1 提交任务
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
public <T> Future<T> submit(Callable<T> task) {  
    // 空值校验
    if (task == null) throw new NullPointerException();  
    // 创建任务
    RunnableFuture<T> ftask = newTaskFor(task);  
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

protected <T> RunnableFuture<T> newTaskFor(Callable<T> callable) {  
    return new FutureTask<T>(callable);  
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

## 3.2 创建工作线程
`ThreadPoolExecutor#addWorker()`创建工作线程：
```java
private boolean addWorker(Runnable firstTask, boolean core) {  
    retry:  // 循环标签
    for (;;) {  
        int c = ctl.get();  
        int rs = runStateOf(c);  
  
        // Check if queue empty only if necessary.  
        if (rs >= SHUTDOWN &&  
            ! (rs == SHUTDOWN &&  
               firstTask == null &&  
               ! workQueue.isEmpty()))  
            return false;  
  
        for (;;) {  
            int wc = workerCountOf(c);  
            // 工作线程数大于最大容量（corePoolSize或maximumPoolSize）：返回false
            if (wc >= CAPACITY ||  
                wc >= (core ? corePoolSize : maximumPoolSize))  
                return false;  
            // CAS操作：工作线程数+1（成功会跳出外层循环）
            if (compareAndIncrementWorkerCount(c))  
                break retry;  
            c = ctl.get();  // Re-read ctl  
            // CAS操作失败 && 工作线程状态不一致：继续外层循环
            if (runStateOf(c) != rs)  
                continue retry;  
            // CAS操作失败：继续内层循环
        }  
    }  
  
    boolean workerStarted = false;  
    boolean workerAdded = false;  
    Worker w = null;  
    try {  
        // 新建Worker
        w = new Worker(firstTask);  
        final Thread t = w.thread;  
        if (t != null) {  
            final ReentrantLock mainLock = this.mainLock;  
            mainLock.lock();  
            try {  
                // Recheck while holding lock.  
                // Back out on ThreadFactory failure or if                
                // shut down before lock acquired.                
                int rs = runStateOf(ctl.get());  
  
                if (rs < SHUTDOWN ||  
                    (rs == SHUTDOWN && firstTask == null)) {  
                    if (t.isAlive()) // precheck that t is startable  
                        throw new IllegalThreadStateException();  
                    // 添加到Worker集合
                    workers.add(w);  
                    int s = workers.size();  
                    if (s > largestPoolSize)  
                        largestPoolSize = s;  
                    workerAdded = true;  
                }  
            } finally {  
                mainLock.unlock();  
            }  
            if (workerAdded) {  
                // 启动工作线程
                t.start();  
                workerStarted = true;  
            }  
        }  
    } finally {  
        if (! workerStarted)  
            addWorkerFailed(w);  
    }  
    return workerStarted;  
}
```

## 3.3 拒绝策略
`ThreadPoolExecutor#reject()`方法是执行拒绝策略的入口：
```java
final void reject(Runnable command) {  
    handler.rejectedExecution(command, this);  
}
```

`ThreadPoolExecutor.AbortPolicy#rejectedExecution()`会抛出异常：
```java
public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {  
    throw new RejectedExecutionException("Task " + r.toString() +  
                                         " rejected from " +  
                                         e.toString());  
}
```

`ThreadPoolExecutor.CallerRunsPolicy#rejectedExecution()`会使用当前线程执行任务：
```java
public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {  
    // 如果线程池没有停止
    if (!e.isShutdown()) {  
        // 直接执行run()方法，即使用当前线程执行任务
        r.run();  
    }  
}
```

`ThreadPoolExecutor.DiscardOldestPolicy#rejectedExecution()`会先移除队列头的任务，然后执行当前任务：
```java
public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {  
    // 如果线程池没有停止
    if (!e.isShutdown()) {  
        // 移除队列头最老的任务
        e.getQueue().poll();  
        // 执行当前任务
        e.execute(r);  
    }  
}
```

`ThreadPoolExecutor.DiscardPolicy#rejectedExecution()`会丢弃当前任务：
```java
public void rejectedExecution(Runnable r, ThreadPoolExecutor e) {  
}
```
