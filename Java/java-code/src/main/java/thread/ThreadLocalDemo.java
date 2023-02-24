package thread;

import java.util.concurrent.*;

public class ThreadLocalDemo {
    public static final ThreadLocal<String> threadName = new ThreadLocal<>();

    public static void main(String[] args) {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                // 设置线程本地变量
                threadName.set(Thread.currentThread().getName());

                // ……复杂的业务调用

                // 获取线程本地变量
                synchronized (threadName) {
                    System.out.println("ThreadLocal: " + threadName.get());
                    System.out.println("Thread.currentThread().getName()" + Thread.currentThread().getName());
                }
                // 线程执行完成前，手动移除本地变量
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
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(3);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        // 1、创建线程池
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,
                6,
                1,
                TimeUnit.SECONDS,
                blockingQueue,
                threadFactory,
                rejectedExecutionHandler);
        return threadPoolExecutor;
    }
}
