package me.in1978.learn.spring.jedis;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import redis.clients.jedis.commands.BasicCommands;
import redis.clients.jedis.commands.JedisCommands;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class GeoFillData {
    @Autowired
    JedisCommands cmds;

    @Test
    public void test() {
        IntStream.of(10_000, 100_000, 1000_000).forEach(this::fill);
        //        memory();
    }

    private void fill(int num) {

        long start = System.currentTimeMillis();

        Random rnd = new Random();
        String k = "test" + num;

        cmds.del(k);
        long mem1 = memory();
        IntStream.range(1, num + 1) //
                .parallel() //
                .mapToObj(i -> new float[] { (float) i, rnd.nextFloat() * 90, rnd.nextFloat() * 80 }) //
                .sequential() //
                .forEach(fs -> cmds.geoadd(k, fs[1], fs[2], "mem" + (int) fs[0])) //
        ;

        long used = System.currentTimeMillis() - start;
        long mem = memory() - mem1;
        System.out.printf("%s: Use %,dms to fill %,d geo points, takes up %,dM memory. \n", k, used, num, mem / 1024 / 1024);
    }

    private long memory() {
        String memory = ((BasicCommands) cmds).info("memory");
        //        System.out.println(memory);
        String used = memory.split("used_memory:")[1].split("[\r\n]")[0];
        return Long.parseLong(used);
    }
}
