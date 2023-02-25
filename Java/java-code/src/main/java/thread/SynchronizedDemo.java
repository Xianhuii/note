package thread;

public class SynchronizedDemo {
    public static synchronized void staticMethod() {
        String str = "static synchronized";
    }

    public synchronized void objMethod() {
        String str = "static synchronized";
    }

    public static void synchronizedBlock() {
        synchronized (SynchronizedDemo.class) {
            String str = "static synchronized";
        }
    }
}
