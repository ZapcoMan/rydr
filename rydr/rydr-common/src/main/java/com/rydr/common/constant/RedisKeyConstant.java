package com.rydr.common.constant;

/**
 * @author oi
 */
public class RedisKeyConstant {

    /**
     * Order grabbing lock key prefix
     */
    public static final String GRAB_LOCK_ORDER_KEY_PRE = "lock_";

    /**
     * Driver order grabbing prefix
     */
    public static final String DRIVER_LISTEN_ORDER_PRE = "driver_order_list_";

    /**
     * Set of online driver ids (members are driver id as string).
     */
    public static final String DRIVER_ONLINE_SET = "driver_online_set";

    /**
     * Driver last known location, value = "longitude,latitude".
     */
    public static final String DRIVER_LOCATION_PRE = "driver_location_";

    /**
     * Driver rating cache, value = rating as string (e.g. "4.8").
     */
    public static final String DRIVER_RATE_PRE = "driver_rate_";
}
