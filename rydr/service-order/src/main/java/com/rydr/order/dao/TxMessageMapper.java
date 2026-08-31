package com.rydr.order.dao;

import com.rydr.order.entity.TxMessage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * MyBatis mapper for the reliable-message outbox (tx_message).
 */
@Mapper
public interface TxMessageMapper {

    int insert(TxMessage record);

    int updateStatus(@Param("id") Long id,
                     @Param("status") int status,
                     @Param("retry") int retry,
                     @Param("nextRetryAt") java.util.Date nextRetryAt);

    /** Update to DONE (final success), carrying the new status. */
    int markDone(@Param("id") Long id);

    /** Fetch a window of rows still INIT / SENT with a next_retry_at in the past. */
    List<TxMessage> selectDue(@Param("limit") int limit);

    TxMessage selectByBizKey(@Param("bizKey") String bizKey);

    int deleteByBizKey(@Param("bizKey") String bizKey);
}
