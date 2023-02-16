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
