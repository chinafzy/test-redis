package me.in1978.learn.spring.jedis;

import java.util.stream.LongStream;
import java.util.stream.Stream;

public class KeysMaker {

    public static Stream<String> make(String prefix, long startInclusive, long endExclusive) {
        String format = prefix + "%09d";
        return LongStream.range(startInclusive, endExclusive).mapToObj(i -> String.format(format, i));
    }

}
