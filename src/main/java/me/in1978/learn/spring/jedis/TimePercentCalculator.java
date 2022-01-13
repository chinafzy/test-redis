package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

/**
 * 参考Apache Benchmark(ab) 做的一个耗时水平线计算器 
 * 
 * @author zeyufang
 */
public class TimePercentCalculator {

    /**
     * 针对传入的耗时记录，计算出来百分比水平线的耗时  
     * 
     * @param m
     * @param percents
     * @return
     */
    public static int[] cal(Map<Integer, ? extends Number> m, double[] percents) {
        if (m.isEmpty())
            return new int[percents.length];

        Arrays.sort(percents);
        TreeMap<Integer, Long> m3 = m.entrySet().stream() //
                .collect(TreeMap::new, (dest, ent) -> dest.put(ent.getKey(), ent.getValue().longValue()), Map::putAll);

        return counts1(m3, percents);
    }

    private static int[] counts1(Map<Integer, Long> m, double[] percents) {

        long allCount = m.values().stream().mapToLong(Long::longValue).sum();

        Iterator<Map.Entry<Integer, Long>> ents = m.entrySet().iterator();
        Map.Entry<Integer, Long> ent = ents.next();
        long cc2 = ent.getValue();

        List<Integer> ret = new ArrayList<>();
        Iterator<Double> percentValues = DoubleStream.of(percents).map(d -> d * allCount).iterator();
        while (percentValues.hasNext()) {
            double percentValue = percentValues.next();

            while (cc2 < percentValue && ents.hasNext()) {
                ent = ents.next();
                cc2 += ent.getValue();
            }
            ret.add(ent.getKey());
        }

        return ret.stream().mapToInt(i -> i).toArray();
    }

    private static int[] counts2(Map<Integer, Long> m, double[] percents) {

        long allCount = m.values().stream().mapToLong(Long::longValue).sum();

        Iterator<Map.Entry<Integer, Long>> ents = m.entrySet().iterator();

        AtomicLong count = new AtomicLong(0);
        AtomicReference<Integer> keyHolder = new AtomicReference<>();
        {
            Map.Entry<Integer, Long> ent = ents.next();
            keyHolder.set(ent.getKey());
            count.addAndGet(ent.getValue());
        }

        return DoubleStream.of(percents).mapToInt(d -> {
            double percentValue = d * allCount;
            while (count.get() < percentValue && ents.hasNext()) {
                Map.Entry<Integer, Long> ent = ents.next();
                keyHolder.set(ent.getKey());
                count.addAndGet(ent.getValue());
            }

            return keyHolder.get();

        }).toArray();

    }

    /**
     * 标准的笨办法来低效完成 但是结果绝对正确。作为标准参考函数来校正其它函数 
     * 
     * @param m
     * @param percents
     * @return
     */
    private static int[] counts100(Map<Integer, Long> m, double[] percents) {
        int[] arr = m.entrySet().stream() //
                .flatMapToInt(ent -> IntStream.range(0, ent.getValue().intValue()).map(i -> ent.getKey())) //
                .toArray();

        return DoubleStream.of(percents) //
                .mapToInt(d -> arr[Math.max(0, (int) (arr.length * d) - 1)]) //
                .toArray();
    }

    public static void main(String[] args) {

        double[] percents = { 1, 0.999, 0.99, 0.95, 0.9, 0.8, 0.75, 0.7, 0.5 };
        Arrays.sort(percents);

        Map<Integer, Long> m = new TreeMap<>();
        {
            Random rnd = new Random(19781971);
            IntStream.range(1, 100).forEach(i -> m.put(i, (long) rnd.nextInt(111)));
        }
        int[] cs1 = counts1(m, percents);
        int[] cs2 = counts2(m, percents);
        int[] cs100 = counts100(m, percents);

        System.out.printf("%10s %10s %10s %10s \n", "percent", "standard", "algo1", "algo2");
        for (int i = 0; i < percents.length; i++) {
            System.out.printf("%, 10.3f %, 10d %, 10d %, 10d  \n", percents[i], cs100[i], cs1[i], cs2[i]);
        }

        //        m.entrySet().forEach(System.out::println);
    }

}
