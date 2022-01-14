package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MakeIt {

    final int c;
    final long n;

    final Runnable task;

    ExecutorService executor;

    ManySpeeders speeders = new ManySpeeders();
    AtomicLong sucCount = new AtomicLong(), failCount = new AtomicLong(), allCount = new AtomicLong();

    TaskAllocator taskAllocator;

    public MakeIt(int c, long n, Runnable task) {
        this.c    = c;
        this.n    = n;
        this.task = task;

        taskAllocator = new TaskAllocator(n, c);

        executor = Executors.newFixedThreadPool(c);
    }

    public void run() {

        for (int i = 0; i < c; i++) {
            executor.submit(() -> {
                while (true) {
                    long[] range = taskAllocator.allocate();
                    if (range == null) {
                        log.debug("No more tasks. existing.");
                        return;
                    }

                    for (long l = range[0]; l < range[1]; l++) {
                        do1(l);
                    }
                }
            });
        }

    }

    private void do1(long l) {
        long stamp1 = System.currentTimeMillis();
        try {
            task.run();
            long used = System.currentTimeMillis() - stamp1;
            speeders.onThread().record((int) used);
        } catch (Throwable tr) {
            log.error("fail on task", tr);
        }
    }

}

