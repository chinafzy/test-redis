package org.wtb.learn.spring.jedis;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import lombok.Data;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.JedisShardInfo;
import redis.clients.jedis.ShardedJedisPool;

@Component
public class Conf {
    public static final String CMDS_SHARD = "jedis-shard", CMDS_SINGLE = "jedis-single";
}

@Component
@ConfigurationProperties("wtb.redis.single")
@Data
class RedisSingleConf {
    // redis://user:password@example.com:6379
    private String url = "redis://localhost:6379/";
    private String host = "localhost";
    private int port = 6379;

    public void adjustFromUrl() {
        if (StringUtils.isEmpty(url))
            return;

        URI uri = URI.create(url);
        host = uri.getHost();
        port = uri.getPort();

    }

    @Bean(Conf.CMDS_SINGLE)
    public JedisPool jedisPool(JedisPoolConfig poolConfig) {
        //        adjustFromUrl();

        return new JedisPool(poolConfig, URI.create(url));
    }

}

@Component
@ConfigurationProperties("wtb.redis.shard")
@Data
class RedisShardConf {

    private List<String> urls = new ArrayList<>();

    @Bean(Conf.CMDS_SHARD)
    public ShardedJedisPool shardedJedisPool(JedisPoolConfig poolConfig) {
        List<JedisShardInfo> infos = urls.stream().map(url -> new JedisShardInfo(URI.create(url))).collect(Collectors.toList());

        return new ShardedJedisPool(poolConfig, infos);
    }
}
