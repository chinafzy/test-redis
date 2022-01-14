package me.in1978.learn.spring.jedis;

import java.util.ArrayList;
import java.util.List;

public class ManySpeeders {

    private List<Speeder> list = new ArrayList<>();

    private ThreadLocal<Speeder> holder = ThreadLocal.withInitial(() -> {
        Speeder ret = new Speeder();
        list.add(ret);
        return ret;
    });

    public Speeder merged() {
        return Speeder.merge(list);
    }

    public Speeder onThread() {
        return holder.get();
    }
}
