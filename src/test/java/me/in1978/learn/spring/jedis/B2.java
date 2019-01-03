package me.in1978.learn.spring.jedis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import redis.clients.jedis.ShardedJedis;
import redis.clients.jedis.ShardedJedisPool;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class B2 {

    @Autowired
    ShardedJedisPool pool;

    @Test
    public void run() {
        ShardedJedis jedis = pool.getResource();
        for (int i = 0; i < 100; i++) {
            jedis.set("k" + i, "1");
        }
    }

}
