package com.rydr.order.dao;

import org.apache.ibatis.annotations.Mapper;

import com.rydr.entity.Order;

import java.util.List;
import java.util.Map;

@Mapper
public interface OrderMapper {
    int deleteByPrimaryKey(Integer orderid);

    int insert(Order record);

    int insertSelective(Order record);

    Order selectByPrimaryKey(Integer orderid);

    Order selectByOrderNumber(String orderNumber);

    List<Order> selectByPassenger(Integer passengerInfoId);

    int updateByPrimaryKeySelective(Order record);

    int updateByPrimaryKey(Order record);

    int payOrder(Map<String, Object> params);

    int cancelOrder(Map<String, Object> params);
}