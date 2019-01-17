package me.in1978.learn.spring.jedis;

import java.util.Iterator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class Util {

    private static final char[] chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append(chars[len % chars.length]);
        }
        return buf.toString();
    }


    public static void main(String[] args) throws InterruptedException {
        Stream<String> keys = IntStream.range(0, 1000).mapToObj(i -> String.format("key%10d", i));
        BBQ<String> q = BBQ.fromStream(keys, 100);
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

                    if (s == null)
                        break;

                    System.out.println(s);
                }
            });
        }
        service.shutdown();
        service.awaitTermination(1, TimeUnit.MINUTES);
    }

    public static <T> Iterable<T> iterable(Iterator<T> itr) {

        return new Iterable<T>() {

            @Override
            public Iterator<T> iterator() {
                return itr;
            }
        };
    }
}
