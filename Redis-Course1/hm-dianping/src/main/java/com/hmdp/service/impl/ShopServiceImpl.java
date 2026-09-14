package com.hmdp.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

/**
 * <p>
 * 服务实现类
 * </p>
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private CacheClient cacheClient;

    @Override
    public Result queryById(Long id) {
        // 解决缓存穿透
        /*Shop shop = cacheClient.queryWithPassThrough(RedisConstants.SHOP_INFO_KEY, id, Shop.class,
                id2 -> getById(id2), RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);*/
        /*Shop shop = cacheClient.queryWithPassThrough(RedisConstants.SHOP_INFO_KEY, id,
                Shop.class, this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);*/

        // 逻辑过期解决缓存击穿
        /*Shop shop = cacheClient.queryWithLogicalExpire(RedisConstants.SHOP_INFO_KEY, id,
                Shop.class, this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);*/

        // 互斥锁解决缓存击穿
        Shop shop = cacheClient.queryWithMutex(RedisConstants.SHOP_INFO_KEY, id, Shop.class,
                this::getById, RedisConstants.SHOP_INFO_TTL, TimeUnit.MINUTES);

        if (shop == null) {
            return Result.fail("店铺不存在");
        }

        // 返回
        return Result.ok(shop);
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
