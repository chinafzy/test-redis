package me.in1978.learn.spring.jedis;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import lombok.Data;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

//@Component
public class Conf {
    public static final String CMDS_SHARD = "jedis-pool-shard", CMDS_SINGLE = "jedis-pool-single";
}

@Component
@ConfigurationProperties("wtb.redis.single")
@Data
class RedisSingleConf {
    // redis://user:password@example.com:6379
    private String url = "redis://localhost:6379/";

    @Bean(Conf.CMDS_SINGLE)
    @ConditionalOnProperty(prefix = "wtb.redis.single", name = "url")
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {
        return new JedisPool(poolConfig, URI.create(url));
    }

}

@Component
@ConfigurationProperties("wtb.redis.shard")
@Data
class RedisShardConf {

    private List<String> urls = new ArrayList<>();

    @Bean(Conf.CMDS_SHARD)
    @ConditionalOnProperty(prefix = "wtb.redis.shard", name = "urls")
    public ShardedJedisPool shardedJedisPool(JedisPoolConfig poolConfig) {
        List<JedisShardInfo> infos = urls.stream().map(url -> new JedisShardInfo(URI.create(url))).collect(Collectors.toList());

        return new ShardedJedisPool(poolConfig, infos);
    }
}
