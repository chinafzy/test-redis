package me.in1978.learn.spring.jedis;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongConsumer;

public class Counter {

    //    public static interface Notifier {
    //        void notify(long value);
    //    }

    private final AtomicLong count = new AtomicLong();

    private CopyOnWriteArrayList<LongConsumer> notifiers = new CopyOnWriteArrayList<>();

    public Counter addNotifier(LongConsumer notifier) {
        notifiers.add(notifier);

        return this;
    }

    /**
     * 
     * @param value
     * @return
     * @see AtomicLong#addAndGet(long)
     */
    public long increase(long value) {
        long ret = count.addAndGet(value);
        for (LongConsumer notifier : notifiers) {
            notifier.accept(ret);
        }

        return ret;
    }
}
