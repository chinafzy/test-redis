package me.in1978.learn.spring.jedis;

import java.util.concurrent.CountDownLatch;
import java.util.function.IntConsumer;
import java.util.function.LongUnaryOperator;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public abstract class TestTask implements Runnable {

    /**
     * Called on task is ready for next work.
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

        test();

        tearDown();
    }

    protected void prepare() {
    }

    protected abstract void test();

    protected void tearDown() {
    }

}
