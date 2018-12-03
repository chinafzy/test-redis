package org.wtb.learn.spring.jedis;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest()
public class Benchmark {
    @Autowired
    JedisConnectionFactory connFactory;

    @Autowired
    StringRedisTemplate tpl;

    String[] arr = {
            "{\"lineStops\":[{\"lineNo\":\"机场专4\",\"timeRanges\":[{\"days\":8,\"time\":\"8-1\"}],\"lineId\":\"022-机场专4-0\",\"lineName\":\"机场专线4\",\"stationName\":\"中北里\",\"direction\":0,\"order\":16},{\"lineNo\":\"842\",\"timeRanges\":[{\"days\":7,\"time\":\"8-1\"}],\"lineId\":\"022-842-1\",\"lineName\":\"842\",\"stationName\":\"中北里\",\"direction\":1,\"order\":9},{\"lineNo\":\"902\",\"timeRanges\":[{\"days\":7,\"time\":\"8-1\"}],\"lineId\":\"022-902-1\",\"lineName\":\"902\",\"stationName\":\"中北里\",\"direction\":1,\"order\":10},{\"lineNo\":\"902\",\"timeRanges\":[{\"days\":6,\"time\":\"8-1\"}],\"lineId\":\"022-902-0\",\"lineName\":\"902\",\"stationName\":\"小白楼\",\"direction\":0,\"order\":15},{\"lineNo\":\"842\",\"timeRanges\":[{\"days\":6,\"time\":\"8-1\"}],\"lineId\":\"022-842-0\",\"lineName\":\"842\",\"stationName\":\"小白楼\",\"direction\":0,\"order\":17}],\"udid\":\"d309cc5ef79afb1fa80bbfdea37b547333a78ce1\"}",
            "{\"lineStops\":[{\"lineNo\":\"609\",\"timeRanges\":[{\"days\":8,\"time\":\"6-4\"}],\"lineId\":\"022-609-1\",\"lineName\":\"609\",\"stationName\":\"北站\",\"direction\":1,\"order\":12},{\"lineNo\":\"903\",\"timeRanges\":[{\"days\":8,\"time\":\"6-4\"}],\"lineId\":\"022-903-1\",\"lineName\":\"903\",\"stationName\":\"建昌道\",\"direction\":1,\"order\":3},{\"lineNo\":\"609\",\"timeRanges\":[{\"days\":7,\"time\":\"6-2\"}],\"lineId\":\"022-609-1\",\"lineName\":\"609\",\"stationName\":\"赵沽里\",\"direction\":1,\"order\":4},{\"lineNo\":\"609\",\"timeRanges\":[{\"days\":13,\"time\":\"6-2\"},{\"days\":17,\"time\":\"6-3\"}],\"lineId\":\"022-609-1\",\"lineName\":\"609\",\"stationName\":\"丹江里\",\"direction\":1,\"order\":3},{\"lineNo\":\"903\",\"timeRanges\":[{\"days\":12,\"time\":\"7-1\"},{\"days\":18,\"time\":\"6-4\"}],\"lineId\":\"022-903-1\",\"lineName\":\"903\",\"stationName\":\"北站\",\"direction\":1,\"order\":7},{\"lineNo\":\"903\",\"timeRanges\":[{\"days\":11,\"time\":\"6-4\"}],\"lineId\":\"022-903-1\",\"lineName\":\"903\",\"stationName\":\"北宁公园\",\"direction\":1,\"order\":6},{\"lineNo\":\"903\",\"timeRanges\":[{\"days\":10,\"time\":\"6-4\"}],\"lineId\":\"022-903-1\",\"lineName\":\"903\",\"stationName\":\"北站体育场\",\"direction\":1,\"order\":5},{\"lineNo\":\"903\",\"timeRanges\":[{\"days\":7,\"time\":\"6-4\"}],\"lineId\":\"022-903-1\",\"lineName\":\"903\",\"stationName\":\"天津服装城\",\"direction\":1,\"order\":4}],\"udid\":\"3fb2ae55-2fb8-4be6-b8b3-18ef6472981a\"}",
            "{\"lineStops\":[{\"lineNo\":\"904\",\"timeRanges\":[{\"days\":16,\"time\":\"7-2\"}],\"lineId\":\"022-904-1\",\"lineName\":\"904\",\"stationName\":\"蓉芳里\",\"direction\":1,\"order\":12},{\"lineNo\":\"954\",\"timeRanges\":[{\"days\":6,\"time\":\"7-4\"}],\"lineId\":\"022-954-1\",\"lineName\":\"954\",\"stationName\":\"外国语大学\",\"direction\":1,\"order\":17},{\"lineNo\":\"954\",\"timeRanges\":[{\"days\":6,\"time\":\"7-4\"}],\"lineId\":\"022-954-1\",\"lineName\":\"954\",\"stationName\":\"重庆道\",\"direction\":1,\"order\":16},{\"lineNo\":\"619\",\"timeRanges\":[{\"days\":12,\"time\":\"7-2\"},{\"days\":9,\"time\":\"7-3\"}],\"lineId\":\"022-619-1\",\"lineName\":\"619\",\"stationName\":\"蓉芳里\",\"direction\":1,\"order\":26}],\"udid\":\"920bb979-3905-470f-881b-3ccf80c1d37d\"}",
            "{\"lineStops\":[{\"lineNo\":\"639东线\",\"timeRanges\":[{\"days\":9,\"time\":\"7-1\"}],\"lineId\":\"0022114434168\",\"lineName\":\"639东线\",\"stationName\":\"昆仑里\",\"direction\":0,\"order\":2},{\"lineNo\":\"639东线\",\"timeRanges\":[{\"days\":6,\"time\":\"7-1\"}],\"lineId\":\"0022114434168\",\"lineName\":\"639东线\",\"stationName\":\"增兴窑\",\"direction\":0,\"order\":1},{\"lineNo\":\"639东线\",\"timeRanges\":[{\"days\":6,\"time\":\"7-1\"}],\"lineId\":\"0022114434168\",\"lineName\":\"639东线\",\"stationName\":\"万新公寓\",\"direction\":0,\"order\":4},{\"lineNo\":\"639东线\",\"timeRanges\":[{\"days\":7,\"time\":\"7-1\"}],\"lineId\":\"0022114434168\",\"lineName\":\"639东线\",\"stationName\":\"天山南路\",\"direction\":0,\"order\":3},{\"lineNo\":\"706\",\"timeRanges\":[{\"days\":10,\"time\":\"7-1\"}],\"lineId\":\"022-706-0\",\"lineName\":\"706\",\"stationName\":\"昆仑里\",\"direction\":0,\"order\":45}],\"udid\":\"a451fd3b-2963-4b1e-b892-16fd560c7ee9\"}",
            "{\"lineStops\":[{\"lineNo\":\"639西线\",\"timeRanges\":[{\"days\":8,\"time\":\"8-1\"},{\"days\":8,\"time\":\"7-4\"}],\"lineId\":\"0022114431304\",\"lineName\":\"639西线\",\"stationName\":\"五十一中学\",\"direction\":1,\"order\":12},{\"lineNo\":\"153\",\"timeRanges\":[{\"days\":9,\"time\":\"7-1\"},{\"days\":8,\"time\":\"7-2\"}],\"lineId\":\"022-153-1\",\"lineName\":\"153\",\"stationName\":\"杨庄子\",\"direction\":1,\"order\":17},{\"lineNo\":\"153\",\"timeRanges\":[{\"days\":7,\"time\":\"8-1\"},{\"days\":6,\"time\":\"7-4\"}],\"lineId\":\"022-153-0\",\"lineName\":\"153\",\"stationName\":\"五十一中学\",\"direction\":0,\"order\":3},{\"lineNo\":\"669\",\"timeRanges\":[{\"days\":9,\"time\":\"7-1\"},{\"days\":9,\"time\":\"6-4\"}],\"lineId\":\"022-669-1\",\"lineName\":\"669\",\"stationName\":\"杨庄子\",\"direction\":1,\"order\":24},{\"lineNo\":\"639西线\",\"timeRanges\":[{\"days\":6,\"time\":\"7-1\"},{\"days\":8,\"time\":\"7-2\"}],\"lineId\":\"0022114431293\",\"lineName\":\"639西线\",\"stationName\":\"西横堤\",\"direction\":0,\"order\":11},{\"lineNo\":\"639西线\",\"timeRanges\":[{\"days\":10,\"time\":\"7-1\"},{\"days\":8,\"time\":\"7-2\"},{\"days\":6,\"time\":\"6-4\"}],\"lineId\":\"0022114431293\",\"lineName\":\"639西线\",\"stationName\":\"杨庄子\",\"direction\":0,\"order\":12}],\"udid\":\"6d2580bf-04b2-4dd6-ae4e-0fdebf50a060\"}",
            "{\"lineStops\":[{\"lineNo\":\"801\",\"timeRanges\":[{\"days\":7,\"time\":\"8-3\"}],\"lineId\":\"022-801-1\",\"lineName\":\"801\",\"stationName\":\"丁字沽\",\"direction\":1,\"order\":34},{\"lineNo\":\"801\",\"timeRanges\":[{\"days\":15,\"time\":\"8-3\"}],\"lineId\":\"022-801-1\",\"lineName\":\"801\",\"stationName\":\"西沽\",\"direction\":1,\"order\":35},{\"lineNo\":\"37\",\"timeRanges\":[{\"days\":6,\"time\":\"8-3\"},{\"days\":12,\"time\":\"8-4\"}],\"lineId\":\"022-37-1\",\"lineName\":\"37\",\"stationName\":\"北门\",\"direction\":1,\"order\":25},{\"lineNo\":\"801\",\"timeRanges\":[{\"days\":6,\"time\":\"8-3\"}],\"lineId\":\"022-801-1\",\"lineName\":\"801\",\"stationName\":\"丁字沽四段\",\"direction\":1,\"order\":33},{\"lineNo\":\"5\",\"timeRanges\":[{\"days\":14,\"time\":\"8-3\"}],\"lineId\":\"022-5-1\",\"lineName\":\"5\",\"stationName\":\"西沽\",\"direction\":1,\"order\":2},{\"lineNo\":\"824\",\"timeRanges\":[{\"days\":7,\"time\":\"8-3\"},{\"days\":11,\"time\":\"8-4\"}],\"lineId\":\"022-824-1\",\"lineName\":\"824\",\"stationName\":\"北门\",\"direction\":1,\"order\":36},{\"lineNo\":\"24\",\"timeRanges\":[{\"days\":9,\"time\":\"8-4\"}],\"lineId\":\"022-24-1\",\"lineName\":\"24\",\"stationName\":\"北门\",\"direction\":1,\"order\":4}],\"udid\":\"7b4d2981f0abac78fecef9f39119ad5683c116c9\"}",
            "{\"lineStops\":[{\"lineNo\":\"34\",\"timeRanges\":[{\"days\":7,\"time\":\"6-4\"}],\"lineId\":\"022-34-1\",\"lineName\":\"34\",\"stationName\":\"瑞景新苑地铁站\",\"direction\":1,\"order\":5},{\"lineNo\":\"34\",\"timeRanges\":[{\"days\":9,\"time\":\"6-4\"}],\"lineId\":\"022-34-1\",\"lineName\":\"34\",\"stationName\":\"辰旺路\",\"direction\":1,\"order\":4},{\"lineNo\":\"34\",\"timeRanges\":[{\"days\":16,\"time\":\"6-4\"}],\"lineId\":\"022-34-1\",\"lineName\":\"34\",\"stationName\":\"瑞益园\",\"direction\":1,\"order\":3},{\"lineNo\":\"34\",\"timeRanges\":[{\"days\":13,\"time\":\"6-4\"}],\"lineId\":\"022-34-1\",\"lineName\":\"34\",\"stationName\":\"紫瑞园\",\"direction\":1,\"order\":2},{\"lineNo\":\"34\",\"timeRanges\":[{\"days\":9,\"time\":\"6-4\"}],\"lineId\":\"022-34-1\",\"lineName\":\"34\",\"stationName\":\"刘园公交站\",\"direction\":1,\"order\":1}],\"udid\":\"24232634-c4fd-4de5-bd22-a10c12c23563\"}",
            "{\"lineStops\":[{\"lineNo\":\"904\",\"timeRanges\":[{\"days\":16,\"time\":\"7-2\"}],\"lineId\":\"022-904-1\",\"lineName\":\"904\",\"stationName\":\"蓉芳里\",\"direction\":1,\"order\":12},{\"lineNo\":\"954\",\"timeRanges\":[{\"days\":6,\"time\":\"7-4\"}],\"lineId\":\"022-954-1\",\"lineName\":\"954\",\"stationName\":\"外国语大学\",\"direction\":1,\"order\":17},{\"lineNo\":\"954\",\"timeRanges\":[{\"days\":6,\"time\":\"7-4\"}],\"lineId\":\"022-954-1\",\"lineName\":\"954\",\"stationName\":\"重庆道\",\"direction\":1,\"order\":16},{\"lineNo\":\"619\",\"timeRanges\":[{\"days\":12,\"time\":\"7-2\"},{\"days\":9,\"time\":\"7-3\"}],\"lineId\":\"022-619-1\",\"lineName\":\"619\",\"stationName\":\"蓉芳里\",\"direction\":1,\"order\":26}],\"udid\":\"920bb979-3905-470f-881b-3ccf80c1d37d\"}",
            "{\"lineStops\":[{\"lineNo\":\"观2\",\"timeRanges\":[{\"days\":11,\"time\":\"6-4\"}],\"lineId\":\"022-观2-1\",\"lineName\":\"观光2\",\"stationName\":\"王串场六号路\",\"direction\":1,\"order\":4},{\"lineNo\":\"653\",\"timeRanges\":[{\"days\":8,\"time\":\"6-3\"}],\"lineId\":\"022-653-0\",\"lineName\":\"653\",\"stationName\":\"王串场六号路\",\"direction\":0,\"order\":21},{\"lineNo\":\"907\",\"timeRanges\":[{\"days\":8,\"time\":\"6-3\"},{\"days\":6,\"time\":\"6-4\"}],\"lineId\":\"022-907-0\",\"lineName\":\"907\",\"stationName\":\"王串场六号路\",\"direction\":0,\"order\":14},{\"lineNo\":\"856\",\"timeRanges\":[{\"days\":6,\"time\":\"6-4\"}],\"lineId\":\"022-856-0\",\"lineName\":\"856\",\"stationName\":\"小树林\",\"direction\":0,\"order\":27},{\"lineNo\":\"824\",\"timeRanges\":[{\"days\":6,\"time\":\"7-2\"}],\"lineId\":\"022-824-0\",\"lineName\":\"824\",\"stationName\":\"柳馨园\",\"direction\":0,\"order\":37},{\"lineNo\":\"824\",\"timeRanges\":[{\"days\":16,\"time\":\"7-1\"},{\"days\":14,\"time\":\"7-2\"}],\"lineId\":\"022-824-0\",\"lineName\":\"824\",\"stationName\":\"北门\",\"direction\":0,\"order\":8},{\"lineNo\":\"824\",\"timeRanges\":[{\"days\":6,\"time\":\"7-1\"}],\"lineId\":\"022-824-0\",\"lineName\":\"824\",\"stationName\":\"城厢东路\",\"direction\":0,\"order\":7},{\"lineNo\":\"观2\",\"timeRanges\":[{\"days\":6,\"time\":\"6-4\"}],\"lineId\":\"022-观2-1\",\"lineName\":\"观光2\",\"stationName\":\"秀山花园\",\"direction\":1,\"order\":3}],\"udid\":\"8cb447d062aa97451cfacb3bc6ddf5c057fa37a5\"}",
            "{\"lineStops\":[{\"lineNo\":\"862\",\"timeRanges\":[{\"days\":6,\"time\":\"7-2\"}],\"lineId\":\"022-862-1\",\"lineName\":\"862\",\"stationName\":\"天和医院\",\"direction\":1,\"order\":15},{\"lineNo\":\"665\",\"timeRanges\":[{\"days\":6,\"time\":\"7-1\"}],\"lineId\":\"022-665-1\",\"lineName\":\"665\",\"stationName\":\"河东体育场\",\"direction\":1,\"order\":12},{\"lineNo\":\"665\",\"timeRanges\":[{\"days\":12,\"time\":\"7-1\"},{\"days\":9,\"time\":\"7-2\"},{\"days\":7,\"time\":\"8-1\"},{\"days\":9,\"time\":\"7-3\"},{\"days\":7,\"time\":\"8-2\"},{\"days\":8,\"time\":\"7-4\"},{\"days\":6,\"time\":\"8-3\"}],\"lineId\":\"022-665-1\",\"lineName\":\"665\",\"stationName\":\"珠江桥\",\"direction\":1,\"order\":29},{\"lineNo\":\"665\",\"timeRanges\":[{\"days\":7,\"time\":\"7-1\"}],\"lineId\":\"022-665-1\",\"lineName\":\"665\",\"stationName\":\"南楼地铁站\",\"direction\":1,\"order\":18},{\"lineNo\":\"48\",\"timeRanges\":[{\"days\":7,\"time\":\"7-1\"},{\"days\":12,\"time\":\"6-4\"}],\"lineId\":\"022-48-0\",\"lineName\":\"48（双层）\",\"stationName\":\"泰兴南路\",\"direction\":0,\"order\":18},{\"lineNo\":\"962\",\"timeRanges\":[{\"days\":10,\"time\":\"6-4\"}],\"lineId\":\"022-962-0\",\"lineName\":\"962\",\"stationName\":\"泰兴南路\",\"direction\":0,\"order\":5},{\"lineNo\":\"862\",\"timeRanges\":[{\"days\":18,\"time\":\"6-4\"}],\"lineId\":\"022-862-1\",\"lineName\":\"862\",\"stationName\":\"泰兴南路\",\"direction\":1,\"order\":4},{\"lineNo\":\"665\",\"timeRanges\":[{\"days\":12,\"time\":\"7-1\"},{\"days\":8,\"time\":\"6-4\"}],\"lineId\":\"022-665-1\",\"lineName\":\"665\",\"stationName\":\"东风立交桥\",\"direction\":1,\"order\":11}],\"udid\":\"7dc58340-cdba-4120-8f5b-b229424bf7fc\"}" };

    @Test
    public void test() throws InterruptedException {
        final long task_count = 20, repeat_per_task = 200_000;
        final long all_count = task_count * repeat_per_task;

        ExecutorService executor = buildExecutor((int) task_count, 1000);
        final ValueOperations<String, String> ops = tpl.opsForValue();
        //        final String str = buildStr(1024 * 1);

        CountDownLatch starter = new CountDownLatch(1);
        final Counter counter = new Counter().addNotifier(new Counter.Notifier() {
            final int step = 100_000;
            long last = System.currentTimeMillis();
            long start = last;

            @Override
            public void notify(long count) {
                if (count % step == 0) {
                    long now = System.currentTimeMillis();
                    long used = now - last, allUsed = now - start;
                    System.out.printf("%, 10d : %, 5d / %, 7d  QPS(s) : %, 5d / % ,5d \n", count, used, allUsed,
                            step * 1000 / used, count * 1000 / allUsed);
                    last = now;
                }
            }
        });

        for (int i = 0; i < task_count; i++) {
            final int i2 = i;
            executor.submit(() -> {
                try {
                    starter.await();

                    for (int j = 0; j < repeat_per_task; j++) {
                        String key = i2 + "key" + j;
//                        ops.set(key, arr[j % arr.length]);
                        ops.get(key);
                        counter.increase(1);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        long stamp1 = System.currentTimeMillis();
        starter.countDown();

        executor.shutdown();
        executor.awaitTermination(1, TimeUnit.HOURS);
        long stamp2 = System.currentTimeMillis();
        long used = stamp2 - stamp1;

        System.out.printf("%,d in %,d ms; qps = %,d \n", all_count, used, (all_count * 1000) / used);
    }

    private static String buildStr(int len) {
        StringBuilder buf = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            buf.append('a');
        }
        return buf.toString();
    }

    private ThreadPoolExecutor buildExecutor(int con, int poolSize) {
        LinkedBlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>(poolSize);

        return new ThreadPoolExecutor(con, con, 1, TimeUnit.HOURS, taskQueue, (task, executor2) -> taskQueue.add(task));
    }

}
