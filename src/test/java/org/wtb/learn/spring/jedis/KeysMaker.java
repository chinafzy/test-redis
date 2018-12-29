package org.wtb.learn.spring.jedis;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class KeysMaker {

    public static Stream<String> make(String prefix, long startInclusive, long endExclusive) {
        String format = prefix + "%09d";
        return LongStream.range(startInclusive, endExclusive).mapToObj(i -> String.format(format, i));
    }

    /**
     * Convert a Stream into BlockingQueue. 
     * <pre>
     * Stream<String> keys = getKeys();
     * final String STOP_FLAG = "Stop Boring.";
     * 
     * int workerCount = 5;
     * int stopRepeats = workerCount; // >= workerCount
     * int queueSize = workerCount * 2; // >= stopRepeats
     * 
     * BlockingQueue<String> keyQueue = asBq(keys, queueSize, STOP_FLAG, stopRepeats);
     * 
     * ExecutorService executor = Executors.newFixedThreadPool(workerCount);
     * for (int i = 0; i < workerCount; i++) {
     *     executor.submit(() -> {
     *         String key;
     *         while(true) {
     *             key = queue.take(); // try catch here.
     *             
     *             if (key == STOP_FLAG)  // STOP_FLAG comes. 
     *                 break;   // break from looping.
     *             
     *             // work with key
     *         }
     *     });
     * }
     * 
     * executor.shutdown();  // tell executor to stop and wait for all done.
     * executor.awaitTermination(1, TimeUnit.MINUTES);
     * 
     * </pre>
     * 
     * @param stream
     * @param poolSize
     * @param stopFlag
     * @param stopRepeats
     * @return
     */
    public static <T> BlockingQueue<T> asBq(Stream<T> stream, int poolSize, T stopFlag, int stopRepeats) {
        if (stopRepeats > poolSize)
            throw new IllegalArgumentException(String.format("stopRepeats %,d > poolSize %,d", stopRepeats, poolSize));

        LinkedBlockingQueue<T> queue = new LinkedBlockingQueue<>(poolSize);
        new Thread(() -> {
            stream.forEach(t -> {
                try {
                    queue.put(t);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
            IntStream.range(0, stopRepeats).forEach(i -> {
                try {
                    queue.put(stopFlag);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            });
        }).start();

        return queue;
    }

    public static void main(String[] args) throws InterruptedException {
        Stream<String> keys = make("k1", 0, 1000);
        final String STOP = "Please Stop Boring.";
        BlockingQueue<String> q = asBq(keys, 1000, STOP, 10);
        ExecutorService service = Executors.newCachedThreadPool();
        for (int i = 0; i < 3; i++) {
            service.submit(() -> {
                String s;
                while (true) {
                    try {
                        s = q.take();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        throw new RuntimeException(e);
                    }

                    if (s == STOP)
                        break;

                    System.out.println(s);
                }
            });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

}
