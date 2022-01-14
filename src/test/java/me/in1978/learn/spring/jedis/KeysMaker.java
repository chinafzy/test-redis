package me.in1978.learn.spring.jedis;

import java.util.SplittableRandom;
import java.util.stream.LongStream;
import java.util.stream.Stream;

public class KeysMaker {

    public static Stream<String> make(String prefix, long startInclusive, long endExclusive) {
        String format = prefix + "%09d";
        return LongStream.range(startInclusive, endExclusive).mapToObj(i -> String.format(format, i));
    }

    private static final SplittableRandom rnd = new SplittableRandom();

    public static String onKey(String prefix, long startInclusive, long endExclusive) {
        return String.format(prefix + "%09d", rnd.nextLong(startInclusive, endExclusive));
    }

}
