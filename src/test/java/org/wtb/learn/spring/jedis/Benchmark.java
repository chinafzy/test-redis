package org.wtb.learn.spring.jedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
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

    @Test
    public void test() throws InterruptedException {
        final long task_count = 10, repeat_per_task = 10_000;
        final long all_count = task_count * repeat_per_task;

        ExecutorService executor = buildExecutor((int) task_count, 1000);
        final ValueOperations<String, String> ops = tpl.opsForValue();
        final String str = buildStr(1024 * 10);

        CountDownLatch starter = new CountDownLatch(1);
        for (int i = 0; i < task_count; i++) {
            final int i2 = i;
            executor.submit(() -> {
                try {
                    starter.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                for (int j = 0; j < repeat_per_task; j++) {
                    ops.set(i2 + "key" + j, str);
                }
            });
        }

        long stamp1 = System.currentTimeMillis();
        starter.countDown();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long stamp2 = System.currentTimeMillis();
        long used = stamp2 - stamp1;

        System.out.printf("%,d in %,d ms; qps = %,d \n", all_count, used, (all_count * 1000) / used);
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

        return new ThreadPoolExecutor(con, con, 1, TimeUnit.HOURS, taskQueue, (task, executor2) -> taskQueue.add(task));
    }

}
