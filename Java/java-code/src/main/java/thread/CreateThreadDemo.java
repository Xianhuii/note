package thread;

public class CreateThreadDemo {
    public static void main(String[] args) {
//        extendsThread();
//        implementsRunnable();
        multiTask();
    }

    public static void extendsThread() {
        Thread thread = new Thread() {
            @Override
            public void run() {
                System.out.println("run()");
            }
        };
        thread.start();
        System.out.println("main()");
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
                System.out.println("run()");
            }
        };
        Thread thread1 = new Thread(task);
        thread1.start();
        Thread thread2 = new Thread(task);
        thread2.start();
    }

    class MyThread implements Runnable {

        @Override
        public void run() {

        }
    }
}
