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
    // 获取当前线程对象
    Thread t = Thread.currentThread();  
    // 从线程对象中获取ThreadLocalMap
    ThreadLocalMap map = getMap(t);  
    if (map != null)  
        // 如果已存在，则设置值（已有会覆盖），键为当前ThreadLocal对象的哈希值
        map.set(this, value);  
    else  
        // 如果不存在，则新建，并且设置值，键为当前ThreadLocal对象哈希值
        createMap(t, value);  
}
```

### 3.1.1 获取ThreadLocalMap
`java.lang.ThreadLocal#getMap()`方法可以从线程对象中获取`ThreadLocalMap`信息：
```java
ThreadLocalMap getMap(Thread t) {  
    return t.threadLocals;  
}
```

### 3.1.2 新建ThreadLocalMap
如果当前线程对象不存在本地变量信息，则会调用`java.lang.ThreadLocal#createMap()`方法，为线程对象创建本地变量信息：
```java
void createMap(Thread t, T firstValue) {  
    t.threadLocals = new ThreadLocalMap(this, firstValue);  
}
```

核心逻辑位于`java.lang.ThreadLocal.ThreadLocalMap#ThreadLocalMap()`：
```java
ThreadLocalMap(ThreadLocal<?> firstKey, Object firstValue) {  
    // 创建Entry数组，长度16
    table = new Entry[INITIAL_CAPACITY];  
    // 计算当前ThreadLocal的位置
    int i = firstKey.threadLocalHashCode & (INITIAL_CAPACITY - 1);  
    // 在指定位置添加Entry对象
    table[i] = new Entry(firstKey, firstValue);  
    // 设置当前本地变量数量
    size = 1;  
    // 设置阈值：len*2/3（负载因子为2/3）
    setThreshold(INITIAL_CAPACITY);  
}
```

### 3.1.3 修改ThreadLocalMap
如果当前线程对象存在本地变量信息，则会直接调用`java.lang.ThreadLocal.ThreadLocalMap#set()`方法，往`ThreadLocalMap`对象添加数据：
```java
private void set(ThreadLocal<?> key, Object value) {  
    // 获取Entry数组和长度
    Entry[] tab = table;  
    int len = tab.length;  
    // 计算当前ThreadLocal的位置
    int i = key.threadLocalHashCode & (len-1);  
  
    // 使用线性探测法解决哈希碰撞((i + 1 < len) ? i + 1 : 0)：以当前位置为起点，循环遍历数组，直到该位置值为null
    // 在每次循环中，会判断该位置的值是否为当前ThreadLocal对应值，进行覆盖
    for (Entry e = tab[i];  
         e != null;  
         e = tab[i = nextIndex(i, len)]) {  
        // e不为空，获取ThreadLocal对象k
        ThreadLocal<?> k = e.get();  
  
        // k和key相等，进行覆盖
        if (k == key) {  
            e.value = value;  
            return;  
        }  
  
        // 如果k为null，说明被垃圾回收机制回收了，需要替换这个“不新鲜”的本地变量
        if (k == null) {  
            replaceStaleEntry(key, value, i);  
            return;  
        }  
    }  
  
    // 没有找到当前ThreadLocal的对应值，进行新增
    tab[i] = new Entry(key, value);  
    int sz = ++size;  
    
    // 以当前位置为起点，循环遍历数组，对key为null的entry对象进行清除
    if (!cleanSomeSlots(i, sz) && sz >= threshold)  
        // 如果当前没有要清除的entry && 数量大于阈值（len*2/3），需要进行rehash
        rehash();  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#replaceStaleEntry()`方法：
```java
private void replaceStaleEntry(ThreadLocal<?> key, Object value,  
                               int staleSlot) {  
    // 获取Entry数组
    Entry[] tab = table;  
    int len = tab.length;  
    Entry e;  
  
    // 往前遍历，直到entry为null：获取最前面的key为null的entry的位置
    int slotToExpunge = staleSlot;  
    for (int i = prevIndex(staleSlot, len);  
         (e = tab[i]) != null;  
         i = prevIndex(i, len))  
        if (e.get() == null)  
            slotToExpunge = i;  
  
    // Find either the key or trailing null slot of run, whichever  
    // occurs first    for (int i = nextIndex(staleSlot, len);  
         (e = tab[i]) != null;  
         i = nextIndex(i, len)) {  
        ThreadLocal<?> k = e.get();  
  
        // If we find key, then we need to swap it  
        // with the stale entry to maintain hash table order.        // The newly stale slot, or any other stale slot        // encountered above it, can then be sent to expungeStaleEntry        // to remove or rehash all of the other entries in run.        if (k == key) {  
            e.value = value;  
  
            tab[i] = tab[staleSlot];  
            tab[staleSlot] = e;  
  
            // Start expunge at preceding stale entry if it exists  
            if (slotToExpunge == staleSlot)  
                slotToExpunge = i;  
            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);  
            return;  
        }  
  
        // If we didn't find stale entry on backward scan, the  
        // first stale entry seen while scanning for key is the        // first still present in the run.        if (k == null && slotToExpunge == staleSlot)  
            slotToExpunge = i;  
    }  
  
    // If key not found, put new entry in stale slot  
    tab[staleSlot].value = null;  
    tab[staleSlot] = new Entry(key, value);  
  
    // If there are any other stale entries in run, expunge them  
    if (slotToExpunge != staleSlot)  
        cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#cleanSomeSlots()`方法会

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