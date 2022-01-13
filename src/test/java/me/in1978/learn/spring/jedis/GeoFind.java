package me.in1978.learn.spring.jedis;

import java.util.Random;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import redis.clients.jedis.BasicCommands;
import redis.clients.jedis.GeoUnit;
import redis.clients.jedis.JedisCommands;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class GeoFind {
    @Autowired
    JedisCommands cmds;

    @Test
    public void test() {

        IntStream.of(100_000, 200_000, 500_000, 1000_000).forEach(keyNum -> //
        IntStream.of(10_000).forEach(queryNum -> //
        IntStream.of(10, 20, 50, 100, 200, 300).forEach(dist -> test(keyNum, queryNum, dist))));

    }

    private void test(int keyNum, int queryNum, int dist) {
        fillIfNot(keyNum);

        long stamp1 = System.currentTimeMillis();
        String k = "test" + keyNum;

        IntStream.range(1, queryNum + 1) //
                .forEach(i -> cmds.georadiusByMember(k, "mem" + (i % keyNum), dist, GeoUnit.KM));

        long used = System.currentTimeMillis() - stamp1;
        System.out.printf("Use %,d ms for %,d calls in %,d points with distance of %,d KM. \n", used, queryNum, keyNum, dist);
    }

    private void fillIfNot(int num) {
        String k = "test" + num;
        if (cmds.exists(k)) {
            return;
        }

        System.out.printf("Preparing %,d points. \n", num);

        long start = System.currentTimeMillis();

        Random rnd = new Random();

        long mem1 = memory();
        // 北起黑龙bai江省漠河以北的黑龙du江主航道的中心线北纬zhi53°31′
        //  海南岛大概是20  [25-50]
        // 西起新疆维吾尔自治区乌恰县以西的帕米尔高原东经73°
        //  东至黑龙江省抚远县境内的黑龙江与乌苏里江汇合处东经135°。 [75-150]
        IntStream.range(1, num + 1) //
                .parallel() //
                .mapToObj(i -> new float[] { (float) i, rnd.nextFloat() * 75 + 75, rnd.nextFloat() * 25 + 25 }) //
                .sequential() //
                .forEach(fs -> cmds.geoadd(k, fs[1], fs[2], "mem" + (int) fs[0])) //
        ;

        long used = System.currentTimeMillis() - start;
        long mem = memory() - mem1;
        System.out.printf("%s: Use %,dms to fill %,d geo points, takes up %,dM memory. \n", k, used, num, mem / 1024 / 1024);
    }

    private long memory() {
        String memory = ((BasicCommands) cmds).info("memory");
        String used = memory.split("used_memory:")[1].split("[\r\n]")[0];
        return Long.parseLong(used);
    }
}
