package com.qzh;

import com.qzh.pojo.User;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest
class SpringDataRedisDemoApplicationTests {
    //@Autowired
    @Resource(name = "redisTemplate")
    private RedisTemplate<String, Object> redisTemplate;

    @Test
    void testString() {
        //写入一条String数据
        redisTemplate.opsForValue().set("name","虎哥");
        //获取string数据
        Object name = redisTemplate.opsForValue().get("name");
        System.out.println("name = " + name);
    }

    @Test
    void testSaveUser() {
        //写入数据
        redisTemplate.opsForValue().set("user:100", new User("虎哥", 21));
        //读取数据
        User user = (User) redisTemplate.opsForValue().get("user:100");
        System.out.println("user = " + user);
    }
}
