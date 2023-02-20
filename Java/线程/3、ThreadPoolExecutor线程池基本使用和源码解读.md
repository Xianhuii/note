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

## 3.4 执行工作线程
在创建工作线程（`ThreadPoolExecutor#addWorker()`）时，会执行`Worker#Worker()`构造函数，将`Work`作为Runnable任务注入线程：
```java
Worker(Runnable firstTask) {  
    setState(-1); // inhibit interrupts until runWorker  
    this.firstTask = firstTask;  
    this.thread = getThreadFactory().newThread(this);  
}
```

后续在启动工作线程时，实际上会执行`ThreadPoolExecutor.Worker#run()`方法：
```java
public void run() {  
    runWorker(this);  
}
```

实际业务位于`ThreadPoolExecutor#runWorker()`：
```java
final void runWorker(Worker w) {  
    Thread wt = Thread.currentThread();  
    Runnable task = w.firstTask;  // 获取当前开发人员提交的任务
    w.firstTask = null;  // 清空任务
    w.unlock(); // allow interrupts  
    boolean completedAbruptly = true;  
    try {  
        // 循环执行
        // 1、如果task存在，执行task
        // 2、否则，获取队列中任务task，如果存在，执行task
        while (task != null || (task = getTask()) != null) {  
            w.lock();  
            // If pool is stopping, ensure thread is interrupted;  
            // if not, ensure thread is not interrupted.  This            
            // requires a recheck in second case to deal with            
            // shutdownNow race while clearing interrupt            
            if ((runStateAtLeast(ctl.get(), STOP) ||  
                 (Thread.interrupted() &&  
                  runStateAtLeast(ctl.get(), STOP))) &&  
                !wt.isInterrupted())  
                wt.interrupt();  
            try {  
                beforeExecute(wt, task);  
                Throwable thrown = null;  
                try {  
                    // 在work线程中执行task的run()方法
                    task.run();  
                } catch (RuntimeException x) {  
                    thrown = x; throw x;  
                } catch (Error x) {  
                    thrown = x; throw x;  
                } catch (Throwable x) {  
                    thrown = x; throw new Error(x);  
                } finally {  
                    afterExecute(task, thrown);  
                }  
            } finally {  
                task = null;  
                w.completedTasks++;  
                w.unlock();  
            }  
        }  
        completedAbruptly = false;  
    } finally {  
        processWorkerExit(w, completedAbruptly);  
    }  
}
```

## 3.5 工作线程存活时间
### 3.5.1 getTask中的线程存活策略
当`worker`启动后，会在工作线程中循环执行`firstTask`和`workQueue`中的任务。

在获取`workQueue`中的任务时，会判断当前线程是否需要剔除：
1. 剔除情况一：开启`allowCoreThreadTimeOut`功能。
2. 剔除情况二：工作线程数 > 核心线程数。

如果上述条件满足，会从`workQueue`中获取任务（等待`keepAliveTime`时间）：
```java
workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS);
```

如果在`keepAliveTime`时间内，队列中都没有多余的任务，那么就会设置线程过期：
```java
timedOut = true;
```

随后统计工作线程数，并退出获取任务的循环：
```java
if ((wc > maximumPoolSize || (timed && timedOut))  
    && (wc > 1 || workQueue.isEmpty())) {  
    if (compareAndDecrementWorkerCount(c))  
        return null;  
    continue;  
}
```

具体源码位于`ThreadPoolExecutor#getTask()`方法（标注了各种可能的执行顺序）：
```java
private Runnable getTask() {  
    // 0、设置timeOut标志为false
    boolean timedOut = false;
  
    for (;;) {  
        // 2、获取当前线程状态
        // 9、获取当前线程状态
        int c = ctl.get();  
        int rs = runStateOf(c);  
  
        // 主要于线程池关闭有关，这里不过多考虑
        if (rs >= SHUTDOWN && (rs >= STOP || workQueue.isEmpty())) {  
            decrementWorkerCount();  
            return null;  
        }  
  
        // 3、获取当前工作线程数量
        // 10、获取当前工作线程数量
        int wc = workerCountOf(c);  
  
        // 4、判断是否开启线程过期功能：开启核心线程过期功能 || 存在非核心线程
        // 11、判断是否开启线程过期功能：开启核心线程过期功能 || 存在非核心线程
        boolean timed = allowCoreThreadTimeOut || wc > corePoolSize;  
  
        // 5、正常情况下，第一次进入总是false（因为timedOut为false）
        // 12、线程过期&(工作线程数量>1或任务队列为空)：计算工作线程数，并返回
        if ((wc > maximumPoolSize || (timed && timedOut))  
            && (wc > 1 || workQueue.isEmpty())) {  
            if (compareAndDecrementWorkerCount(c))  
                return null;  
            continue;  
        }  
  
        try {  
            // 6、开启线程过期功能：workQueue.poll()，阻塞keepAliveTime纳秒
            // 没有开启线程过期功能：workQueue.take()，一直阻塞
            // 13、如果此时(工作线程数量=1&任务队列不为空)：将新任务交给当前线程去执行
            Runnable r = timed ?  
                workQueue.poll(keepAliveTime, TimeUnit.NANOSECONDS) :  
                workQueue.take();  
            // 7、任务不为空：返回任务
            // 14、返回新任务
            if (r != null)  
                return r;  
            // 8、任务为空（开启线程过期功能）：设置timedOut为true，进入下一次循环
            timedOut = true;  
        } catch (InterruptedException retry) {  
            timedOut = false;  
        }  
    }  
}
```

### 3.5.2 processWorkerExit中的线程存活策略
当上述`ThreadPoolExecutor#getTask()`方法获取不到任务时，会跳出`worker`执行任务的循环，在`finally`代码块中执行`ThreadPoolExecutor#processWorkerExit()`方法。

在`processWorkerExit()`方法中，会对当前线程的工作情况进行汇总，然后根据线程池的工作线程情况进行判断：真正剔除当前线程，或者新建一个替换线程。

简单来说，会计算当前线程池能够容忍的最小工作线程数量：
1. 如果当前工作线程数量 >= 最小工作线程数量：剔除当前线程。
2. 如果当前工作线程数量 < 最小工作线程数量：剔除当前线程，但是会一个新增工作线程用作替代
3. 。

`ThreadPoolExecutor#processWorkerExit()`：
```java
private void processWorkerExit(Worker w, boolean completedAbruptly) {  
    // 线程池中断的情况，这里不过多考虑
    if (completedAbruptly)
        decrementWorkerCount();  
  
    // 汇总当前线程的任务完成数量，从workers中移除当前worker
    final ReentrantLock mainLock = this.mainLock;  
    mainLock.lock();  
    try {  
        completedTaskCount += w.completedTasks;  
        workers.remove(w);  
    } finally {  
        mainLock.unlock();  
    }  
  
    // 终止当前线程
    tryTerminate();  
  
    // 判断当前线程池的工作线程情况
    int c = ctl.get();  
    if (runStateLessThan(c, STOP)) {  
        if (!completedAbruptly) {  
            // 工作线程的最小数量
            int min = allowCoreThreadTimeOut ? 0 : corePoolSize;  
            // 如果任务队列不为空，至少需要留一个工作线程
            if (min == 0 && ! workQueue.isEmpty())  
                min = 1;  
            // 如果当前工作线程数量 >= 最小数量：剔除当前工作线程（实际上就是不操作，让它走完run方法）
            if (workerCountOf(c) >= min)  
                return; // replacement not needed  
        }  
        // 如果当前工作线程数量 < 最小数量：新增一个work，用来替换当前工作线程
        addWorker(null, false);  
    }  
}
```