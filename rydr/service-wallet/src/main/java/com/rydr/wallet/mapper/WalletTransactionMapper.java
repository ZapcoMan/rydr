package com.rydr.wallet.mapper;

import com.rydr.wallet.entity.WalletTransaction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletTransactionMapper {

    int insert(WalletTransaction record);

    /**
     * Returns the transaction with the given business number, or null if none exists yet.
     * Used for idempotency checks before applying a balance change.
     */
    WalletTransaction selectByBizNo(@Param("bizNo") String bizNo);
}
