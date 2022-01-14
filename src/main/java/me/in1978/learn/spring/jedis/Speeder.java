package me.in1978.learn.spring.jedis;

import java.io.PrintStream;
import java.util.Collection;
import java.util.Random;
import java.util.Spliterators;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;

/**
 * 记速器，记录一次次的速度消耗，生成统计结果汇总。
 * <br />
 * 注意：本身是支持线程安全的，但是在高并发下建议使用每个线程一个实例，然后合并{@link #merge(Iterable speeders)}成一个。这样会从数据模型上降低竞态状态。
 * 
 * @author zeyufang
 */
public class Speeder {

    private final ConcurrentHashMap<Integer, AtomicInteger> records = new ConcurrentHashMap<>(1024);

    public void record(Integer speed) {
        AtomicInteger record = records.get(speed);
        if (record == null) {
            records.putIfAbsent(speed, new AtomicInteger());
            record = records.get(speed);
        }

        record.incrementAndGet();
    }

    public void printSummary(PrintStream pw, double[] percents) {
        int[] values = TimePercentCalculator.cal(records, percents);
        pw.printf("%10s %12s \n", "Percent", "Use Time(ms)");
        for (int i = 0; i < percents.length; i++) {
            pw.printf("% 10.3f % 12d \n", percents[i] * 100, values[i]);
        }
    }

    public static Speeder merge(Iterable<Speeder> speeders) {

        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(speeders.iterator(), 0), false) //
                .reduce(new Speeder(), (dest, speeder) -> {

                    speeder.records.entrySet().forEach(ent -> {
                        Integer k = ent.getKey();
                        AtomicInteger v = dest.records.get(k);
                        if (v == null) {
                            dest.records.put(k, v = new AtomicInteger());
                        }

                        v.addAndGet(ent.getValue().get());
                    });

                    return dest;
                });
    }

    public static void main(String[] args) {
        Speeder speeder = new Speeder();
        Random rnd = new Random();
        IntStream.range(1, 100_000).forEach(i -> speeder.record(rnd.nextInt(10)));

        speeder.printSummary(System.out, new double[] { 0.5, 0.6, 0.7, 0.75, 0.8, 0.9, 0.95, 0.99, 0.999, 0.9999, 1 });
    }

}
