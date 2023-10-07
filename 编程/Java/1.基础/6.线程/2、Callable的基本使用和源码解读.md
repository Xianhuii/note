# 1 使用
`java.util.concurrent.Callable`是有返回值的多线程任务：
```java
public interface Callable<V> {  
    V call() throws Exception;  
}
```

通过实现`Callable`接口，在`call()`方法中定义业务逻辑，并返回处理结果。例如：
```java
Callable<Integer> task = new Callable() {  
    @Override  
    public Integer call() throws Exception {  
        // 业务处理  
        return 100;  
    }  
};
```

在Java中，启动线程的唯一方式是`Thread`类，它可以将`Runnable`对象作为线程执行的任务。但是，我们发现Callable跟Runnable并没有关系。

为了执行Callable任务，我们需要使用`FutureTask`作为适配器，将任务包装起来。

FutureTask是Runnable的实现类，其中定义了执行Callable任务的模板方法。

当任务执行完成之后，可以使用FutureTask#get()方法获取处理结果。

因此，我们使用以下方式执行Callable任务：
```java
FutureTask futureTask = new FutureTask(task);  
Thread thread = new Thread(futureTask);  
thread.start();  
try {  
    System.out.println(futureTask.get());  
} catch (InterruptedException e) {  
    e.printStackTrace();  
} catch (ExecutionException e) {  
    e.printStackTrace();  
}
```

# 2 源码
![[Callable.png]]

在创建`FutureTask`对象时，会保存`callable`任务：
```java
public FutureTask(Callable<V> callable) {  
    if (callable == null)  
        throw new NullPointerException();  
    this.callable = callable;  
    this.state = NEW;       // ensure visibility of callable  
}
```

FutureTask实现了Runnable接口，在线程运行时，会执行其`run()`方法：
```java
public void run() {  
    if (state != NEW ||  
        !UNSAFE.compareAndSwapObject(this, runnerOffset,  
                                     null, Thread.currentThread()))  
        return;  
    try {  
        // 执行callable任务
        Callable<V> c = callable;  
        if (c != null && state == NEW) {  
            V result;  
            boolean ran;  
            try {  
                result = c.call();  
                ran = true;  
            } catch (Throwable ex) {  
                result = null;  
                ran = false;  
                setException(ex);  
            }  
            // 将处理结果设置到成员变量outcome
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

通过`java.util.concurrent.FutureTask#get()`方法，可以获取到执行结果：
```java
public V get() throws InterruptedException, ExecutionException {  
    int s = state;  
    // 如果任务未完成，等待任务完成
    if (s <= COMPLETING)  
        s = awaitDone(false, 0L);  
    // 返回处理结果：outcome
    return report(s);  
}
```

`java.util.concurrent.FutureTask#awaitDone()`方法会阻塞当前线程，直到业务处理完成：
```java
private int awaitDone(boolean timed, long nanos)  
    throws InterruptedException {  
    final long deadline = timed ? System.nanoTime() + nanos : 0L;  
    WaitNode q = null;  
    boolean queued = false;  
    // 死循环
    for (;;) {  
        if (Thread.interrupted()) {  
            removeWaiter(q);  
            throw new InterruptedException();  
        }  
  
        int s = state;  
        // 如果任务执行完成，返回
        if (s > COMPLETING) {  
            if (q != null)  
                q.thread = null;  
            return s;  
        }  
        // 如果任务执行马上完成，让出CPU
        else if (s == COMPLETING) // cannot time out yet  
            Thread.yield();  
        // 循环等待，或阻塞当前线程
        else if (q == null)  
            q = new WaitNode();  
        else if (!queued)  
            queued = UNSAFE.compareAndSwapObject(this, waitersOffset,  
                                                 q.next = waiters, q);  
        else if (timed) {  
            nanos = deadline - System.nanoTime();  
            if (nanos <= 0L) {  
                removeWaiter(q);  
                return state;  
            }  
            LockSupport.parkNanos(this, nanos);  
        }  
        else  
            LockSupport.park(this);  
    }  
}
```