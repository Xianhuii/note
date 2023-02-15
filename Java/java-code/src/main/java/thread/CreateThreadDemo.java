package thread;

public class CreateThreadDemo {
    public static void main(String[] args) {
//        extendsThread();
        implementsRunnable();
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

    class MyThread implements Runnable {

        @Override
        public void run() {

        }
    }
}
