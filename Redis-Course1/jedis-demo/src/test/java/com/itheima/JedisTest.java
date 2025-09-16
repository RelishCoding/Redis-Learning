package com.itheima;

import com.itheima.util.JedisConnectionFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.Jedis;

import java.util.Map;

public class JedisTest {
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        // 1.建立连接
        // jedis = new Jedis("192.168.221.140", 6379);
        jedis = JedisConnectionFactory.getJedis();
        // 2.设置密码
        jedis.auth("123456");
        // 3.选择库
        jedis.select(0);
    }

    @Test
    void testString() {
        // 存入数据
        String result = jedis.set("name", "虎哥");
        System.out.println("result = " + result);
        // 获取数据
        String name = jedis.get("name");
        System.out.println("name = " + name);
    }

    @Test
    void testHash() {
        // 插入hash数据
        jedis.hset("user:1", "name", "Jack");
        jedis.hset("user:1", "age", "18");
        // 获取数据
        String name = jedis.hget("user:1", "name");
        System.out.println("name = " + name);
        String age = jedis.hget("user:1", "age");
        System.out.println("age = " + age);

        Map<String, String> map = jedis.hgetAll("user:1");
        System.out.println("map = " + map);
    }

    @AfterEach
    void tearDown() {
        if (jedis != null) {
            // 关闭连接
            jedis.close();
        }
    }
}
