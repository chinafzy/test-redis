package me.in1978.learn.spring.jedis;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.LongStream;
import java.util.stream.Stream;

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
public class BenExecutorBatch {

    @Autowired
    private ApplicationContext spring;

    @Autowired
    private TestConf testConf;

    @Test
    public void run() {
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

        final ExecutorService executor = executor(concurrency, 3000);
        final String str = Util.buildStr(testConf.getValueSize());

        ThreadLocal<JedisCommands> jedisHolder = ThreadLocal.withInitial(() -> (JedisCommands) spring
                .getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class).getResource());
        ThreadLocal<Speeder> speederHolder = buildHolder(Speeder::new, speeders);
        ThreadLocal<AtomicLong> failCountHolder = buildHolder(AtomicLong::new, failCounts);
        ThreadLocal<AtomicLong> successCountHolder = buildHolder(AtomicLong::new, successCounts);

        class Test implements Runnable {
            Stream<String> keys;

            public Test(Stream<String> keys) {
                this.keys = keys;
            }

            @Override
            public void run() {
                JedisCommands jedis = jedisHolder.get();

                keys.forEach(key -> {

                    long stampx = System.currentTimeMillis();

                    try {
                        jedis.set(key, str);

                        int used = (int) (System.currentTimeMillis() - stampx);
                        speederHolder.get().record(used);

                        successCountHolder.get().incrementAndGet();
                    } catch (Throwable tr) {
                        tr.printStackTrace();
                        failCountHolder.get().incrementAndGet();
                    } finally {
                        counter.increase(1);
                    }
                });

            }
        }

        long stamp1 = System.currentTimeMillis();

        periodPrinter.reset();
        periodPrinter.printHeader();
        Util.averageRanges(number, testConf.getRangeSize()) //
                .map(range -> LongStream.range(range[0], range[1]).mapToObj(Util::key)) //
                .forEach(keys -> executor.submit(new Test(keys))) //
        ;

        executor.shutdown();
        try {
            executor.awaitTermination(1, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
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

    private static ExecutorService executor(int concurrency, int queueSize) {
        LinkedBlockingQueue<Runnable> q = new LinkedBlockingQueue<>(queueSize);
        ThreadPoolExecutor ret = new ThreadPoolExecutor(concurrency, concurrency, 1, TimeUnit.MINUTES, q, (r, exe) -> {
            try {
                q.put(r);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return ret;
    }

    private static <T> ThreadLocal<T> buildHolder(Supplier<T> supplier, List<T> list) {

        return new ThreadLocal<T>() {

            @Override
            protected T initialValue() {
                T ret = supplier.get();
                list.add(ret);

                return ret;
            }
        };
    }
}
