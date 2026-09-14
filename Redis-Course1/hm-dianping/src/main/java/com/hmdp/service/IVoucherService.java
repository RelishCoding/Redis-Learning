package com.hmdp.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.hmdp.dto.Result;
import com.hmdp.entity.Voucher;

/**
 * <p>
 * 服务类
 * </p>
 */
public interface IVoucherService extends IService<Voucher> {
    void addSeckillVoucher(Voucher voucher);

    Result queryVoucherOfShop(Long shopId);
}
