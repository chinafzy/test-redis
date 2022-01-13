package me.in1978.learn.spring.jedis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import redis.clients.jedis.Jedis;

@Component
public class Task1 {

//    @Autowired
//    JedisConnectionFactory connFactory;

//    @Autowired
    public void needTlp(StringRedisTemplate tpl) {
        Jedis j = null;
        
//        j.z
        
        System.out.println(tpl);
        tpl.opsForValue().get("123");
//        tpl.boundZSetOps("").ran

//        for (int i = 0; i < 100; i++) {
//
//            System.out.printf("%d: %s \n", i, connFactory.getConnection());
//        }

    }

}
