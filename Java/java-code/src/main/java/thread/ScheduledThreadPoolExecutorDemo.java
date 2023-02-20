package thread;

import java.util.concurrent.*;

public class ScheduledThreadPoolExecutorDemo {
    public static void main(String[] args) {
        ThreadFactory threadFactory = Executors.defaultThreadFactory();
        RejectedExecutionHandler rejectedExecutionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        // 1、创建线程池
        ScheduledThreadPoolExecutor scheduledThreadPoolExecutor = new ScheduledThreadPoolExecutor(
                3,
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
        // 执行延时任务
//        scheduledThreadPoolExecutor.schedule(task, 1, TimeUnit.SECONDS);
        // 执行定时任务
        scheduledThreadPoolExecutor.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
        scheduledThreadPoolExecutor.shutdown();
        scheduledThreadPoolExecutor.shutdownNow();
    }
}
