package thread;

import java.util.concurrent.*;

public class ThreadPoolExecutorDemo {
    public static void main(String[] args) {
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(3);
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(
                3,
                6,
                1,
                TimeUnit.SECONDS,
                blockingQueue,
                threadFactory,
                rejectedExecutionHandler);
        Runnable task = new Runnable() {
            @Override
            public void run() {
                System.out.println(Thread.currentThread().getName() + ": run()");
//                try {
//                    Thread.sleep(500);
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
            }
        };
        for (int i = 0; i < 10; i++) {
            threadPoolExecutor.submit(task);
        }
    }
}
