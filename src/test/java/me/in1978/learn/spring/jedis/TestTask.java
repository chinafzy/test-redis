package me.in1978.learn.spring.jedis;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntConsumer;
import java.util.function.LongUnaryOperator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TestTask implements Runnable {

    /**
     * Called when task is ready for next work.
     */
    final protected Runnable readyNotifier;
    /**
     * KickOff signal waiting for outing call.
     */
    final protected CountDownLatch kickOff;

    final protected IntConsumer speeder;

    final protected LongUnaryOperator successCount, failCount;

    @Override
    public void run() {

        prepare();

        readyNotifier.run();

        try {
            kickOff.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            return;
        }

        try {
            test();
        } finally {
            tearDown();
        }
    }

    protected void testOnce(Runnable run) {
        long stampx = System.currentTimeMillis();

        try {
            int used = (int) (System.currentTimeMillis() - stampx);
            speeder.accept(used);

            successCount.applyAsLong(1);
        } catch (Throwable tr) {
            tr.printStackTrace();
            failCount.applyAsLong(1);
        }
    }

    protected void prepare() {
    }

    protected abstract void test();

    protected void tearDown() {
    }

}
