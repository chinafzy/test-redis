package me.in1978.learn.spring.jedis;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.Data;
import redis.clients.jedis.JedisPoolConfig;

@ConfigurationProperties("wtb.redis.pool")
@Data
@Component
public class JedisPoolConfigConf {

    private int maxIdle;
    private int minIdle;
    private int maxTotal;
    private long maxWaitMillis;
    private long minEvictableIdleTimeMillis;

    @Bean
    JedisPoolConfig poolConfig() {
        JedisPoolConfig ret = new JedisPoolConfig();
        if (maxIdle > 0)
            ret.setMaxIdle(maxIdle);
        if (minIdle > 0)
            ret.setMinIdle(minIdle);
        if (maxTotal > 0)
            ret.setMaxTotal(maxTotal);
        if (maxWaitMillis > 0)
            ret.setMaxWaitMillis(maxWaitMillis);
        if (minEvictableIdleTimeMillis > 0)
            ret.setMinEvictableIdleTimeMillis(minEvictableIdleTimeMillis);

        return ret;
    }

}
