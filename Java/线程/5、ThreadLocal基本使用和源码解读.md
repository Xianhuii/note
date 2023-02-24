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
`java.lang.ThreadLocal#set()`方法会为当前线程添加本地变量：
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

`java.lang.ThreadLocal.ThreadLocalMap#replaceStaleEntry()`方法，会从本地变量数组中找到当前`ThreadLocal`对应的值，替换当前“不新鲜”的值（因为使用了线性探测法，实际位置可能改变）。如果没有找到，则在当前位置新增本地变量：
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
  
    // 从头开始遍历，直到entry为null
    for (int i = nextIndex(staleSlot, len);  
         (e = tab[i]) != null;  
         i = nextIndex(i, len)) {  
        // 获取该位置的key
        ThreadLocal<?> k = e.get();  
  
        // 如果找到当前entry，进行覆盖值，并将entry交换到staleSlot位置
        if (k == key) {  
            e.value = value;  
  
            // 将entry交换到staleSlot位置
            tab[i] = tab[staleSlot];  
            tab[staleSlot] = e;  
  
            // Start expunge at preceding stale entry if it exists  
            if (slotToExpunge == staleSlot)  
                slotToExpunge = i;  
            cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);  
            return;  
        }  
  
        // If we didn't find stale entry on backward scan, the  
        // first stale entry seen while scanning for key is the        
        // first still present in the run.        
        if (k == null && slotToExpunge == staleSlot)  
            slotToExpunge = i;  
    }  
  
    // 如果没有找到key，则新建entry对象
    tab[staleSlot].value = null;  
    tab[staleSlot] = new Entry(key, value);  
  
    // If there are any other stale entries in run, expunge them  
    if (slotToExpunge != staleSlot)  
        cleanSomeSlots(expungeStaleEntry(slotToExpunge), len);  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#cleanSomeSlots()`方法会按规则访问本地变量数组，对key为空的本地变量进行清除，释放内存资源：
```java
private boolean cleanSomeSlots(int i, int n) {  
    boolean removed = false;  
    Entry[] tab = table;  
    int len = tab.length;  
    do {  
        i = nextIndex(i, len);  
        Entry e = tab[i];  
        // entry不为空，且key为空（被垃圾收集器回收了）
        if (e != null && e.get() == null) {  
            n = len;  
            removed = true;  
            i = expungeStaleEntry(i);  
        }  
    } while ( (n >>>= 1) != 0);  
    return removed;  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#expungeStaleEntry()`方法会将当前位置，以及其他key为空的本地变量设置为`null`，释放内存资源。同时还会重新计算位置，并且进行交换：
```java
private int expungeStaleEntry(int staleSlot) {  
    Entry[] tab = table;  
    int len = tab.length;  
  
    // 清除当前位置的entry
    tab[staleSlot].value = null;  
    tab[staleSlot] = null;  
    size--;  
  
    // Rehash until we encounter null  
    Entry e;  
    int i;  
    for (i = nextIndex(staleSlot, len);  
         (e = tab[i]) != null;  
         i = nextIndex(i, len)) {  
        ThreadLocal<?> k = e.get();  
        // 如果key为null，进行清除
        if (k == null) {  
            e.value = null;  
            tab[i] = null;  
            size--;  
        } 
        // 如果key不为null，重新计算其位置，进行交换位置
        else {  
            int h = k.threadLocalHashCode & (len - 1);  
            if (h != i) {  
                tab[i] = null;  
                while (tab[h] != null)  
                    h = nextIndex(h, len);  
                tab[h] = e;  
            }  
        }  
    }  
    return i;  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#rehash()`方法会清除“不新鲜”的本地变量、重新计算&交换位置、扩容：
```java
private void rehash() {  
    // 清除“不新鲜”的本地变量、重新计算&交换位置
    expungeStaleEntries();  
  
    // 本地变量数量大于(threshold - threshold / 4)，进行扩容
    if (size >= threshold - threshold / 4)  
        resize();  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#resize()`方法会进行扩容，将本地变量数组扩容为原来的2倍：
```java
private void resize() {  
    // 获取oldTab
    Entry[] oldTab = table;  
    int oldLen = oldTab.length;  
    // 创建newLen
    int newLen = oldLen * 2;  
    Entry[] newTab = new Entry[newLen];  
    // rehash和数组复制
    int count = 0;  
    for (int j = 0; j < oldLen; ++j) {  
        Entry e = oldTab[j];  
        if (e != null) {  
            ThreadLocal<?> k = e.get();  
            if (k == null) {  
                e.value = null; // Help the GC  
            } else {  
                int h = k.threadLocalHashCode & (newLen - 1);  
                while (newTab[h] != null)  
                    h = nextIndex(h, newLen);  
                newTab[h] = e;  
                count++;  
            }  
        }  
    }  
    setThreshold(newLen);  
    size = count;  
    table = newTab;  
}
```

## 3.2 get
`java.lang.ThreadLocal#get()`方法：
```java
public T get() {  
    // 获取当前线程对象
    Thread t = Thread.currentThread();  
    // 从线程对象中获取ThreadLocalMap
    ThreadLocalMap map = getMap(t);  
    if (map != null) {  
        // 从map中获取当前ThreadLocal对应的值
        ThreadLocalMap.Entry e = map.getEntry(this);  
        if (e != null) {  
            @SuppressWarnings("unchecked")  
            T result = (T)e.value;  
            return result;  
        }  
    }  
    // 设置并返回初始值（null）
    return setInitialValue();  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#getEntry()`方法会根据ThreadLocal对象获取对应位置的本地变量值：
```java
private Entry getEntry(ThreadLocal<?> key) {  
    // 计算位置
    int i = key.threadLocalHashCode & (table.length - 1);  
    // 获取对应位置的entry
    Entry e = table[i];  
    // 如果entry匹配当前ThreadLocal：返回对应值
    if (e != null && e.get() == key)  
        return e;  
    else  
    // 如果entry不匹配当前ThreadLocal（entry==null或key不匹配）：根据线性探测法搜索
        return getEntryAfterMiss(key, i, e);  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#getEntryAfterMiss()`会根据线性探测法进行搜索：
```java
private Entry getEntryAfterMiss(ThreadLocal<?> key, int i, Entry e) {  
    Entry[] tab = table;  
    int len = tab.length;  
    // 遍历数组，进行搜索
    while (e != null) {  
        ThreadLocal<?> k = e.get();  
        // 找到：返回
        if (k == key)  
            return e;  
        // key为null：清除
        if (k == null)  
            expungeStaleEntry(i);  
        // 线性探测法下个位置
        else  
            i = nextIndex(i, len);  
        e = tab[i];  
    }  
    // 没找到：返回null
    return null;  
}
```

`java.lang.ThreadLocal#setInitialValue()`方法会为当前线程的本地变量设置一个初始值（默认是null）：
```java
private T setInitialValue() {  
    // 生成初始值（默认是null）
    T value = initialValue();  
    // 获取当前线程对象
    Thread t = Thread.currentThread();  
    // 获取线程对象的ThreadLocalMap
    ThreadLocalMap map = getMap(t);  
    // 设置初始值
    if (map != null)  
        map.set(this, value);  
    else  
        createMap(t, value);  
    return value;  
}
```

## 3.3 remove
`java.lang.ThreadLocal#remove()`方法会手动清除某个本地变量的对象引用，避免内存泄漏：
```java
public void remove() {  
    // 获取当前线程对象的ThreadLocalMap
    ThreadLocalMap m = getMap(Thread.currentThread());  
    if (m != null)  
        // 清除当前ThreadLocal对应的entry
        m.remove(this);  
}
```

`java.lang.ThreadLocal.ThreadLocalMap#remove()`：
```java
private void remove(ThreadLocal<?> key) {  
    Entry[] tab = table;  
    int len = tab.length;  
    // 计算位置
    int i = key.threadLocalHashCode & (len-1);  
    // 遍历数组
    for (Entry e = tab[i];  
         e != null;  
         e = tab[i = nextIndex(i, len)]) {  
        if (e.get() == key) {  
            // 清除entry
            e.clear();  
            expungeStaleEntry(i);  
            return;  
        }  
    }  
}
```

`java.lang.ref.Reference#clear()`方法会将引用设为null，然后再交给`expungeStaleEntry()`方法清除资源：
```java
public void clear() {  
    this.referent = null;  
}
```