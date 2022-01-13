package me.in1978.learn.spring.jedis;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.junit4.SpringRunner;

import redis.clients.jedis.JedisCommands;
import redis.clients.util.Pool;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class T2 {

    @Autowired
    private ApplicationContext spring;
    @Autowired
    private TestConf testConf;

    @Test
    public void run() throws InterruptedException {
        final JedisCommands cmds = (JedisCommands) spring
                .getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class).getResource();

        for (int i = 0; i < 2; i++) {
            System.out.println(cmds.set("a", "2"));
        }
    }
}
