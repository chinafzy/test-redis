//package me.in1978.learn.spring.jedis;
//
//import java.util.ArrayList;
//import java.util.List;
//import java.util.function.Supplier;
//
//import lombok.Getter;
//
//public class TlHelper<T> {
//
//    @Getter
//    private List<T> members = new ArrayList<>();
//    private List<ThreadLocal<T>> tls = new ArrayList<>();
//
//    private final Supplier<? extends T> supplier;
//
//    private TlHelper(Supplier<? extends T> supplier) {
//        this.supplier = supplier;
//    }
//
//    public static <T> TlHelper<T> ins(Supplier<? extends T> supplier) {
//        return new TlHelper<>(supplier);
//    }
//
//    public ThreadLocal<T> onThread() {
//        ThreadLocal<T> tl = ThreadLocal.withInitial(() -> {
//            var ret = supplier.get();
//            members.add(ret);
//            return ret;
//        });
//        tls.add(tl);
//
//        return tl;
//    }
//
//    public void clearTls() {
//        tls.forEach(ThreadLocal::remove);
//    }
//
//}
