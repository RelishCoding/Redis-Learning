package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final ExecutorService CACHE_REBUILD_EXECUTOR = Executors.newFixedThreadPool(10);

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryByIdWithCacheUtil(Long id) {
        // 缓存穿透
//        Shop shop = cacheClient.queryWithPassThrough(RedisConstants.SHOP_INFO_KEY, id,
//                Shop.class, this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);

        // 逻辑过期解决缓存击穿
//        Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.SHOP_INFO_KEY, id,
//                Shop.class, this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);

        // 互斥锁解决缓存击穿
        Shop shop = cacheClient.queryWithMutex(RedisConstants.SHOP_INFO_KEY, id, Shop.class,
                this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        // 7.返回
        return Result.ok(shop);
    }

    @Override
    public Result queryById(Long id) {
        // 缓存穿透
        // Shop shop = queryWithPassThrough(id);

        // 互斥锁解决缓存击穿
        // Shop shop = queryWithMutex(id);

        // 逻辑过期解决缓存击穿
        Shop shop = queryWithLogicalExpire(id);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        // 7.返回
        return Result.ok(shop);
    }

    // 利用逻辑过期实现的缓存击穿的解决方案
    private Shop queryWithLogicalExpire(Long id) {
        String key = RedisConstants.SHOP_INFO_KEY + id;

        // 1.从Redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在
        if (StrUtil.isBlank(shopJson)) {
            // 3.缓存未命中，直接返回null
            return null;
        }

        // 4.命中，需要先把json序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject) redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();

        // 5.判断是否过期
        if (expireTime.isAfter(LocalDateTime.now())) {
            // 未过期，直接返回店铺信息
            return shop;
        }

        // 6.已过期，需要缓存重建
        // 6.1.获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        boolean success = tryLock(lockKey);
        // 6.2.判断是否获取锁成功
        if (success) {
            // 6.3.成功，再次检测redis缓存是否过期，做DoubleCheck
            shopJson = stringRedisTemplate.opsForValue().get(key);
            redisData = JSONUtil.toBean(shopJson, RedisData.class);
            shop = JSONUtil.toBean((JSONObject) redisData.getData(), Shop.class);
            expireTime = redisData.getExpireTime();
            if (expireTime.isAfter(LocalDateTime.now())) {
                // 未过期，无需重建缓存，直接返回
                return shop;
            }
            // 过期，开启独立线程，实现缓存重建
            CACHE_REBUILD_EXECUTOR.submit(() -> {
                try {
                    this.saveShop2Redis(id, 20L);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                } finally {
                    // 释放锁
                    unlock(lockKey);
                }
            });
        }
        // 6.4.返回过期的商铺信息
        return shop;
    }

    public void saveShop2Redis(Long id, Long expireSeconds) throws InterruptedException {
        // 1.查询店铺数据
        Shop shop = getById(id);
        // 模拟缓存重建的延迟
        Thread.sleep(20);
        // 2.封装逻辑过期时间
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireSeconds));
        // 3.写入Redis
        stringRedisTemplate.opsForValue().set(RedisConstants.SHOP_INFO_KEY + id, JSONUtil.toJsonStr(redisData));
    }

    // 利用互斥锁实现的缓存击穿的解决方案
    private Shop queryWithMutex(Long id) {
        String key = RedisConstants.SHOP_INFO_KEY + id;
        Shop shop;

        // 1.从Redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }

        // 判断命中的是否是空值
        if ("".equals(shopJson)) {
            // 返回错误信息
            return null;
        }

        // 4.实现缓存重建
        // 4.1.获取互斥锁
        String lockKey = RedisConstants.LOCK_SHOP_KEY + id;
        try {
            boolean success = tryLock(lockKey);
            // 4.2.判断是否获取成功
            if (!success) {
                // 4.3.失败，则休眠并重试
                Thread.sleep(50);
                return queryWithMutex(id);
            }
            // 4.4.成功，再次检测redis缓存是否存在，做DoubleCheck
            shopJson = stringRedisTemplate.opsForValue().get(key);
            if (StrUtil.isNotBlank(shopJson)) {
                // 存在，无需重建缓存，直接返回
                shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }
            // 不存在，根据id查询数据库
            shop = getById(id);
            // 模拟查询数据库的延迟
            // Thread.sleep(200);

            // 5.不存在，返回错误
            if (shop == null) {
                // 将空值写入redis
                stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
                // 返回错误信息
                return null;
            }

            // 6.存在，写入Redis
            stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            // 7.释放互斥锁
            unlock(lockKey);
        }

        // 8.返回
        return shop;
    }

    // 缓存穿透的解决方案
    private Shop queryWithPassThrough(Long id) {
        String key = RedisConstants.SHOP_INFO_KEY + id;

        // 1.从Redis查询商户缓存
        String shopJson = stringRedisTemplate.opsForValue().get(key);

        // 2.判断是否存在
        if (StrUtil.isNotBlank(shopJson)) {
            // 3.存在，直接返回
            return JSONUtil.toBean(shopJson, Shop.class);
        }

        // 判断命中的是否是空值
        if ("".equals(shopJson)) {
            // 返回错误信息
            return null;
        }

        // 4.不存在，根据id查询数据库
        Shop shop = getById(id);

        // 5.不存在，返回错误
        if (shop == null) {
            // 将空值写入redis
            stringRedisTemplate.opsForValue().set(key, "", RedisConstants.CACHE_NULL_TTL, TimeUnit.MINUTES);
            // 返回错误信息
            return null;
        }

        // 6.存在，写入Redis
        stringRedisTemplate.opsForValue().set(key, JSONUtil.toJsonStr(shop), RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);

        // 7.返回
        return shop;
    }

    private boolean tryLock(String key) {
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key, "1", RedisConstants.LOCK_SHOP_TTL, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key) {
        stringRedisTemplate.delete(key);
    }

    @Override
    @Transactional
    public Result update(Shop shop) {
        Long id = shop.getId();
        if (id == null) {
            return Result.fail("店铺id不能为空");
        }
        // 1.更新数据库
        updateById(shop);
        // 2.删除缓存
        stringRedisTemplate.delete(RedisConstants.SHOP_INFO_KEY + id);
        return Result.ok();
    }
}
