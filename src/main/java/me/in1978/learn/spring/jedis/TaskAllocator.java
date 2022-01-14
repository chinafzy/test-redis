package me.in1978.learn.spring.jedis;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TaskAllocator {
    // inputs
    final long n;
    final int c;

    // 
    final long pages;
    final Object lock = new Object();
    long pos;

    public TaskAllocator(long n, int c) {
        this.c     = c;
        this.n     = n;
        this.pages = c * 10;
    }

    public long remained() {
        return n - pos - 1;
    }

    public long[] allocate() {
        synchronized (lock) {
            long remained = remained();
            if (remained == 0) {
                log.debug("no more tasks.");
                return null;
            } else if (remained < 0) {
                log.warn(String.format("oversold: %,d / %,d ", pos, n));
                return null;
            }

            // 1 <= size <= 10000
            long size = Math.max(1, Math.min(remained / pages, 10000));
            long[] ret = { pos, pos + size };
            pos += size;

            log.debug("");
            return ret;
        }
    }

    public static void main(String[] args) throws InterruptedException {
        int c = 10;
        long n = 100_000;

        TaskAllocator allocator = new TaskAllocator(n, c);
        ExecutorService executor = Executors.newFixedThreadPool(c);
        Random rnd = new Random();
        for (int i = 0; i < c; i++) {
            executor.submit(() -> {
                long[] range;
                while ((range = allocator.allocate()) != null) {
                    log.info("get tasks [{}, {})", range[0], range[1]);

                    try {
                        Thread.sleep(rnd.nextInt(1000));
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                log.info("done");
            });
        }

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.DAYS);
    }
}