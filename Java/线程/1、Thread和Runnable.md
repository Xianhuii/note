# 1 创建线程
## 1.1 两种创建方法
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

## 1.2 异同点
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

从它们的执行逻辑来看，Thread代表的是`线程`，而Runnable代表的是`任务`。

由于线程只能启动一次（多次启动会抛异常），而任务可以被多个线程执行。因此，更加推荐使用实现Runnable接口的方式创建线程。

例如：
```java
Runnable task = new Runnable() {  
    @Override  
    public void run() {  
        System.out.println("run()");  
    }  
};  
Thread thread1 = new Thread(task);  
thread1.start();  
Thread thread2 = new Thread(task);  
thread2.start();
```

# 2 Thread核心源码
## 2.1 成员变量
![[Thread.png]]

核心成员变量如下：
- `name`：线程名
- `group`：归属的线程组
- `daemon`：是否是守护线程
- `priority`：优先级
- `contextClassLoader`：上下文类加载器
- `inheritedAccessControlContext`：继承的访问控制上下文，用于检查对系统资源的访问权限
- `target`：目标任务
- `inheritableThreadLocals`：存储线程本地变量
- `stackSize`：当前线程的栈大小
- `tid`：线程id
- `threadStatus`：线程状态，默认为`0`，映射为`State.NEW`。

## 2.2 线程状态
Java中的线程包括以下状态：
- `NEW`：线程对象还没有执行`start()`方法。
- `RUNNABLE`：线程正在虚拟机中执行。
- `BLOCKED`：线程由于在等待锁而处于阻塞状态。
- `WAITING`：线程无限期地等待其他线程执行特定操作（`notify()`等）。
- `TIMED_WAITING`：线程有限期地等待其他线程执行特定操作（`notify()`等）。
- `TERMINATED`：线程已经终止退出。

`java.lang.Thread.State`：
```java
public enum State {  
    /**  
     * Thread state for a thread which has not yet started.     
     */    
    NEW,  
  
    /**  
     * Thread state for a runnable thread.  A thread in the runnable     
     * state is executing in the Java virtual machine but it may     
     * be waiting for other resources from the operating system     
     * such as processor.     
     */    
    RUNNABLE,  
  
    /**  
     * Thread state for a thread blocked waiting for a monitor lock.     
     * A thread in the blocked state is waiting for a monitor lock     
     * to enter a synchronized block/method or     
     * reenter a synchronized block/method after calling     
     * {@link Object#wait() Object.wait}.  
     */    
    BLOCKED,  
  
    /**  
     * Thread state for a waiting thread.     
     * A thread is in the waiting state due to calling one of the     
     * following methods:     
     * <ul>  
     *   <li>{@link Object#wait() Object.wait} with no timeout</li>  
     *   <li>{@link #join() Thread.join} with no timeout</li>  
     *   <li>{@link LockSupport#park() LockSupport.park}</li>  
     * </ul>  
     *     
     * <p>A thread in the waiting state is waiting for another thread to  
     * perform a particular action.     
     *     
     * For example, a thread that has called <tt>Object.wait()</tt>  
     * on an object is waiting for another thread to call     
     * <tt>Object.notify()</tt> or <tt>Object.notifyAll()</tt> on     
     * that object. A thread that has called <tt>Thread.join()</tt>  
     * is waiting for a specified thread to terminate.     
     */    
    WAITING,  
  
    /**  
     * Thread state for a waiting thread with a specified waiting time.     
     * A thread is in the timed waiting state due to calling one of     
     * the following methods with a specified positive waiting time:     
     * <ul>  
     *   <li>{@link #sleep Thread.sleep}</li>  
     *   <li>{@link Object#wait(long) Object.wait} with timeout</li>  
     *   <li>{@link #join(long) Thread.join} with timeout</li>  
     *   <li>{@link LockSupport#parkNanos LockSupport.parkNanos}</li>  
     *   <li>{@link LockSupport#parkUntil LockSupport.parkUntil}</li>  
     * </ul>  
     */    
    TIMED_WAITING,  
  
    /**  
     * Thread state for a terminated thread.     
     * The thread has completed execution.     
     */    
    TERMINATED;  
}
```

获取当前线程的状态`java.lang.Thread#getState()`：
```java
public State getState() {  
    // get current thread state  
    return sun.misc.VM.toThreadState(threadStatus);  
}
```

Java会将操作系统的线程状态映射到统一的`State`中，例如`sun.misc.VM#toThreadState()`：
```java
public static State toThreadState(int var0) {  
    if ((var0 & 4) != 0) {  
        return State.RUNNABLE;  
    } else if ((var0 & 1024) != 0) {  
        return State.BLOCKED;  
    } else if ((var0 & 16) != 0) {  
        return State.WAITING;  
    } else if ((var0 & 32) != 0) {  
        return State.TIMED_WAITING;  
    } else if ((var0 & 2) != 0) {  
        return State.TERMINATED;  
    } else {  
        return (var0 & 1) == 0 ? State.NEW : State.RUNNABLE;  
    }  
}
```

## 2.3 静态方法
Thread在类加载时会执行`registerNatives()`方法，用来注册本地方法：
```java
private static native void registerNatives();  
static {  
    registerNatives();  
}
```

在`Thread.c`中，定义了需要注册的本地方法：
```c
// 本地方法数组
static JNINativeMethod methods[] = {  
    {"start0",           "()V",        (void *)&JVM_StartThread},  
    {"stop0",            "(" OBJ ")V", (void *)&JVM_StopThread},  
    {"isAlive",          "()Z",        (void *)&JVM_IsThreadAlive},  
    {"suspend0",         "()V",        (void *)&JVM_SuspendThread},  
    {"resume0",          "()V",        (void *)&JVM_ResumeThread},  
    {"setPriority0",     "(I)V",       (void *)&JVM_SetThreadPriority},  
    {"yield",            "()V",        (void *)&JVM_Yield},  
    {"sleep",            "(J)V",       (void *)&JVM_Sleep},  
    {"currentThread",    "()" THD,     (void *)&JVM_CurrentThread},  
    {"countStackFrames", "()I",        (void *)&JVM_CountStackFrames},  
    {"interrupt0",       "()V",        (void *)&JVM_Interrupt},  
    {"isInterrupted",    "(Z)Z",       (void *)&JVM_IsInterrupted},  
    {"holdsLock",        "(" OBJ ")Z", (void *)&JVM_HoldsLock},  
    {"getThreads",        "()[" THD,   (void *)&JVM_GetAllThreads},  
    {"dumpThreads",      "([" THD ")[[" STE, (void *)&JVM_DumpThreads},  
    {"setNativeName",    "(" STR ")V", (void *)&JVM_SetNativeThreadName},  
};  

// 注册本地方法
JNIEXPORT void JNICALL  
Java_java_lang_Thread_registerNatives(JNIEnv *env, jclass cls)  
{  
    (*env)->RegisterNatives(env, cls, methods, ARRAY_LENGTH(methods));  
}
```

## 2.4 初始化方法
我们可以通过构造函数创建线程对象，常用的两个构造函数如下：
```java
public Thread() {  
    init(null, null, "Thread-" + nextThreadNum(), 0);  
}
public Thread(Runnable target) {  
    init(null, target, "Thread-" + nextThreadNum(), 0);  
}
```

可以看到，实际初始化方法是`java.lang.Thread#init()`，它会对核心成员变量进行初始化：
```java
private void init(ThreadGroup g, Runnable target, String name,  
                  long stackSize, AccessControlContext acc,  
                  boolean inheritThreadLocals) {  
    // 设置线程名
    if (name == null) {  
        throw new NullPointerException("name cannot be null");  
    }  
    this.name = name;  
  
    Thread parent = currentThread();  
    SecurityManager security = System.getSecurityManager();  
    if (g == null) {  
        /* Determine if it's an applet or not */  
  
        /* If there is a security manager, ask the security manager           
        what to do. */        
        if (security != null) {  
            g = security.getThreadGroup();  
        }  
  
        /* If the security doesn't have a strong opinion of the matter  
           use the parent thread group. */        
           if (g == null) {  
            g = parent.getThreadGroup();  
        }  
    }  
  
    /* checkAccess regardless of whether or not threadgroup is  
       explicitly passed in. */    
    g.checkAccess();  
    
    /*  
     * Do we have the required permissions?     
     */    
    if (security != null) {  
        if (isCCLOverridden(getClass())) {  
            security.checkPermission(SUBCLASS_IMPLEMENTATION_PERMISSION);  
        }  
    }  
  
    g.addUnstarted();  
  
    this.group = g;  
    this.daemon = parent.isDaemon();  
    this.priority = parent.getPriority();  
    if (security == null || isCCLOverridden(parent.getClass()))  
        this.contextClassLoader = parent.getContextClassLoader();  
    else  
        this.contextClassLoader = parent.contextClassLoader;  
    this.inheritedAccessControlContext =  
            acc != null ? acc : AccessController.getContext();  
    this.target = target;  
    setPriority(priority);  
    if (inheritThreadLocals && parent.inheritableThreadLocals != null)  
        this.inheritableThreadLocals =  
            ThreadLocal.createInheritedMap(parent.inheritableThreadLocals);  
    /* Stash the specified stack size in case the VM cares */  
    this.stackSize = stackSize;  
  
    /* Set thread ID */  
    tid = nextThreadID();  
}
```

线程对象初始化后，它的`threadStatus=0`，处于`State.NEW`状态。

## 2.5 启动线程
通过`java.lang.Thread#start()`方法启动线程：
```java
public synchronized void start() {  
    /**  
     * A zero status value corresponds to state "NEW".     
     */    
    if (threadStatus != 0)  
        throw new IllegalThreadStateException();  
  
    /* Notify the group that this thread is about to be started  
     * so that it can be added to the group's list of threads     
     * and the group's unstarted count can be decremented. 
     */    
    group.add(this);  
  
    boolean started = false;  
    try {  
        start0();  
        started = true;  
    } finally {  
        try {  
            if (!started) {  
                group.threadStartFailed(this);  
            }  
        } catch (Throwable ignore) {  
            /* do nothing. If start0 threw a Throwable then  
              it will be passed up the call stack */        
        }  
    }  
}
```

启动线程时会校验当前线程的状态，结合`synchronized`关键字，保证每个线程最多只能启动一次。

实际启动线程的方法位于`java.lang.Thread#start0()`，它是一个本地方法。因此会执行之前注册的本地方法：
```c
{"start0",           "()V",        (void *)&JVM_StartThread}
```

`JVM_StartThread`是执行虚拟机的内部方法：
```cpp
JVM_ENTRY(void, JVM_StartThread(JNIEnv* env, jobject jthread))  
  JVMWrapper("JVM_StartThread");  
  JavaThread *native_thread = NULL;  
  
  // 是否抛出异常标识
  // We cannot hold the Threads_lock when we throw an exception,  
  // due to rank ordering issues. Example:  we might need to grab the  
  // Heap_lock while we construct the exception.  
  bool throw_illegal_thread_state = false;  
  
  // We must release the Threads_lock before we can post a jvmti event  
  // in Thread::start.  
  {  
    // Ensure that the C++ Thread and OSThread structures aren't freed before  
    // we operate.    
    MutexLocker mu(Threads_lock);  
  
    // Since JDK 5 the java.lang.Thread threadStatus is used to prevent  
    // re-starting an already started thread, so we should usually find    
    // that the JavaThread is null. However for a JNI attached thread    
    // there is a small window between the Thread object being created    
    // (with its JavaThread set) and the update to its threadStatus, so we    
    // have to check for this    
    if (java_lang_Thread::thread(JNIHandles::resolve_non_null(jthread)) != NULL) {  
      // 当前线程已启动，设置抛异常标识
      throw_illegal_thread_state = true;  
    } else {  
      // We could also check the stillborn flag to see if this thread was already stopped, but  
      // for historical reasons we let the thread detect that itself when it starts running  
      jlong size =  
             java_lang_Thread::stackSize(JNIHandles::resolve_non_null(jthread));  
      // Allocate the C++ Thread structure and create the native thread.  The  
      // stack size retrieved from java is signed, but the constructor takes      
      // size_t (an unsigned type), so avoid passing negative values which would      
      // result in really large stacks.      
      // 创建线程
      size_t sz = size > 0 ? (size_t) size : 0;  
      native_thread = new JavaThread(&thread_entry, sz);  
  
      // At this point it may be possible that no osthread was created for the  
      // JavaThread due to lack of memory. Check for this situation and throw      
      // an exception if necessary. Eventually we may want to change this so      
      // that we only grab the lock if the thread was created successfully -      
      // then we can also do this check and throw the exception in the      
      // JavaThread constructor.      
      if (native_thread->osthread() != NULL) {  
        // Note: the current thread is not being used within "prepare".  
        native_thread->prepare(jthread);  
      }  
    }  
  }  
  
  if (throw_illegal_thread_state) {  
    THROW(vmSymbols::java_lang_IllegalThreadStateException());  
  }  
  
  assert(native_thread != NULL, "Starting null thread?");  
  
  if (native_thread->osthread() == NULL) {  
    // No one should hold a reference to the 'native_thread'.  
    delete native_thread;  
    if (JvmtiExport::should_post_resource_exhausted()) {  
      JvmtiExport::post_resource_exhausted(  
        JVMTI_RESOURCE_EXHAUSTED_OOM_ERROR | JVMTI_RESOURCE_EXHAUSTED_THREADS,  
        "unable to create new native thread");  
    }  
    THROW_MSG(vmSymbols::java_lang_OutOfMemoryError(),  
              "unable to create new native thread");  
  }  
  
#if INCLUDE_JFR  
  if (JfrRecorder::is_recording() && EventThreadStart::is_enabled() &&  
      EventThreadStart::is_stacktrace_enabled()) {  
    JfrThreadLocal* tl = native_thread->jfr_thread_local();  
    // skip Thread.start() and Thread.start0()  
    tl->set_cached_stack_trace_id(JfrStackTraceRepository::record(thread, 2));  
  }  
#endif  
  
  // 启动线程
  Thread::start(native_thread);  
  
JVM_END
```

线程对象启动后，它会处于`State.RUNNABLE`状态。

## 2.6 阻塞线程
### 2.6.1 获取锁失败
如果当前线程在执行时间片中尝试获取锁，但是发现锁被其他线程持有了，那么当前线程就会放弃执行，进入阻塞状态。等到下次执行时，再重新尝试获取锁。

例如，我们定义一个任务：
```java
static class MyThread implements Runnable {  
    private volatile int index = 0;  
  
    @Override  
    public void run() {  
        print();  
    }  
  
    private synchronized void print() {  
        for (; index < 3; index++) {  
            if (index == 2) {  
                System.exit(-1);  
            }  
            System.out.println(Thread.currentThread().getName() + ":" + index);  
        }  
    }  
}
```

使用两个线程去执行：
```java
Runnable task = new MyThread();  
Thread thread1 = new Thread(task, "thread1");  
Thread thread2 = new Thread(task, "thread2");  
System.out.println("before start(), thread1:" + thread1.getState() + ", thread2:" + thread2.getState());  
thread1.start();  
thread2.start();  
while (!MyThread.exit) {  
    System.out.println("in while(), thread1:" + thread1.getState() + ", thread2:" + thread2.getState());  
}  
for (int i = 0; i < 3; i++) {  
    System.out.println("in for(), thread1:" + thread1.getState() + ", thread2:" + thread2.getState());  
}
```

输出结果可能如下：
```
before start(), thread1:NEW, thread2:NEW
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:BLOCKED
in while(), thread1:RUNNABLE, thread2:BLOCKED
thread1:RUNNABLE:0
in while(), thread1:RUNNABLE, thread2:BLOCKED
in while(), thread1:BLOCKED, thread2:BLOCKED
thread1:RUNNABLE:1
in while(), thread1:BLOCKED, thread2:BLOCKED
thread1:RUNNABLE:2
in for(), thread1:RUNNABLE, thread2:BLOCKED
in for(), thread1:TERMINATED, thread2:TERMINATED
in for(), thread1:TERMINATED, thread2:TERMINATED
```

在启动线程前，线程状态都是`NEW`。
执行任务前，线程状态都是`RUNNABLE`。
由于thread1抢到了锁，所以thread2在试图获取锁时失败，进入`BLOCKED`状态。
任务执行完，线程状态都变成了`TERMINATED`。
需要注意的是，尽管thread1全程持有锁，但是它在某些时候也会处理`BLOCKED`状态（不知道为什么，可能是时间片用完后会进入阻塞状态）。

### 2.6.2 从WAITING或TIMED_WAITING状态苏醒
当线程从`WAITING`状态苏醒后，会先进入到`BLOCKED`状态。例如，我们修改上述任务逻辑：
```java
for (; index < 3; index++) {  
    if (index == 2) {  
        exit = true;  
    }  
    System.out.println(Thread.currentThread().getName() + ":" + Thread.currentThread().getState() + ":" + index);  
    if (index == 0) {  
        index++;  
        try {  
            this.wait();  
            System.out.println(Thread.currentThread().getName() + ":after wait():" + Thread.currentThread().getState() + ":" + index);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }  
    }  
    if (index == 1) {  
        this.notifyAll();  
    }  
}
```

执行的可能结果如下：
```
before start(), thread1:NEW, thread2:NEW
in while(), thread1:RUNNABLE, thread2:RUNNABLE
in while(), thread1:RUNNABLE, thread2:BLOCKED
thread1:RUNNABLE:0
in while(), thread1:RUNNABLE, thread2:BLOCKED
thread2:RUNNABLE:1
in while(), thread1:WAITING, thread2:RUNNABLE
thread2:RUNNABLE:2
in for(), thread1:BLOCKED, thread2:RUNNABLE
thread1:after wait():RUNNABLE:3
in for(), thread1:BLOCKED, thread2:TERMINATED
in for(), thread1:TERMINATED, thread2:TERMINATED
```

在启动线程前，线程状态都是`NEW`。
执行任务前，线程状态都是`RUNNABLE`。
由于thread1抢到了锁，所以thread2在试图获取锁时失败，进入`BLOCKED`状态。
thread1打印后，调用`Object#wait()`后释放锁，并进入`WAITING`状态。
thread2获取锁，并执行。
thread2通知thread1从`WAITING`状态苏醒，thread1进入`BLOCKED`状态。
thread2执行完，释放锁。
thread1获取锁，从`Object#wait()`方法开始执行。
任务执行完，线程状态都变成了`TERMINATED`。

需要注意的是，`Thread.sleep()`方法并不会释放锁。如果我们将任务逻辑修改如下，又会执行完全不同的逻辑：
```java
for (; index < 3; index++) {  
    if (index == 2) {  
        exit = true;  
    }  
    System.out.println(Thread.currentThread().getName() + ":" + Thread.currentThread().getState() + ":" + index);  
    if (index == 0) {  
        index++;  
        try {  
            Thread.sleep(1);  
            System.out.println(Thread.currentThread().getName() + ":after sleep():" + Thread.currentThread().getState() + ":" + index);  
        } catch (InterruptedException e) {  
            e.printStackTrace();  
        }  
    }  
    if (index == 1) {  
        this.notifyAll();  
    }  
}
```

执行的可能结果如下：
```
before start(), thread1:NEW, thread2:NEW
in while(), thread1:RUNNABLE, thread2:RUNNABLE
thread1:RUNNABLE:0
in while(), thread1:RUNNABLE, thread2:BLOCKED
in while(), thread1:TIMED_WAITING, thread2:BLOCKED
in while(), thread1:BLOCKED, thread2:BLOCKED
in while(), thread1:BLOCKED, thread2:BLOCKED
in while(), thread1:BLOCKED, thread2:BLOCKED
thread1:after sleep():RUNNABLE:1
thread1:RUNNABLE:2
in while(), thread1:BLOCKED, thread2:BLOCKED
in for(), thread1:TERMINATED, thread2:TERMINATED
in for(), thread1:TERMINATED, thread2:TERMINATED
in for(), thread1:TERMINATED, thread2:TERMINATED
```

在启动线程前，线程状态都是`NEW`。
执行任务前，线程状态都是`RUNNABLE`。
由于thread1抢到了锁，所以thread2在试图获取锁时失败，进入`BLOCKED`状态。
thread1打印后，调用`Thread#sleep()`后不会释放锁，并进入`TIMED_WAITING`状态。
thread2获取始终获取不到锁，处于`BLOCKED`状态。
thread1从`TIMED_WAITING`状态苏醒，进入`BLOCKED`状态。
thread1获取执行时间片，进入`RUNNABLE`状态，从`Thread#sleep()`方法开始执行。
任务执行完，线程状态都变成了`TERMINATED`。

## 2.7 （时间）等待线程
使用`Object#wait()`、`Thread#join()`和`LockSupport#park()`方法，可以让线程进入`WAITING`状态。

使用`Thread#sleep()`、`Object#wait(long)`、`Thread#join(long)`、`LockSupport#parkNanos()`和`LockSupport#parkUntil()`方法，可以让线程进入`TIMED_WAITING`状态。

### 2.7.1 Object的方法
`Object#wait()`方法有两个要求：
1. 当前对象是锁对象。
2. `Object#wait()`在同步代码块中执行。

使用`Object#wait()`方法，可以让当前线程进入`WAITING`状态，并释放锁：
```java
public final void wait() throws InterruptedException {  
    wait(0);  
}
```

使用`Object#wait(long)`和`Object#wait(long,int)`方法，可以让当前线程进入`TIMED_WAITING`状态，并释放锁。

它们底层是同一个方法：
```java
public final native void wait(long timeout) throws InterruptedException;
```

在`Object.c`中可以查看对应的虚拟机方法：
```c
static JNINativeMethod methods[] = {  
    {"hashCode",    "()I",                    (void *)&JVM_IHashCode},  
    {"wait",        "(J)V",                   (void *)&JVM_MonitorWait},  
    {"notify",      "()V",                    (void *)&JVM_MonitorNotify},  
    {"notifyAll",   "()V",                    (void *)&JVM_MonitorNotifyAll},  
    {"clone",       "()Ljava/lang/Object;",   (void *)&JVM_Clone},  
};
```

`JVM_MonitorWait`方法如下：
```cpp
JVM_ENTRY(void, JVM_MonitorWait(JNIEnv* env, jobject handle, jlong ms))
  JVMWrapper("JVM_MonitorWait");
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));
  JavaThreadInObjectWaitState jtiows(thread, ms != 0);
  if (JvmtiExport::should_post_monitor_wait()) {
    JvmtiExport::post_monitor_wait((JavaThread *)THREAD, (oop)obj(), ms);
    // The current thread already owns the monitor and it has not yet
    // been added to the wait queue so the current thread cannot be
    // made the successor. This means that the JVMTI_EVENT_MONITOR_WAIT
    // event handler cannot accidentally consume an unpark() meant for
    // the ParkEvent associated with this ObjectMonitor.
  }
  // 调用ObjectSynchronizer的wait()方法
  ObjectSynchronizer::wait(obj, ms, CHECK);

JVM_END
```

`ObjectSynchronizer::wait()`方法如下：
```cpp
void ObjectSynchronizer::wait(Handle obj, jlong millis, TRAPS) {
  if (UseBiasedLocking) {
    BiasedLocking::revoke_and_rebias(obj, false, THREAD);
    assert(!obj->mark()->has_bias_pattern(), "biases should be revoked by now");
  }

  if (millis < 0) {
    TEVENT (wait - throw IAX) ;
    THROW_MSG(vmSymbols::java_lang_IllegalArgumentException(), "timeout value is negative");
  }
  ObjectMonitor* monitor = ObjectSynchronizer::inflate(THREAD,
                                                       obj(),
                                                       inflate_cause_wait);
  DTRACE_MONITOR_WAIT_PROBE(monitor, obj(), THREAD, millis);
  // 调用监视器的wait()方法
  monitor->wait(millis, true, THREAD);
  /* This dummy call is in place to get around dtrace bug 6254741.  Once
     that's fixed we can uncomment the following line and remove the call */
  // DTRACE_MONITOR_PROBE(waited, monitor, obj(), THREAD);
  dtrace_waited_probe(monitor, obj, THREAD);

}
```

`ObjectMonitor::wait()`方法如下：
```cpp
void ObjectMonitor::wait(jlong millis, bool interruptible, TRAPS) {  
   Thread * const Self = THREAD ;  
   assert(Self->is_Java_thread(), "Must be Java thread!");  
   JavaThread *jt = (JavaThread *)THREAD;  
  
   DeferredInitialize () ;  
  
   // Throw IMSX or IEX.  
   CHECK_OWNER();  
  
   EventJavaMonitorWait event;  
  
   // check for a pending interrupt  
   if (interruptible && Thread::is_interrupted(Self, true) && !HAS_PENDING_EXCEPTION) {  
     // post monitor waited event.  Note that this is past-tense, we are done waiting.  
     if (JvmtiExport::should_post_monitor_waited()) {  
        // Note: 'false' parameter is passed here because the  
        // wait was not timed out due to thread interrupt.        
        JvmtiExport::post_monitor_waited(jt, this, false);  
  
        // In this short circuit of the monitor wait protocol, the  
        // current thread never drops ownership of the monitor and        
        // never gets added to the wait queue so the current thread        
        // cannot be made the successor. This means that the        
        // JVMTI_EVENT_MONITOR_WAITED event handler cannot accidentally        
        // consume an unpark() meant for the ParkEvent associated with        
        // this ObjectMonitor.     
        }  
     if (event.should_commit()) {  
       post_monitor_wait_event(&event, this, 0, millis, false);  
     }  
     TEVENT (Wait - Throw IEX) ;  
     THROW(vmSymbols::java_lang_InterruptedException());  
     return ;  
   }  
  
   TEVENT (Wait) ;  
  
   assert (Self->_Stalled == 0, "invariant") ;  
   Self->_Stalled = intptr_t(this) ;  
   jt->set_current_waiting_monitor(this);  
  
   // create a node to be put into the queue  
   // Critically, after we reset() the event but prior to park(), we must check   
   // for a pending interrupt.   
   ObjectWaiter node(Self);  
   node.TState = ObjectWaiter::TS_WAIT ;  
   Self->_ParkEvent->reset() ;  
   OrderAccess::fence();          // ST into Event; membar ; LD interrupted-flag  
  
   // Enter the waiting queue, which is a circular doubly linked list in this case   
   // but it could be a priority queue or any data structure.   
   // _WaitSetLock protects the wait queue.  Normally the wait queue is accessed only   
   // by the the owner of the monitor *except* in the case where park()   
   // returns because of a timeout of interrupt.  Contention is exceptionally rare   
   // so we use a simple spin-lock instead of a heavier-weight blocking lock.  
   Thread::SpinAcquire (&_WaitSetLock, "WaitSet - add") ;  
   AddWaiter (&node) ;  
   Thread::SpinRelease (&_WaitSetLock) ;  
  
   if ((SyncFlags & 4) == 0) {  
      _Responsible = NULL ;  
   }  
   intptr_t save = _recursions; // record the old recursion count  
   _waiters++;                  // increment the number of waiters  
   _recursions = 0;             // set the recursion level to be 1  
   exit (true, Self) ;                    // exit the monitor  
   guarantee (_owner != Self, "invariant") ;  
  
   // The thread is on the WaitSet list - now park() it.  
   // On MP systems it's conceivable that a brief spin before we park   
   // could be profitable.   
   //   
   // TODO-FIXME: change the following logic to a loop of the form  
   //   while (!timeout && !interrupted && _notified == 0) park()  
  
   int ret = OS_OK ;  
   int WasNotified = 0 ;  
   { // State transition wrappers  
     OSThread* osthread = Self->osthread();  
     OSThreadWaitState osts(osthread, true);  
     {  
       ThreadBlockInVM tbivm(jt);  
       // Thread is in thread_blocked state and oop access is unsafe.  
       jt->set_suspend_equivalent();  
  
       if (interruptible && (Thread::is_interrupted(THREAD, false) || HAS_PENDING_EXCEPTION)) {  
           // Intentionally empty  
       } else  
       if (node._notified == 0) {  
         if (millis <= 0) {  
            Self->_ParkEvent->park () ;  
         } else {  
            ret = Self->_ParkEvent->park (millis) ;  
         }  
       }  
  
       // were we externally suspended while we were waiting?  
       if (ExitSuspendEquivalent (jt)) {  
          // TODO-FIXME: add -- if succ == Self then succ = null.  
          jt->java_suspend_self();  
       }  
  
     } // Exit thread safepoint: transition _thread_blocked -> _thread_in_vm  
  
  
     // Node may be on the WaitSet, the EntryList (or cxq), or in transition     
     // from the WaitSet to the EntryList.     
     // See if we need to remove Node from the WaitSet.     
     // We use double-checked locking to avoid grabbing _WaitSetLock     
     // if the thread is not on the wait queue.     
     //     
     // Note that we don't need a fence before the fetch of TState.     
     // In the worst case we'll fetch a old-stale value of TS_WAIT previously     
     // written by the is thread. (perhaps the fetch might even be satisfied     
     // by a look-aside into the processor's own store buffer, although given     
     // the length of the code path between the prior ST and this load that's     
     // highly unlikely).  If the following LD fetches a stale TS_WAIT value     
     // then we'll acquire the lock and then re-fetch a fresh TState value.     
     // That is, we fail toward safety.  
     if (node.TState == ObjectWaiter::TS_WAIT) {  
         Thread::SpinAcquire (&_WaitSetLock, "WaitSet - unlink") ;  
         if (node.TState == ObjectWaiter::TS_WAIT) {  
            DequeueSpecificWaiter (&node) ;       // unlink from WaitSet  
            assert(node._notified == 0, "invariant");  
            node.TState = ObjectWaiter::TS_RUN ;  
         }  
         Thread::SpinRelease (&_WaitSetLock) ;  
     }  
  
     // The thread is now either on off-list (TS_RUN),  
     // on the EntryList (TS_ENTER), or on the cxq (TS_CXQ).     
     // The Node's TState variable is stable from the perspective of this thread.     
     // No other threads will asynchronously modify TState.     
     guarantee (node.TState != ObjectWaiter::TS_WAIT, "invariant") ;  
     OrderAccess::loadload() ;  
     if (_succ == Self) _succ = NULL ;  
     WasNotified = node._notified ;  
  
     // Reentry phase -- reacquire the monitor.  
     // re-enter contended monitor after object.wait().     
     // retain OBJECT_WAIT state until re-enter successfully completes     
     // Thread state is thread_in_vm and oop access is again safe,     
     // although the raw address of the object may have changed.     
     // (Don't cache naked oops over safepoints, of course).  
     // post monitor waited event. Note that this is past-tense, we are done waiting.     
     if (JvmtiExport::should_post_monitor_waited()) {  
       JvmtiExport::post_monitor_waited(jt, this, ret == OS_TIMEOUT);  
  
       if (node._notified != 0 && _succ == Self) {  
         // In this part of the monitor wait-notify-reenter protocol it  
         // is possible (and normal) for another thread to do a fastpath         
         // monitor enter-exit while this thread is still trying to get         
         // to the reenter portion of the protocol.         
         //         
         // The ObjectMonitor was notified and the current thread is         
         // the successor which also means that an unpark() has already         
         // been done. The JVMTI_EVENT_MONITOR_WAITED event handler can         
         // consume the unpark() that was done when the successor was         
         // set because the same ParkEvent is shared between Java         
         // monitors and JVM/TI RawMonitors (for now).         
         //         
         // We redo the unpark() to ensure forward progress, i.e., we         
         // don't want all pending threads hanging (parked) with none         
         // entering the unlocked monitor.         
         node._event->unpark();  
       }  
     }  
  
     if (event.should_commit()) {  
       post_monitor_wait_event(&event, this, node._notifier_tid, millis, ret == OS_TIMEOUT);  
     }  
  
     OrderAccess::fence() ;  
  
     assert (Self->_Stalled != 0, "invariant") ;  
     Self->_Stalled = 0 ;  
  
     assert (_owner != Self, "invariant") ;  
     ObjectWaiter::TStates v = node.TState ;  
     if (v == ObjectWaiter::TS_RUN) {  
         enter (Self) ;  
     } else {  
         guarantee (v == ObjectWaiter::TS_ENTER || v == ObjectWaiter::TS_CXQ, "invariant") ;  
         ReenterI (Self, &node) ;  
         node.wait_reenter_end(this);  
     }  
  
     // Self has reacquired the lock.  
     // Lifecycle - the node representing Self must not appear on any queues.     
     // Node is about to go out-of-scope, but even if it were immortal we wouldn't     
     // want residual elements associated with this thread left on any lists.     
     guarantee (node.TState == ObjectWaiter::TS_RUN, "invariant") ;  
     assert    (_owner == Self, "invariant") ;  
     assert    (_succ != Self , "invariant") ;  
   } // OSThreadWaitState()  
  
   jt->set_current_waiting_monitor(NULL);  
  
   guarantee (_recursions == 0, "invariant") ;  
   _recursions = save;     // restore the old recursion count  
   _waiters--;             // decrement the number of waiters  
  
   // Verify a few postconditions   
   assert (_owner == Self       , "invariant") ;  
   assert (_succ  != Self       , "invariant") ;  
   assert (((oop)(object()))->mark() == markOopDesc::encode(this), "invariant") ;  
  
   if (SyncFlags & 32) {  
      OrderAccess::fence() ;  
   }  
  
   // check if the notification happened  
   if (!WasNotified) {  
     // no, it could be timeout or Thread.interrupt() or both  
     // check for interrupt event, otherwise it is timeout     
     if (interruptible && Thread::is_interrupted(Self, true) && !HAS_PENDING_EXCEPTION) {  
       TEVENT (Wait - throw IEX from epilog) ;  
       THROW(vmSymbols::java_lang_InterruptedException());  
     }  
   }  
  
   // NOTE: Spurious wake up will be consider as timeout.  
   // Monitor notify has precedence over thread interrupt.}
```

如果线程A通过调用`Object#wait()`方法进入了`WAITING`状态，那么需要其他线程调用同一个锁对象的`Object#notify()`或`Object#notifyAll()`方法进行唤醒。

`Object#notify()`和`Object#notifyAll()`都是本地方法：
```java
public final native void notify();
public final native void notifyAll();
```

`Object#notify()`对应的虚拟机方法是`JVM_MonitorNotify`：
```cpp
JVM_ENTRY(void, JVM_MonitorNotify(JNIEnv* env, jobject handle))  
  JVMWrapper("JVM_MonitorNotify");  
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));  
  ObjectSynchronizer::notify(obj, CHECK);  
JVM_END
```

`ObjectSynchronizer::notify()`：
```cpp
void ObjectSynchronizer::notify(Handle obj, TRAPS) {
 if (UseBiasedLocking) {
    BiasedLocking::revoke_and_rebias(obj, false, THREAD);
    assert(!obj->mark()->has_bias_pattern(), "biases should be revoked by now");
  }
  markOop mark = obj->mark();
  if (mark->has_locker() && THREAD->is_lock_owned((address)mark->locker())) {
    return;
  }
  ObjectSynchronizer::inflate(THREAD,
                              obj(),
                              inflate_cause_notify)->notify(THREAD);
}
```

`ObjectMonitor::notify()`：
```cpp
void ObjectMonitor::notify(TRAPS) {  
  CHECK_OWNER();  
  if (_WaitSet == NULL) {  
     TEVENT (Empty-Notify) ;  
     return ;  
  }  
  DTRACE_MONITOR_PROBE(notify, this, object(), THREAD);  
  
  int Policy = Knob_MoveNotifyee ;  
  
  Thread::SpinAcquire (&_WaitSetLock, "WaitSet - notify") ;  
  ObjectWaiter * iterator = DequeueWaiter() ;  
  if (iterator != NULL) {  
     TEVENT (Notify1 - Transfer) ;  
     guarantee (iterator->TState == ObjectWaiter::TS_WAIT, "invariant") ;  
     guarantee (iterator->_notified == 0, "invariant") ;  
     if (Policy != 4) {  
        iterator->TState = ObjectWaiter::TS_ENTER ;  
     }  
     iterator->_notified = 1 ;  
     Thread * Self = THREAD;  
     iterator->_notifier_tid = JFR_THREAD_ID(Self);  
  
     ObjectWaiter * List = _EntryList ;  
     if (List != NULL) {  
        assert (List->_prev == NULL, "invariant") ;  
        assert (List->TState == ObjectWaiter::TS_ENTER, "invariant") ;  
        assert (List != iterator, "invariant") ;  
     }  
  
     if (Policy == 0) {       // prepend to EntryList  
         if (List == NULL) {  
             iterator->_next = iterator->_prev = NULL ;  
             _EntryList = iterator ;  
         } else {  
             List->_prev = iterator ;  
             iterator->_next = List ;  
             iterator->_prev = NULL ;  
             _EntryList = iterator ;  
        }  
     } else  
     if (Policy == 1) {      // append to EntryList  
         if (List == NULL) {  
             iterator->_next = iterator->_prev = NULL ;  
             _EntryList = iterator ;  
         } else {  
            // CONSIDER:  finding the tail currently requires a linear-time walk of  
            // the EntryList.  We can make tail access constant-time by converting to            
            // a CDLL instead of using our current DLL.            
            ObjectWaiter * Tail ;  
            for (Tail = List ; Tail->_next != NULL ; Tail = Tail->_next) ;  
            assert (Tail != NULL && Tail->_next == NULL, "invariant") ;  
            Tail->_next = iterator ;  
            iterator->_prev = Tail ;  
            iterator->_next = NULL ;  
        }  
     } else  
     if (Policy == 2) {      // prepend to cxq  
         // prepend to cxq         
         if (List == NULL) {  
             iterator->_next = iterator->_prev = NULL ;  
             _EntryList = iterator ;  
         } else {  
            iterator->TState = ObjectWaiter::TS_CXQ ;  
            for (;;) {  
                ObjectWaiter * Front = _cxq ;  
                iterator->_next = Front ;  
                if (Atomic::cmpxchg_ptr (iterator, &_cxq, Front) == Front) {  
                    break ;  
                }  
            }  
         }  
     } else  
     if (Policy == 3) {      // append to cxq  
        iterator->TState = ObjectWaiter::TS_CXQ ;  
        for (;;) {  
            ObjectWaiter * Tail ;  
            Tail = _cxq ;  
            if (Tail == NULL) {  
                iterator->_next = NULL ;  
                if (Atomic::cmpxchg_ptr (iterator, &_cxq, NULL) == NULL) {  
                   break ;  
                }  
            } else {  
                while (Tail->_next != NULL) Tail = Tail->_next ;  
                Tail->_next = iterator ;  
                iterator->_prev = Tail ;  
                iterator->_next = NULL ;  
                break ;  
            }  
        }  
     } else {  
        ParkEvent * ev = iterator->_event ;  
        iterator->TState = ObjectWaiter::TS_RUN ;  
        OrderAccess::fence() ;  
        ev->unpark() ;  
     }  
  
     if (Policy < 4) {  
       iterator->wait_reenter_begin(this);  
     }  
  
     // _WaitSetLock protects the wait queue, not the EntryList.  We could  
     // move the add-to-EntryList operation, above, outside the critical section     
     // protected by _WaitSetLock.  In practice that's not useful.  With the     
     // exception of  wait() timeouts and interrupts the monitor owner     
     // is the only thread that grabs _WaitSetLock.  There's almost no contention     
     // on _WaitSetLock so it's not profitable to reduce the length of the     
     // critical section.  
  }  
     Thread::SpinRelease (&_WaitSetLock) ;  
  
  if (iterator != NULL && ObjectMonitor::_sync_Notifications != NULL) {  
     ObjectMonitor::_sync_Notifications->inc() ;  
  }  
}
```

`Object#notifyAll()`对应的虚拟机方法是`JVM_MonitorNotifyAll`：
```cpp
JVM_ENTRY(void, JVM_MonitorNotifyAll(JNIEnv* env, jobject handle))  
  JVMWrapper("JVM_MonitorNotifyAll");  
  Handle obj(THREAD, JNIHandles::resolve_non_null(handle));  
  ObjectSynchronizer::notifyall(obj, CHECK);  
JVM_END
```

`ObjectSynchronizer::notifyall()`：
```cpp
void ObjectMonitor::notifyAll(TRAPS) {  
  CHECK_OWNER();  
  ObjectWaiter* iterator;  
  if (_WaitSet == NULL) {  
      TEVENT (Empty-NotifyAll) ;  
      return ;  
  }  
  DTRACE_MONITOR_PROBE(notifyAll, this, object(), THREAD);  
  
  int Policy = Knob_MoveNotifyee ;  
  int Tally = 0 ;  
  Thread::SpinAcquire (&_WaitSetLock, "WaitSet - notifyall") ;  
  
  for (;;) {  
     iterator = DequeueWaiter () ;  
     if (iterator == NULL) break ;  
     TEVENT (NotifyAll - Transfer1) ;  
     ++Tally ;  
  
     // Disposition - what might we do with iterator ?  
     // a.  add it directly to the EntryList - either tail or head.     
     // b.  push it onto the front of the _cxq.     
     // For now we use (a).  
     guarantee (iterator->TState == ObjectWaiter::TS_WAIT, "invariant") ;  
     guarantee (iterator->_notified == 0, "invariant") ;  
     iterator->_notified = 1 ;  
     Thread * Self = THREAD;  
     iterator->_notifier_tid = JFR_THREAD_ID(Self);  
     if (Policy != 4) {  
        iterator->TState = ObjectWaiter::TS_ENTER ;  
     }  
  
     ObjectWaiter * List = _EntryList ;  
     if (List != NULL) {  
        assert (List->_prev == NULL, "invariant") ;  
        assert (List->TState == ObjectWaiter::TS_ENTER, "invariant") ;  
        assert (List != iterator, "invariant") ;  
     }  
  
     if (Policy == 0) {       // prepend to EntryList  
         if (List == NULL) {  
             iterator->_next = iterator->_prev = NULL ;  
             _EntryList = iterator ;  
         } else {  
             List->_prev = iterator ;  
             iterator->_next = List ;  
             iterator->_prev = NULL ;  
             _EntryList = iterator ;  
        }  
     } else  
     if (Policy == 1) {      // append to EntryList  
         if (List == NULL) {  
             iterator->_next = iterator->_prev = NULL ;  
             _EntryList = iterator ;  
         } else {  
            // CONSIDER:  finding the tail currently requires a linear-time walk of  
            // the EntryList.  We can make tail access constant-time by converting to            
            // a CDLL instead of using our current DLL.            
            ObjectWaiter * Tail ;  
            for (Tail = List ; Tail->_next != NULL ; Tail = Tail->_next) ;  
            assert (Tail != NULL && Tail->_next == NULL, "invariant") ;  
            Tail->_next = iterator ;  
            iterator->_prev = Tail ;  
            iterator->_next = NULL ;  
        }  
     } else  
     if (Policy == 2) {      // prepend to cxq  
         // prepend to cxq         
         iterator->TState = ObjectWaiter::TS_CXQ ;  
         for (;;) {  
             ObjectWaiter * Front = _cxq ;  
             iterator->_next = Front ;  
             if (Atomic::cmpxchg_ptr (iterator, &_cxq, Front) == Front) {  
                 break ;  
             }  
         }  
     } else  
     if (Policy == 3) {      // append to cxq  
        iterator->TState = ObjectWaiter::TS_CXQ ;  
        for (;;) {  
            ObjectWaiter * Tail ;  
            Tail = _cxq ;  
            if (Tail == NULL) {  
                iterator->_next = NULL ;  
                if (Atomic::cmpxchg_ptr (iterator, &_cxq, NULL) == NULL) {  
                   break ;  
                }  
            } else {  
                while (Tail->_next != NULL) Tail = Tail->_next ;  
                Tail->_next = iterator ;  
                iterator->_prev = Tail ;  
                iterator->_next = NULL ;  
                break ;  
            }  
        }  
     } else {  
        ParkEvent * ev = iterator->_event ;  
        iterator->TState = ObjectWaiter::TS_RUN ;  
        OrderAccess::fence() ;  
        ev->unpark() ;  
     }  
  
     if (Policy < 4) {  
       iterator->wait_reenter_begin(this);  
     }  
  
     // _WaitSetLock protects the wait queue, not the EntryList.  We could  
     // move the add-to-EntryList operation, above, outside the critical section     
     // protected by _WaitSetLock.  In practice that's not useful.  With the     
     // exception of  wait() timeouts and interrupts the monitor owner     
     // is the only thread that grabs _WaitSetLock.  There's almost no contention     
     // on _WaitSetLock so it's not profitable to reduce the length of the     
     // critical section.  
  }  
  
  Thread::SpinRelease (&_WaitSetLock) ;  
  
  if (Tally != 0 && ObjectMonitor::_sync_Notifications != NULL) {  
     ObjectMonitor::_sync_Notifications->inc(Tally) ;  
  }  
}
```

### 2.7.2 Thread的方法
使用`Thread#join()`方法，可以让线程对象进入`WAITING`状态。

使用`Thread#join(long)`和`Thread#sleep()`方法，可以让线程对象进入`TIMED_WAITING`状态。

`Thread#join()`、`Thread#join(long)`和`Thread#join(long,int)`底层是同一个方法：
```java
public final synchronized void join(long millis)  
throws InterruptedException {  
    long base = System.currentTimeMillis();  
    long now = 0;  
  
    if (millis < 0) {  
        throw new IllegalArgumentException("timeout value is negative");  
    }  
    // Thread#join()或Thread#join(0)
    if (millis == 0) {  
        // 
        while (isAlive()) {  
            wait(0);  
        }  
    } 
    // Thread#join(long)
    else {  
        while (isAlive()) {  
            long delay = millis - now;  
            if (delay <= 0) {  
                break;  
            }  
            wait(delay);  
            now = System.currentTimeMillis() - base;  
        }  
    }  
}
```

### 2.7.3 LockSupport
使用`Object#wait()`、`Thread#join()`和`LockSupport#park()`方法，可以让线程进入`WAITING`状态。

使用`Thread#sleep()`、`Object#wait(long)`、`Thread#join(long)`、`LockSupport#parkNanos()`和`LockSupport#parkUntil()`方法，可以让线程进入`TIMED_WAITING`状态。