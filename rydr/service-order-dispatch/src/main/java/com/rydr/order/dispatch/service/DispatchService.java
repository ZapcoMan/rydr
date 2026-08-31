package com.rydr.order.dispatch.service;

import com.rydr.dto.ResponseResult;

import java.util.List;

/**
 * @author oi
 */
public interface DispatchService {

    /**
     * Dispatch a specified order to multiple drivers
     * @param orderId
     * @param driverIdList
     * @return
     */
    public ResponseResult dispatch(int orderId , List<Integer> driverIdList);

    /**
     * Select the best candidate drivers for an order using a real strategy:
     * online drivers within range, ranked by distance and rating.
     *
     * @param orderId     order id (used to log / tag the dispatch)
     * @param userLng     passenger longitude
     * @param userLat     passenger latitude
     * @param maxDrivers  maximum number of candidates to return
     * @return response containing the ordered candidate driver id list
     */
    public ResponseResult<List<Integer>> selectDrivers(int orderId, double userLng, double userLat, int maxDrivers);
}
