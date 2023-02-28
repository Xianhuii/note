package thread;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.FutureTask;

public class CompletableFutureDemo {
    public static void main(String[] args) {
        Callable<String> task = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return null;
            }
        };
        CompletableFuture<String> completableFuture = new CompletableFuture<>();
        
    }
}
