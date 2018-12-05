package org.wtb.learn.spring.jedis;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class Benchmark {
    @Autowired
    JedisConnectionFactory connFactory;

    @Autowired
    StringRedisTemplate tpl;

    ConcurrentHashMap<Integer, AtomicInteger> uses = new ConcurrentHashMap<>();

    @Test
    public void run() throws InterruptedException {
        final long task_count = 10, repeat_per_task = 200_000;
        final long all_count = task_count * repeat_per_task;

        ExecutorService executor = buildExecutor((int) task_count, 1000);
        final ValueOperations<String, String> ops = tpl.opsForValue();
        final String str = buildStr(1024 * 1);

        CountDownLatch starter = new CountDownLatch(1);
        final Counter counter = new Counter().addNotifier(new Counter.Notifier() {
            final int step = 100_000;
            long last = System.currentTimeMillis();
            long start = last;

            @Override
            public void notify(long count) {
                if (count % step == 0) {
                    long now = System.currentTimeMillis();
                    long used = now - last, allUsed = now - start;
                    System.out.printf("%, 10d : %, 5d / %, 7d  QPS(s) : %, 5d / % ,5d \n", count, used, allUsed,
                            step * 1000 / used, count * 1000 / allUsed);
                    last = now;
                }
            }
        });

        Speeder speeder = new Speeder();

        // 
        {
            int cccc = (int) (task_count * 3);
            ExecutorService s = buildExecutor(cccc, 1000);
            IntStream.range(1, cccc).forEach(i -> executor.submit(() -> ops.set("1", "1")));
            s.shutdown();
            s.awaitTermination(1, TimeUnit.MINUTES);
        }
        //        

        long stamp1 = System.currentTimeMillis();

        KeysMaker.make("key_", 0, (int) all_count).forEach(key -> {
            executor.submit(() -> {
                //                try {
                //                    starter.await();
                //                } catch (InterruptedException e) {
                //                    e.printStackTrace();
                //                    return;
                //                }

                //                System.out.println(key);
                long stampx = System.currentTimeMillis();
                ops.set(key, str);
                //                ops.get(key);
                int used = (int) (System.currentTimeMillis() - stampx);
                speeder.record(used);
                counter.increase(1);
            });
        });

        //        starter.countDown();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long stamp2 = System.currentTimeMillis();
        long used = stamp2 - stamp1;

        System.out.printf("%,d in %,d ms; qps = %,d \n", all_count, used, (all_count * 1000) / used);

        System.out.println("\nPercentage of the requests served within a certain time (ms)");
        speeder.printSummary(System.out, new double[] { 0.5, 0.75, 0.8, 0.9, 0.95, 0.99, 0.999, .9999, 1 });
    }

    static long[] nextRange(AtomicLong pos, int step, long max) {
        AtomicReference<long[]> tmp = new AtomicReference<>();

        pos.getAndUpdate((v) -> {
            long v2 = Math.min(max, v + step);
            tmp.set(new long[] { v, v2 });
            return v2;
        });

        return tmp.get();
    }

    private static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append('a');
        }
        return buf.toString();
    }

    private ThreadPoolExecutor buildExecutor(int con, int poolSize) {
        LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(poolSize);

        return new ThreadPoolExecutor(con, con, 1, TimeUnit.HOURS, taskQueue, (task, executor2) -> {
            try {
                taskQueue.put(task);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

}
