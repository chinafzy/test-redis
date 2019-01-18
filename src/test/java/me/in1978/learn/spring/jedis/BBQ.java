package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 
 * @author zeyufang
 *
 * @param <T>
 */
public class BBQ<T> implements BlockingQueue<T> {

    private static interface InterruptedSupplier<T> {
        T get() throws InterruptedException;
    }

    private static final Object FLAG = new Object();
    private static Predicate<Object> p_notFlag = o -> o != FLAG;

    public static <T> BBQ<T> fromIterable(Iterable<T> it, int poolSize) {
        return new BBQ<>(it, poolSize);
    }

    public static <T> BBQ<T> fromIterator(Iterator<T> itr, int poolSize) {
        return fromIterable(Util.iterable(itr), poolSize);
    }

    public static <T> BBQ<T> fromStream(Stream<T> stream, int poolSize) {
        return fromIterator(stream.iterator(), poolSize);
    }

    private LinkedBlockingQueue<Object> inner;
    private Iterable<T> src;

    private AtomicBoolean stopped = new AtomicBoolean(false);

    private BBQ(Iterable<T> src, int poolSize) {
        this.src = src;
        this.inner = new LinkedBlockingQueue<>(poolSize);

        new Thread(() -> {
            for (T t : this.src) {
                if (stopped.get())
                    break;

                try {
                    inner.put(t);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

            for (int i = 0; i < poolSize && !stopped.get(); i++) {
                try {
                    inner.put(FLAG);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    throw new RuntimeException(e);
                }
            }

        }).start();
    }

    @Override
    public boolean add(T e) {
        return inner.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        return inner.addAll(c);
    }

    @Override
    public void clear() {
        inner.clear();
    }

    @Override
    public boolean contains(Object o) {
        return inner.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return inner.containsAll(c);
    }

    // TODO
    public void destroy() {
        // mark as stopped
        stopped.set(true);

        // clear all 
        try {
            while (inner.poll(1, TimeUnit.SECONDS) != null) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // fill in with stop_flag
        try {
            while (inner.offer(FLAG, 1, TimeUnit.SECONDS)) {
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    public int drainTo(Collection<? super T> c) {
        return drainTo(c, Integer.MAX_VALUE);
    }

    @Override
    public int drainTo(Collection<? super T> c, int maxElements) {
        ArrayList<Object> l = new ArrayList<>();
        inner.drainTo(l, maxElements);

        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<T> l2 = (List) l.stream().filter(p_notFlag).collect(Collectors.toList());
        c.addAll(l2);

        return l2.size();
    }

    @Override
    public T element() {
        return retrieve(this::element);
    }

    @Override
    public boolean isEmpty() {
        return inner.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        Iterator<Object> itr = inner.iterator();
        return new Iterator<T>() {
            T obj;

            @SuppressWarnings("unchecked")
            @Override
            public boolean hasNext() {
                if (obj != null)
                    return true;

                if (!itr.hasNext())
                    return true;

                Object o = itr.next();
                if (o == FLAG)
                    return false;

                obj = (T) o;
                return true;
            }

            @Override
            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();

                try {
                    return obj;
                } finally {
                    obj = null;
                }
            }
        };
    }

    @Override
    public boolean offer(T e) {
        return inner.offer(e);
    }

    @Override
    public boolean offer(T e, long timeout, TimeUnit unit) throws InterruptedException {
        return inner.offer(e, timeout, unit);
    }

    @Override
    public T peek() {
        return retrieve(this::peek);
    }

    @Override
    public T poll() {
        return retrieve(this::poll);
    }

    @Override
    public T poll(long timeout, TimeUnit unit) throws InterruptedException {
        return retrieveInterruptly(() -> inner.poll(timeout, unit));
    }

    @Override
    public void put(T e) throws InterruptedException {
        inner.put(e);
    }

    @Override
    public int remainingCapacity() {
        return inner.remainingCapacity();
    }

    @Override
    public T remove() {
        return retrieve(this::remove);
    }

    @Override
    public boolean remove(Object o) {
        return inner.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return inner.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return inner.retainAll(c);
    }

    @SuppressWarnings("unchecked")
    private T retrieve(Supplier<T> supplier) {
        if (stopped.get())
            return null;

        Object ret = inner.element();

        if (ret == FLAG)
            return null;

        return (T) ret;
    }

    @SuppressWarnings("unchecked")
    private T retrieveInterruptly(InterruptedSupplier<Object> supplier) {
        if (stopped.get())
            return null;

        Object ret = inner.element();

        if (ret == FLAG)
            return null;

        return (T) ret;
    }

    @Override
    public int size() {
        return inner.size();
    }

    @Override
    public T take() throws InterruptedException {
        return retrieveInterruptly(() -> inner.take());
    }

    @Override
    public Object[] toArray() {
        return inner.stream().filter(p_notFlag).toArray();
    }

    @SuppressWarnings("hiding")
    @Override
    public <T> T[] toArray(T[] a) {
        return inner.stream().filter(p_notFlag).collect(Collectors.toList()).toArray(a);
    }

}
