package com.hmdp;

import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.CacheClient;
import com.hmdp.utils.RedisConstants;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import javax.annotation.Resource;
import java.util.concurrent.TimeUnit;

@SpringBootTest
public class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;

    @Resource
    private CacheClient cacheClient;

    @Test
    void testSaveShop2Redis() throws InterruptedException {
        // shopService.saveShop2Redis(1L, 10L);

        Shop shop = shopService.getById(1L);
        cacheClient.setWithLogicalExpire(RedisConstants.SHOP_INFO_KEY + 1L, shop, 10L, TimeUnit.SECONDS);
    }
}
