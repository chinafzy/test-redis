package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;

public class PercentCalculator {

    public static int[] counts(Map<Integer, ? extends Number> m, double[] percents) {
        Arrays.sort(percents);
        Map<Integer, Integer> m3 = m.entrySet().stream() //
                .collect(TreeMap::new, (ret, ent) -> ret.put(ent.getKey(), ent.getValue().intValue()), Map::putAll);

        return counts1(m3, percents);
    }

    private static int[] counts1(Map<Integer, Integer> m, double[] percents) {
        if (m.isEmpty())
            return new int[percents.length];

        int allCount = m.values().stream().mapToInt(i -> i.intValue()).sum();
        Iterator<Double> percentValues = DoubleStream.of(percents).map(d -> d * allCount).iterator();

        Iterator<Map.Entry<Integer, Integer>> ents = m.entrySet().iterator();

        Map.Entry<Integer, Integer> ent = ents.next();
        int cc2 = ent.getValue();

        List<Integer> l = new ArrayList<>();
        while (percentValues.hasNext()) {
            double percentValue = percentValues.next();

            while (cc2 < percentValue && ents.hasNext()) {
                ent = ents.next();
                cc2 += ent.getValue();
            }
            l.add(ent.getKey());
        }
        int[] ret = l.stream().mapToInt(i -> i).toArray();

        return ret;
    }

    private static int[] counts2(Map<Integer, Integer> m, double[] percents) {
        if (m.isEmpty())
            return new int[percents.length];

        int allCount = m.values().stream().mapToInt(i -> i.intValue()).sum();

        Iterator<Map.Entry<Integer, Integer>> ents = m.entrySet().iterator();

        AtomicInteger count = new AtomicInteger(0);
        AtomicReference<Integer> keyHolder = new AtomicReference<>();
        {
            Map.Entry<Integer, Integer> ent = ents.next();
            keyHolder.set(ent.getKey());
            count.addAndGet(ent.getValue());
        }

        return DoubleStream.of(percents).mapToInt(d -> {
            double percentValue = d * allCount;
            while (count.get() < percentValue && ents.hasNext()) {
                Map.Entry<Integer, Integer> ent = ents.next();
                keyHolder.set(ent.getKey());
                count.addAndGet(ent.getValue());
            }

            return keyHolder.get();
        }).toArray();

    }

    private static int[] counts100(Map<Integer, Integer> m, double[] percents) {
        int[] arr = m.entrySet().stream() //
                .flatMapToInt(ent -> IntStream.range(0, ent.getValue()).map(i -> ent.getKey())) //
                .toArray();

        return DoubleStream.of(percents) //
                .mapToInt(d -> arr[Math.max(0, (int) (arr.length * d) - 1)]) //
                .toArray();
    }

    public static void main(String[] args) {

        double[] percents = { 1, 0.999, 0.99, 0.95, 0.9, 0.8, 0.75, 0.7, 0.5 };
        Arrays.sort(percents);

        Map<Integer, Integer> m = new TreeMap<>();
        {
            Random rnd = new Random(19781971);
            IntStream.range(1, 100).forEach(i -> m.put(i, rnd.nextInt(111)));
        }
        int[] cs1 = counts1(m, percents);
        int[] cs2 = counts2(m, percents);
        int[] cs100 = counts100(m, percents);

        for (int i = 0; i < percents.length; i++) {
            System.out.printf(" %, 3.3f  %, 5d  %, 5d  %, 5d  \n", percents[i], cs1[i], cs2[i], cs100[i]);
        }

        //        m.entrySet().forEach(System.out::println);
    }

}
