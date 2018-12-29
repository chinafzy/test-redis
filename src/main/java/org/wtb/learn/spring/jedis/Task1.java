package org.wtb.learn.spring.jedis;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class Task1 {

//    @Autowired
//    JedisConnectionFactory connFactory;

//    @Autowired
    public void needTlp(StringRedisTemplate tpl) {
        System.out.println(tpl);
        tpl.opsForValue().get("123");

//        for (int i = 0; i < 100; i++) {
//
//            System.out.printf("%d: %s \n", i, connFactory.getConnection());
//        }

    }

}
