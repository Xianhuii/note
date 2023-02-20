package thread;

import java.util.concurrent.*;

public class ThreadPoolExecutorDemo {
    public static void main(String[] args) {
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
        // 3、提交任务
        for (int i = 0; i < 10; i++) {
            threadPoolExecutor.submit(task);
        }
        // 4、关闭线程池
        threadPoolExecutor.shutdown();
//        threadPoolExecutor.shutdownNow();
    }
}
