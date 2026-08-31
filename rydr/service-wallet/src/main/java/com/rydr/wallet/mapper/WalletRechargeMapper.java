package com.rydr.wallet.mapper;

import com.rydr.wallet.entity.WalletRecharge;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletRechargeMapper {

    int insert(WalletRecharge record);

    WalletRecharge selectByOutTradeNo(@Param("outTradeNo") String outTradeNo);

    int updateStatus(@Param("outTradeNo") String outTradeNo,
                     @Param("status") Integer status,
                     @Param("tradeNo") String tradeNo);
}
