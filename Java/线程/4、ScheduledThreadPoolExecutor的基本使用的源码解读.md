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
![[ScheduledThreadPoolExecutor.png]]

## 2.1 创建线程池
`ScheduledThreadPoolExecutor`继承了`ThreadPoolExecutor`，直接使用了父类的底层数据结构，但是为其中某些核心参数设置的固定值：
```java
public ScheduledThreadPoolExecutor(int corePoolSize,  
                                   ThreadFactory threadFactory,  
                                   RejectedExecutionHandler handler) {  
    super(corePoolSize, Integer.MAX_VALUE, 0, NANOSECONDS,  
          new DelayedWorkQueue(), threadFactory, handler);  
}
```

核心参数：
- 核心工作线程：`corePoolSize`
- 任务队列：固定为`DelayedWorkQueue`
- 额外工作线程：固定为`Integer.MAX_VALUE`
- 拒绝策略：`handler`
- 工作线程存活时间：固定为`0`

## 2.2 提交任务
### 2.2.1 延时任务
通过`ScheduledThreadPoolExecutor#schedule()`方法可以提交延时任务，需要指定延迟时间。该方法会返回`ScheduledFuture`对象，如果任务有返回值，可以通过`Future#get()`方法获取：
```java
public ScheduledFuture<?> schedule(Runnable command,  
                                   long delay,  
                                   TimeUnit unit) {  
    // 参数校验
    if (command == null || unit == null) throw new NullPointerException();  
    // 封装任务
    RunnableScheduledFuture<?> t = decorateTask(command,  
        new ScheduledFutureTask<Void>(command, null,  
                                      // 计算下次执行时间戳
                                      triggerTime(delay, unit)));  
    // 延时执行任务
    delayedExecute(t);  
    return t;  
}

public <V> ScheduledFuture<V> schedule(Callable<V> callable,  
                                       long delay,  
                                       TimeUnit unit) {  
    // 参数校验
    if (callable == null || unit == null) throw new NullPointerException();  
    // 封装任务
    RunnableScheduledFuture<V> t = decorateTask(callable,  
        new ScheduledFutureTask<V>(callable,  
                                   triggerTime(delay, unit)));  
    // 延时执行任务
    delayedExecute(t);  
    return t;  
}
```

### 2.2.2 定时任务
通过`ScheduledThreadPoolExecutor#scheduleAtFixedRate()`方法可以提交定时任务，需要指定第一次延迟执行时间和定时执行周期。该方法会返回`ScheduledFuture`对象，如果任务有返回值，可以通过`Future#get()`方法获取：
```java
public ScheduledFuture<?> scheduleAtFixedRate(Runnable command,  
                                              long initialDelay,  
                                              long period,  
                                              TimeUnit unit) {  
    // 参数校验
    if (command == null || unit == null) throw new NullPointerException();  
    if (period <= 0) throw new IllegalArgumentException();  
    // 封装任务
    ScheduledFutureTask<Void> sft =  
        new ScheduledFutureTask<Void>(command,  
                                      null,  
                                      // 计算下次执行时间戳
                                      triggerTime(initialDelay, unit),  
                                      // 计算定时执行周期
                                      unit.toNanos(period));  
    RunnableScheduledFuture<Void> t = decorateTask(command, sft);  
    sft.outerTask = t;  
    // 延迟执行任务
    delayedExecute(t);  
    return t;  
}

public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command,  
                                                 long initialDelay,  
                                                 long delay,  
                                                 TimeUnit unit) {  
    // 参数校验
    if (command == null || unit == null) throw new NullPointerException();  
    if (delay <= 0) throw new IllegalArgumentException();  
    // 封装任务
    ScheduledFutureTask<Void> sft =  
        new ScheduledFutureTask<Void>(command,  
                                      null,  
                                      triggerTime(initialDelay, unit),  
                                      unit.toNanos(-delay));  
    RunnableScheduledFuture<Void> t = decorateTask(command, sft);  
    sft.outerTask = t;  
    // 延迟执行任务
    delayedExecute(t);  
    return t;  
}
```

## 2.3 第一次执行任务
提交任务时，会通过`ScheduledThreadPoolExecutor#delayedExecute()`方法执行任务：
```java
private void delayedExecute(RunnableScheduledFuture<?> task) {  
    // 如果线程池已停止：执行拒绝策略
    if (isShutdown())  
        reject(task);  
    else {  
        // 将任务入队
        super.getQueue().add(task);  
        // 判断线程池状态
        if (isShutdown() &&  
            !canRunInCurrentRunState(task.isPeriodic()) &&  
            remove(task))  
            task.cancel(false);  
        // 创建工作线程
        else  
            ensurePrestart();  
    }  
}
```

在`ThreadPoolExecutor#ensurePrestart()`方法中会根据线程池状态来创建工作线程：
```java
void ensurePrestart() {  
    int wc = workerCountOf(ctl.get());  
    // 创建核心工作线程
    if (wc < corePoolSize)  
        addWorker(null, true);  
    // 创建额外工作线程
    else if (wc == 0)  
        addWorker(null, false);  
}
```

在创建工作线程时，会调用`ThreadPoolExecutor#addWorker()`方法，创建`Worker`对象，并启动工作线程：
```java
private boolean addWorker(Runnable firstTask, boolean core) {  
    retry:  
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
            if (wc >= CAPACITY ||  
                wc >= (core ? corePoolSize : maximumPoolSize))  
                return false;  
            if (compareAndIncrementWorkerCount(c))  
                break retry;  
            c = ctl.get();  // Re-read ctl  
            if (runStateOf(c) != rs)  
                continue retry;  
            // else CAS failed due to workerCount change; retry inner loop  
        }  
    }  
  
    boolean workerStarted = false;  
    boolean workerAdded = false;  
    Worker w = null;  
    try {  
        // 创建工作线程
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

启动工作线程后，会执行`ThreadPoolExecutor.Worker#run()`方法：
```java
public void run() {  
    runWorker(this);  
}
```

实际线程执行逻辑位于`ThreadPoolExecutor#runWorker()`，会循环执行提交的`ScheduledFutureTask`任务：
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

## 2.4 ScheduledFutureTask
所有提交的`Runnable`和`Callable`任务，在内部都会被封装成`ScheduledFutureTask`对象。它继承了`FutureTask`，可以用来适配`Callable`任务。
![[ScheduledFutureTask.png]]

![[ScheduledFutureTask2.png]]
`ScheduledFutureTask`核心成员变量如下：
- `time`：下次执行时间戳。
- `period`：执行周期（0：non-repeating task；正数：fixed-rate execution；负数：fixed-delay execution）
- `outerTask`：代理的实际任务。

工作线程会执行`ScheduledThreadPoolExecutor.ScheduledFutureTask#run()`方法：
```java
public void run() {  
    // 是否周期执行：period != 0
    boolean periodic = isPeriodic();  
    // 判断线程池状态：能否运行
    if (!canRunInCurrentRunState(periodic))  
        cancel(false);  
    // 非周期任务：执行任务
    else if (!periodic)  
        ScheduledFutureTask.super.run();  
    // 周期任务：执行任务，计算下次执行时间，任务再次入队
    else if (ScheduledFutureTask.super.runAndReset()) {  
        setNextRunTime();  
        reExecutePeriodic(outerTask);  
    }  
}
```

### 2.341 延时任务
如果是延时任务，会执行`java.util.concurrent.FutureTask#run()`方法：
```java
public void run() {  
    if (state != NEW ||  
        !UNSAFE.compareAndSwapObject(this, runnerOffset,  
                                     null, Thread.currentThread()))  
        return;  
    try {  
        Callable<V> c = callable;  
        if (c != null && state == NEW) {  
            V result;  
            boolean ran;  
            try {  
                // 执行任务
                result = c.call();  
                ran = true;  
            } catch (Throwable ex) {  
                result = null;  
                ran = false;  
                setException(ex);  
            }  
            if (ran)  
                set(result);  
        }  
    } finally {  
        // runner must be non-null until state is settled to  
        // prevent concurrent calls to run()        
        runner = null;  
        // state must be re-read after nulling runner to prevent  
        // leaked interrupts        
        int s = state;  
        if (s >= INTERRUPTING)  
            handlePossibleCancellationInterrupt(s);  
    }  
}
```

### 2.4.2 定时任务
如果是定时任务，会执行`FutureTask#runAndReset()`：
```java
protected boolean runAndReset() {  
    // 判断任务状态
    if (state != NEW ||  
        !UNSAFE.compareAndSwapObject(this, runnerOffset,  
                                     null, Thread.currentThread()))  
        return false;  
    
    boolean ran = false;  
    int s = state;  
    try {  
        Callable<V> c = callable;  
        if (c != null && s == NEW) {  
            try {  
                // 执行任务
                c.call(); // don't set result  
                ran = true;  
            } catch (Throwable ex) {  
                setException(ex);  
            }  
        }  
    } finally {  
        // runner must be non-null until state is settled to  
        // prevent concurrent calls to run()        
        runner = null;  
        // state must be re-read after nulling runner to prevent  
        // leaked interrupts        
        s = state;  
        if (s >= INTERRUPTING)  
            handlePossibleCancellationInterrupt(s);  
    }  
    return ran && s == NEW;  
}
```

然后，计算下次执行时间`ScheduledThreadPoolExecutor.ScheduledFutureTask#setNextRunTime()`：
```java
private void setNextRunTime() {  
    long p = period;  
    if (p > 0)  
        time += p;  
    else  
        time = triggerTime(-p);  
}
```

最后，将任务重新入队，等待下次执行。`ScheduledThreadPoolExecutor#reExecutePeriodic()`：
```java
void reExecutePeriodic(RunnableScheduledFuture<?> task) {  
    if (canRunInCurrentRunState(true)) {  
        // 任务重新入队
        super.getQueue().add(task);  
        if (!canRunInCurrentRunState(true) && remove(task))  
            task.cancel(false);  
        else  
            ensurePrestart();  
    }  
}
```

## 2.5 延迟任务队列
`ScheduledThreadPoolExecutor`使用的任务队列是`DelayedWorkQueue`，它是基于堆的数据结构，是执行延时/定时任务的核心，主要用到了队列的`add/pool/take()`方法，以及任务的`compareTo()`方法。

### 2.5.1 compareTo
`ScheduledThreadPoolExecutor.ScheduledFutureTask#compareTo()`用于队列堆中的任务排序，主要是根据下次执行时间戳`time`进行从小到大排序：
```java
public int compareTo(Delayed other) {  
    if (other == this) // compare zero if same object  
        return 0;  
    if (other instanceof ScheduledFutureTask) {  
        ScheduledFutureTask<?> x = (ScheduledFutureTask<?>)other;  
        long diff = time - x.time;  
        if (diff < 0)  
            return -1;  
        else if (diff > 0)  
            return 1;  
        else if (sequenceNumber < x.sequenceNumber)  
            return -1;  
        else  
            return 1;  
    }  
    long diff = getDelay(NANOSECONDS) - other.getDelay(NANOSECONDS);  
    return (diff < 0) ? -1 : (diff > 0) ? 1 : 0;  
}
```

### 2.5.2 add
`ScheduledThreadPoolExecutor.DelayedWorkQueue#add()`用于添加任务：
```java
public boolean add(Runnable e) {  
    return offer(e);  
}
```

实际逻辑位于`ScheduledThreadPoolExecutor.DelayedWorkQueue#offer()`：
```java
public boolean offer(Runnable x) {  
    if (x == null) throw new NullPointerException();  
    RunnableScheduledFuture<?> e = (RunnableScheduledFuture<?>)x;  
    final ReentrantLock lock = this.lock;  
    lock.lock();  
    try {  
        int i = size;  
        // 数组扩容
        if (i >= queue.length)  
            grow();  
        size = i + 1;  
        // 第一个节点，直接添加
        if (i == 0) {  
            queue[0] = e;  
            setIndex(e, 0);  
        // 子节点，根据下一次执行时间戳进行堆排序
        } else {  
            siftUp(i, e);  
        }  
        if (queue[0] == e) {  
            leader = null;  
            available.signal();  
        }  
    } finally {  
        lock.unlock();  
    }  
    return true;  
}
```

### 2.5.3 pool
`ScheduledThreadPoolExecutor.DelayedWorkQueue#poll()`方法用于获取任务，该方法会阻塞`timeout`时间，如果超时会返回空：
```java
public RunnableScheduledFuture<?> poll(long timeout, TimeUnit unit)  
    throws InterruptedException {  
    long nanos = unit.toNanos(timeout);  
    final ReentrantLock lock = this.lock;  
    lock.lockInterruptibly();  
    try {  
        for (;;) {  
            // 获取第一个任务
            RunnableScheduledFuture<?> first = queue[0];  
            // 如果任务为空，阻塞线程nanos
            if (first == null) {  
                if (nanos <= 0)  
                    return null;  
                else  
                    nanos = available.awaitNanos(nanos);  
            } 
            // 如果任务不为空，判断延时
            else {  
                // 获取延时时间：当前时间-下次执行时间
                long delay = first.getDelay(NANOSECONDS);  
                // 任务需要执行：获取任务，堆排序
                if (delay <= 0)  
                    return finishPoll(first);  
                // 任务不需要执行：返回null或阻塞线程
                if (nanos <= 0)  
                    return null;  
                first = null; // don't retain ref while waiting  
                if (nanos < delay || leader != null)  
                    nanos = available.awaitNanos(nanos);  
                else {  
                    Thread thisThread = Thread.currentThread();  
                    leader = thisThread;  
                    try {  
                        long timeLeft = available.awaitNanos(delay);  
                        nanos -= delay - timeLeft;  
                    } finally {  
                        if (leader == thisThread)  
                            leader = null;  
                    }  
                }  
            }  
        }  
    } finally {  
        if (leader == null && queue[0] != null)  
            available.signal();  
        lock.unlock();  
    }  
}
```

### 2.5.4 take
`ScheduledThreadPoolExecutor.DelayedWorkQueue#take()`方法用于获取任务，该方法会阻塞线程：
```java
public RunnableScheduledFuture<?> take() throws InterruptedException {  
    final ReentrantLock lock = this.lock;  
    lock.lockInterruptibly();  
    try {  
        for (;;) {  
            // 获取第一个任务
            RunnableScheduledFuture<?> first = queue[0];  
            // 如果任务为空，阻塞线程
            if (first == null)  
                available.await();  
            // 如果任务不为空，判断延时
            else {  
                // 获取延时时间：当前时间-下次执行时间
                long delay = first.getDelay(NANOSECONDS);  
                // 任务需要执行：获取任务，堆排序
                if (delay <= 0)  
                    return finishPoll(first);  
                // 任务不需要执行：阻塞线程
                first = null; // don't retain ref while waiting  
                if (leader != null)  
                    available.await();  
                else {  
                    Thread thisThread = Thread.currentThread();  
                    leader = thisThread;  
                    try {  
                        available.awaitNanos(delay);  
                    } finally {  
                        if (leader == thisThread)  
                            leader = null;  
                    }  
                }  
            }  
        }  
    } finally {  
        if (leader == null && queue[0] != null)  
            available.signal();  
        lock.unlock();  
    }  
}
```