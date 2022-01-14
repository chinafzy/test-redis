package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

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
public class Benchmark2 {

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
        TaskAllocator taskAllocator = new TaskAllocator(all_count, task_count);

        final Counter counter = new Counter().addNotifier(periodPrinter);

        // set up 
        final ExecutorService executor = Executors.newFixedThreadPool(task_count);
        final String str = buildStr(testConf.getValueSize());

        CountDownLatch kickoff = new CountDownLatch(1);
        ManySpeeders speeders = new ManySpeeders();
        List<AtomicLong> successCounts = new ArrayList<>(task_count);
        List<AtomicLong> failCounts = new ArrayList<>(task_count);

        for (var i = 0; i < task_count; i++) {
            executor.submit(() -> {
                //                try {

                final JedisCommands cmds = (JedisCommands) spring
                    .getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class).getResource();

                final Speeder speeder = speeders.onThread();

                final AtomicLong successCount = new AtomicLong();
                successCounts.add(successCount);
                final AtomicLong failCount = new AtomicLong();
                failCounts.add(failCount);

                try {
                    kickoff.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    return;
                }

                long[] range;
                while ((range = taskAllocator.allocate()) != null) {
                    String key = KeysMaker.onKey("key_", 0, testConf.getRangeSize());

                    long stamp1 = System.currentTimeMillis();

                    try {
                        cmds.set(key, str);
                        cmds.get(key);

                        successCount.incrementAndGet();
                    } catch (Throwable tr) {
                        failCount.incrementAndGet();
                    } finally {
                        int used = (int) (System.currentTimeMillis() - stamp1);
                        speeder.record(used);
                    }

                    counter.increase(1);
                }

                //                } catch (Throwable tr) {
                //                    tr.printStackTrace();
                //                }

            });
        }

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

        Speeder speeder = speeders.merged();
        System.out.println();
        System.out.println("Percentage of the requests served within a certain time (ms)");
        speeder.printSummary(System.out, new double[] { .5, .75, .8, .9, .95, .99, .995, .999, .9999, .99999, 1 });
    }

    private static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append('a');
        }
        return buf.toString();
    }

}
