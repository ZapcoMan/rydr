package com.rydr.wallet.mapper;

import com.rydr.wallet.entity.WalletAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WalletAccountMapper {

    WalletAccount selectByPassengerId(@Param("passengerInfoId") Integer passengerInfoId);

    int insert(WalletAccount record);

    /**
     * Deduct {@code amount} from capital only when the current balance is sufficient.
     * Returns 1 when the row was updated (deduction succeeded), 0 otherwise.
     * This is the concurrency-safe, over-deduction-proof operation.
     */
    int deductCapital(@Param("passengerInfoId") Integer passengerInfoId,
                      @Param("amount") java.math.BigDecimal amount);

    /**
     * Add {@code amount} to capital (used by recharge callback).
     */
    int addCapital(@Param("passengerInfoId") Integer passengerInfoId,
                   @Param("amount") java.math.BigDecimal amount);
}
