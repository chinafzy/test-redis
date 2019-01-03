package me.in1978.learn.spring.jedis;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties("wtb.test")
@Component
@Data
public class TestConf {

    @ConfigurationProperties("wtb.redis.shard")
    @Component
    @Data
    // a helpful skill for injecting multiple values by @ConfigurationProperties since @Value does not work perfectly here.
    private static class SkillA {
        List<String> urls;
    }

    private int number = 300_000;
    private int concurrency = 3;
    private int valueSize = 1024;

    private int summaryStep = 100_000;

    @Value("${wtb.redis.single.url:}")
    private String singleUrl;

    @Autowired
    private SkillA a;

    public boolean shardMode() {
        return a.urls != null && !a.urls.isEmpty();
    }

    public List<String> getShardUrls() {
        return a.getUrls();
    }

}