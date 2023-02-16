package thread;

public class CreateThreadDemo {
    public static void main(String[] args) {
//        extendsThread();
//        implementsRunnable();
//        multiTask();
//        multiTask2();
        interrupt();
    }

    public static void extendsThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                try {
                    System.out.println(this.getState());
                    Thread.sleep(1000);
                    System.out.println(this.getState());
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("run()");
            }
        };
        thread.start();
        System.out.println("main()");
        System.out.println(thread.getState());
    }

    public static void implementsRunnable() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("run()");
            }
        });
        thread.start();
        System.out.println("main()");
    }

    public static void multiTask() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(Thread.currentThread().getName() + ":yield()");
                Thread.yield();
                System.out.println(Thread.currentThread().getName() + ":run()");
            }
        };
        Thread thread1 = new Thread(task);
        thread1.start();
        Thread thread2 = new Thread(task);
        thread2.start();
//        thread1.yield();

        System.out.println("main()");
    }

    static class MyThread implements Runnable {
        private volatile int index = 0;
        public static volatile boolean exit = false;

        @Override
        public void run() {
            print1();
        }

        private synchronized void print1() {
            for (; index < 3; index++) {
                if (index == 2) {
                    exit = true;
                }
                System.out.println(Thread.currentThread().getName() + ":" + Thread.currentThread().getState() + ":" + index);
                if (index == 0) {
                    index++;
                    System.out.println(Thread.currentThread().getName() + ":yield()");
                    Thread.yield();
                    System.out.println(Thread.currentThread().getName() + ":after yield():" + Thread.currentThread().getState() + ":" + index);
                }
                if (index == 1) {
                    this.notifyAll();
                }
            }
        }

        private synchronized void print() {
            for (; index < 10; index++) {
                if (index == 3) {
                    index++;
                    try {
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (index == 6) {
                    index++;
                    try {
                        this.notifyAll();
                        this.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                if (index == 9) {
                    System.exit(-1);
                }
                System.out.println(Thread.currentThread().getName() + ":" + index);
            }
        }
    }

    public static void multiTask2() {
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
    }

    public static void interrupt() {
        Runnable task = new Runnable() {
            @Override
            public void run() {
                while (!Thread.interrupted()) {
                    System.out.println(Thread.currentThread().getName() + "interrupted: " + Thread.interrupted());
                }
            }
        };
        Thread thread = new Thread(task);
        thread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("before interrupt(): " + thread.isInterrupted());
        thread.interrupt();
        System.out.println("after interrupt()" + thread.isInterrupted());
    }
}
