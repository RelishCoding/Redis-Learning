package com.hmdp.service.impl;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;

/**
 * <p>
 *  服务实现类
 * </p>
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryTypeList() {
        // 1.从redis中查询数据

        //使用list取 【0 -1】 代表全部
        //debug发现取出的10条数据全在一起，也就是说List里面只有一个元素，内容是所有的数据
        List<String> shopTypeList = stringRedisTemplate.opsForList().range(RedisConstants.SHOP_TYPE_KEY, 0, -1);

        //使用string取
        //String shopType = stringRedisTemplate.opsForValue().get(RedisConstants.SHOP_TYPE_KEY);

        // 2.判断是否存在
        if (CollectionUtil.isNotEmpty(shopTypeList)) {
            // 3.存在，返回数据
            //shopTypeList.get(0) 其实是获取了整个List集合里的元素
            List<ShopType> types = JSONUtil.toList(shopTypeList.get(0), ShopType.class);
            return Result.ok(types);
        }

        /*if (!StrUtil.isEmpty(shopType)) {
            List<ShopType> typeList = JSONUtil.toList(shopType, ShopType.class);
            return Result.ok(typeList);
        }*/

        // 4.不存在，查询数据库
        List<ShopType> typeList = query().orderByAsc("sort").list();
        // 5.不存在，返回错误
        if (CollectionUtil.isEmpty(typeList)) {
            return Result.fail("商户列表信息不存在！");
        }

        // 6.存在，写入redis中

        //list 存
        String jsonStr = JSONUtil.toJsonStr(typeList);
        stringRedisTemplate.opsForList().leftPushAll(RedisConstants.SHOP_TYPE_KEY, jsonStr);

        //string 存
        //stringRedisTemplate.opsForValue().set(RedisConstants.SHOP_TYPE_KEY, JSONUtil.toJsonStr(typeList));

        // 7.返回
        return Result.ok(typeList);
    }
}
