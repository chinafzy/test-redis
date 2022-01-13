package me.in1978.learn.spring.jedis;

public class BitDo {

    public static void main(String[] args) {

    }

    static long v(double x, double y) {
        long lx = v(x), ly = v(y);

        long ret = 0;
        while (lx > 0 || ly > 0) {
            ret = ((ret << 1 | (lx % 2)) << 1) | (ly % 2);
            lx /= 2;
            ly /= 2;
        }

        return ret;
    }

    static long v(double d) {
        return (long) (1_000_000l * d);
    }

}
