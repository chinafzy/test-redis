package me.in1978.learn.spring.jedis;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class Counter {

    public static interface Notifier {
        void notify(long value);
    }

    private final AtomicLong count = new AtomicLong();

    private CopyOnWriteArrayList<Notifier> notifiers = new CopyOnWriteArrayList<>();

    public Counter addNotifier(Notifier notifier) {
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
        for (Notifier notifier : notifiers) {
            notifier.notify(ret);
        }
        return ret;
    }
}
