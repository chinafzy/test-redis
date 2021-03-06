package me.in1978.learn.spring.jedis;

import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
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
public class BenBQRange {

    @Autowired
    private ApplicationContext spring;

    @Autowired
    private TestConf testConf;

    private BlockingQueue<long[]> ranges(long num, int bufSize) {

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

        return BBQ.fromIterator(ranges, bufSize);
    }

    @Test
    public void run() {

        //
        // read configuration.
        final int concurrency = testConf.getConcurrency();
        final int number = testConf.getNumber();
        final double[] percents = testConf.getPrintPercents();

        if (testConf.shardMode()) {
            System.out.println("Shard Mode: " + testConf.getShardUrls());
        } else {
            if (StringUtils.isEmpty(testConf.getSingleUrl())) {
                throw new IllegalArgumentException("One of wtb.redis.shared.urls/wtb.redis.single.url should be set.");
            }

            System.out.println("Single Mode: " + testConf.getSingleUrl());
        }

        System.out.printf("number: %,d; concurrency: %d; value_size: %,d; \n", //
                number, concurrency, testConf.getValueSize());

        PeriodPrinter periodPrinter = new PeriodPrinter(testConf.getSummaryStep());
        final Counter counter = new Counter().addNotifier(periodPrinter);

        final ExecutorService executor = Executors.newFixedThreadPool(concurrency);
        final String str = Util.buildStr(testConf.getValueSize());

        CountDownLatch kickOff = new CountDownLatch(1);
        List<Speeder> speeders = new CopyOnWriteArrayList<>();
        if (testConf.getSummarySpeedStep() > 0) {
            counter.addNotifier((long value) -> {
                if (value % testConf.getSummarySpeedStep() != 0)
                    return;

                Speeder.merge(speeders).printSummary(System.out, percents);
            });
        }

        List<AtomicLong> successCounts = new CopyOnWriteArrayList<>();
        List<AtomicLong> failCounts = new CopyOnWriteArrayList<>();

        final BlockingQueue<long[]> ranges = ranges(number, concurrency * 3);

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
                    while ((range = ranges.take()) != null) {
                        testRange(range);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            private void testRange(long[] range) {
                LongStream.range(range[0], range[1]).forEach(l -> {
                    String key = Util.key(l);
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

        CountDownLatch tasksReady = new CountDownLatch(concurrency);
        // submit test tasks.
        IntStream.range(0, concurrency).forEach(i -> {
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
        try {
            tasksReady.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        long stamp1 = System.currentTimeMillis();

        kickOff.countDown();
        periodPrinter.reset();
        periodPrinter.printHeader();

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        long used = System.currentTimeMillis() - stamp1;

        long successCount = successCounts.stream().mapToLong(AtomicLong::get).sum();
        long failCount = failCounts.stream().mapToLong(AtomicLong::get).sum();

        System.out.printf("%,d in %,d ms; qps = %,d ; success(%,d); fail(%,d); concurrency(%,d) \n", //
                number, used, (((long) number) * 1000) / used, successCount, failCount, concurrency);

        Speeder speeder = Speeder.merge(speeders);
        System.out.println();
        System.out.println("Percentage of the requests served within a certain time (ms)");
        speeder.printSummary(System.out, percents);
    }

}
