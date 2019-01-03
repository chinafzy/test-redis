package me.in1978.learn.spring.jedis;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.IntConsumer;
import java.util.function.LongUnaryOperator;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

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

    private static final char[] chars = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append(chars[len % chars.length]);
        }
        return buf.toString();
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

    @Autowired
    private ApplicationContext spring;

    @Autowired
    private TestConf testConf;

    @SuppressWarnings("unused")
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

    private BlockingQueue<long[]> ranges(long num, int bufSize, long[] stopFlag) {

        Iterator<long[]> ranges = new Iterator<long[]>() {
            long pos = num;

            @Override
            public boolean hasNext() {
                return pos > 0;
            }

            @Override
            public long[] next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                long step = pos > 5_000_000 ? 5_000 : //
                pos > 1000_000 ? 1000 : //
                pos > 100_000 ? 100 : //
                pos > 10_000 ? 10 : //
                1;

                long pos2 = Math.max(0, pos - step);
                long[] ret = { pos2, pos };
                pos = pos2;

                return ret;
            }
        };

        BlockingQueue<long[]> ret = KeysMaker.asBq(ranges, bufSize, stopFlag);

        //        try {
        //            Thread.sleep(1000);
        //        } catch (InterruptedException e) {
        //            throw new RuntimeException(e);
        //        }

        return ret;
    }

    @Test
    public void run() throws InterruptedException {

        //
        // read configuration.
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

        System.out.printf("number: %,d; concurrency: %d; value_size: %,d; \n", //
                all_count, task_count, testConf.getValueSize());

        PeriodPrinter periodPrinter = new PeriodPrinter(testConf.getSummaryStep());
        final Counter counter = new Counter().addNotifier(periodPrinter);

        final ExecutorService executor = Executors.newFixedThreadPool(task_count);
        final String str = buildStr(testConf.getValueSize());

        CountDownLatch kickOff = new CountDownLatch(1);
        List<Speeder> speeders = new CopyOnWriteArrayList<>();
        List<AtomicLong> successCounts = new CopyOnWriteArrayList<>();
        List<AtomicLong> failCounts = new CopyOnWriteArrayList<>();

        final long[] STOP_RANGE = new long[0];
        final BlockingQueue<long[]> ranges = ranges(all_count, task_count * 3, STOP_RANGE);

        class TestRedis extends TestTask {

            public TestRedis(Runnable readyNotifier, CountDownLatch kickOff, IntConsumer speeder, LongUnaryOperator successCount,
                    LongUnaryOperator failCount) {
                super(readyNotifier, kickOff, speeder, successCount, failCount);
            }

            private JedisCommands cmds;

            @Override
            protected void prepare() {
                cmds = (JedisCommands) spring.getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class)
                        .getResource();
                // warm up 
                IntStream.range(0, 1_000).forEach(i -> cmds.get("k" + i));
            }

            @Override
            protected void test() {
                long[] range;
                try {
                    while ((range = ranges.take()) != STOP_RANGE) {
                        testRange(range);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            private void testRange(long[] range) {
                LongStream.range(range[0], range[1]).forEach(l -> {
                    String key = String.format("key_%09d", l);
                    long stampx = System.currentTimeMillis();

                    try {
                        cmds.set(key, str);

                        int used = (int) (System.currentTimeMillis() - stampx);
                        speeder.accept(used);

                        successCount.applyAsLong(1);
                    } catch (Throwable tr) {
                        tr.printStackTrace();
                        failCount.applyAsLong(1);
                    } finally {
                        counter.increase(1);
                    }
                });
            }

        }

        CountDownLatch tasksReady = new CountDownLatch(task_count);
        // submit test tasks.
        IntStream.range(0, task_count).forEach(i -> {
            Speeder speeder = new Speeder();
            speeders.add(speeder);
            AtomicLong successCount = new AtomicLong();
            successCounts.add(successCount);
            AtomicLong failCount = new AtomicLong();
            failCounts.add(failCount);

            executor.submit(new TestRedis(tasksReady::countDown, kickOff, speeder::record, successCount::addAndGet,
                    failCount::addAndGet));
        });

        // wait until all tasks are ready.
        tasksReady.await();

        long stamp1 = System.currentTimeMillis();

        kickOff.countDown();
        periodPrinter.reset();

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

}
