package com.rydr.dao.mapper;

import com.rydr.entity.PassengerAddress;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * Passenger address mapper.
 *
 * @author oi
 */
@Mapper
public interface PassengerAddressMapper {

    /**
     * Query passenger addresses by passenger id and optional address type.
     *
     * @param passengerInfoId passenger id
     * @param type            address type (nullable)
     * @return matching addresses
     */
    List<PassengerAddress> selectByPassengerAndType(@Param("passengerInfoId") Integer passengerInfoId,
                                                    @Param("type") Integer type);

    int insertSelective(PassengerAddress record);

    int updateByPrimaryKeySelective(PassengerAddress record);
}
