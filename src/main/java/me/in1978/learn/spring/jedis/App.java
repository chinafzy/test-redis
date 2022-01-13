package me.in1978.learn.spring.jedis;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;

import redis.clients.jedis.commands.JedisCommands;
import redis.clients.jedis.util.Pool;

/**
 * Hello world!
 *
 */

@SpringBootApplication
public class App {
    public static void main(String[] args) {
        SpringApplication.run(App.class, args);
    }

    @Bean
    public JedisCommands x(ApplicationContext spring, TestConf testConf) {
        System.out.println("haha");
        return  (JedisCommands) spring
                .getBean(testConf.shardMode() ? Conf.CMDS_SHARD : Conf.CMDS_SINGLE, Pool.class).getResource();
    }

}
