package me.in1978.learn.spring.jedis;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.StringUtils;

import redis.clients.jedis.JedisCommands;
import redis.clients.util.Pool;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class Benchmark {

    @Autowired
    private ApplicationContext spring;
    @Autowired
    private TestConf testConf;

    @Test
    public void run() throws InterruptedException {

        //
        // prepare for testing.
        //       
        final int task_count = testConf.getConcurrency();
        final int all_count = testConf.getNumber();

        if (testConf.shardMode()) {
            System.out.println("Shard Mode: " + testConf.getShardUrls());
        } else {
            if (StringUtils.isEmpty(testConf.getSingleUrl())) {
                throw new IllegalArgumentException("One of wtb.redis.shared.urls/wtb.redis.single.url should be set.");
            }

            System.out.println("Single Mode: " + testConf.getSingleUrl());
        }

        System.out.printf("number: %,d; concurrency: %d; value_size: %,d; \n",
            all_count, task_count, testConf.getValueSize());

        PeriodPrinter periodPrinter = new PeriodPrinter(testConf.getSummaryStep());

        final Counter counter = new Counter().addNotifier(periodPrinter);

        // set up 
        final ExecutorService executor = Executors.newFixedThreadPool(task_count);
        final String str = buildStr(testConf.getValueSize());

        CountDownLatch kickoff = new CountDownLatch(1);
        List<Speeder> speeders = new CopyOnWriteArrayList<>();
        List<AtomicLong> successCounts = new CopyOnWriteArrayList<>();
        List<AtomicLong> failCounts = new CopyOnWriteArrayList<>();

        long[][] ranges = splitRanges(all_count, task_count);
        Arrays.asList(ranges).forEach(range -> executor.submit(() -> {
            try {

                final JedisCommands cmds = (JedisCommands) spring
                    .getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class).getResource();

                final long startPos = range[0], endPos = range[1];
                final Speeder speeder = new Speeder();
                speeders.add(speeder);

                final AtomicLong successCount = new AtomicLong();
                successCounts.add(successCount);
                final AtomicLong failCount = new AtomicLong();
                failCounts.add(failCount);

                // warm up the connection
                IntStream.range(0, 1000).forEach(i -> cmds.get("k" + i));

                try {
                    kickoff.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                KeysMaker.make("key_", startPos, endPos).forEach(key -> {
                    long stampx = System.currentTimeMillis();

                    try {
                        cmds.set(key, str);
                        cmds.get(key);

                        int used = (int) (System.currentTimeMillis() - stampx);
                        speeder.record(used);
                        successCount.incrementAndGet();
                    } catch (Throwable tr) {
                        failCount.incrementAndGet();
                    }

                    counter.increase(1);
                });
            } catch (Throwable tr) {
                tr.printStackTrace();
            }

        }));

        long stamp1 = System.currentTimeMillis();

        periodPrinter.printHeader();
        periodPrinter.reset();
        kickoff.countDown();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);

        long used = System.currentTimeMillis() - stamp1;

        long successCount = successCounts.stream().mapToLong(AtomicLong::get).sum();
        long failCount = failCounts.stream().mapToLong(AtomicLong::get).sum();

        System.out.printf("%,d in %,d ms; qps = %,d ; success(%,d); fail(%,d) \n", all_count, used,
            (((long) all_count) * 1000) / used, successCount, failCount);

        Speeder speeder = Speeder.merge(speeders);
        System.out.println();
        System.out.println("Percentage of the requests served within a certain time (ms)");
        speeder.printSummary(System.out, new double[] { .5, .75, .8, .9, .95, .99, .995, .999, .9999, .99999, 1 });
    }

    /**
     * Split a number into ranges.
     *  
     * @param num
     * @param count
     * @return
     */
    private static long[][] splitRanges(long num, int count) {
        long[] tmp = new long[count + 1];

        long step = num / count;
        for (int i = 0; i < count; i++) {
            tmp[i] = step * i;
        }
        tmp[count] = num;

        int tail = (int) (num % count);
        for (int i = 1; i < tail; i++) {
            tmp[count - i] -= tail - i;
        }

        long[][] ret = new long[count][];
        for (int i = 0; i < count; i++) {
            ret[i] = new long[] { tmp[i], tmp[i + 1] };
        }

        return ret;
    }

    @SuppressWarnings("unused")
    private static long[] nextRange(AtomicLong pos, int step, long max) {
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
