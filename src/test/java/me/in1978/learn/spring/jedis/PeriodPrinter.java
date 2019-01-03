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
        System.out.printf("%, 10d -> Use Time: %, 7d / %, 7d ; QPS: %, 9d / %, 9d \n", //
                count, used, allUsed, step * 1000 / used, count * 1000 / allUsed);
        last = now;
    }

    public void reset() {
        System.out.println("PeriodPrinter reset.");
        start = last = System.currentTimeMillis();
    }

}
