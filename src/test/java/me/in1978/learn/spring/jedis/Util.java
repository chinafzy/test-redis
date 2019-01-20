package me.in1978.learn.spring.jedis;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class Util {

    private static final char[] chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    public static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append(chars[len % chars.length]);
        }
        return buf.toString();
    }

    public static Stream<long[]> averageRanges(long num, int step) {
        Iterator<long[]> itr = new Iterator<long[]>() {
            long pos = 0;
            long[] item;

            @Override
            public long[] next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                try {
                    return item;
                } finally {
                    item = null;
                }
            }

            @Override
            public boolean hasNext() {
                if (item != null)
                    return true;

                if (pos >= num)
                    return false;

                long pos2 = Math.min(pos + step, num);
                item = new long[] { pos, pos2 };
                pos = pos2;

                return true;
            }
        };

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(itr, Spliterator.ORDERED), false);
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
    
    public static String key(long l) {
        return String.format("%09d", l);
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
