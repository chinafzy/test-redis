package org.wtb.learn.spring.jedis;

import java.io.PrintStream;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

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
        int[] values = PercentCalculator.counts(records, percents);
        pw.printf("%10s %12s \n", "Percent(%)", "Use Time(ms)");
        for (int i = 0; i < percents.length; i++) {
            pw.printf("% 10.2f % 12d \n", percents[i] * 100, values[i]);
        }
    }

    public static void main(String[] args) {
        Speeder speeder = new Speeder();
        Random rnd = new Random();
        IntStream.range(1, 100_000).forEach(i -> speeder.record(rnd.nextInt(10)));

        speeder.printSummary(System.out, new double[] { 0.5, 0.6, 0.7, 0.75, 0.8, 0.9, 0.95, 0.99, 0.999, 0.9999, 1 });
    }

}
