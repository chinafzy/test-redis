package me.in1978.learn.spring.jedis;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class PeriodPrinter implements Counter.Notifier {
    final int step;

    long last, start;

    @Override
    public void notify(long count) {
        if (count % step != 0)
            return;

        long now = System.currentTimeMillis();
        long used = now - last, allUsed = now - start;
        System.out.printf("%, 11d %, 10d %, 10d %, 11d %, 11d \n", //
                count, used, allUsed, step * 1000 / used, count * 1000 / allUsed);
        last = now;
    }

    public void printHeader() {
        System.out.printf("%11s %10s %10s %11s %11s \n", "Number", "Time", "All Time", "Period QPS", "Global QPS");
    }

    public void reset() {
//        System.out.println("PeriodPrinter reset.");
        start = last = System.currentTimeMillis();
    }

}
