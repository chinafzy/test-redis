package org.wtb.learn.spring.jedis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class KeysMaker {

    public static Stream<String> make(String prefix, int startInclusive, int endExclusive) {
        String format = prefix + "%09d";
        return IntStream.range(startInclusive, endExclusive).mapToObj(i -> String.format(format, i));
    }

    public static <T> BlockingQueue<T> wrap(Stream<T> stream, int poolSize) {
        LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(poolSize);
        new Thread(() -> stream.forEach(t -> {
            try {
                //                System.out.println("push " + t);
                queue.put(t);
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        })).start();

        return queue;
    }

    public static void main(String[] args) throws InterruptedException {
        Stream<String> keys = make("k1", 0, 1000);
        BlockingQueue<String> q = wrap(keys, 1000);
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0; i < 3; i++) {
            service.submit(() -> {
                while (true) {
                    try {
                        System.out.println(q.take());
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }
                }
            });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

}
