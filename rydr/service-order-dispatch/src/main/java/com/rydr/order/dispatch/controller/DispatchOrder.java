package com.rydr.order.dispatch.controller;

import com.rydr.dto.ResponseResult;
import com.rydr.order.dispatch.service.DispatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

import com.rydr.dto.ResponseResult;

/**
 * @author oi
 */
@RestController
@RequestMapping("/dispatch")
public class DispatchOrder {

    @Autowired
    private DispatchService dispatchService;


    @GetMapping("/call/{orderId}")
    public ResponseResult callCar(@PathVariable("orderId") int orderId,
                                  @RequestParam(value = "driverIds", required = false) List<Integer> driverIds){
        // Driver selection is performed by the real dispatch strategy in phase F.
        // Until then the caller supplies the candidate drivers; no driver is hard-coded here.
        if (driverIds == null) {
            driverIds = new ArrayList<>();
        }
		if (driverIds.isEmpty()) {
			return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
					"No candidate drivers to dispatch order " + orderId + " to");
		}
		return dispatchService.dispatch(orderId, driverIds);
	}

	/**
	 * Auto-dispatch: pick the best candidates with the real driver-selection strategy
	 * (online + distance + rating) and push the order to them.
	 */
	@GetMapping("/auto/{orderId}")
	public ResponseResult autoDispatch(@PathVariable("orderId") int orderId,
									   @RequestParam("userLng") double userLng,
									   @RequestParam("userLat") double userLat,
									   @RequestParam(value = "maxDrivers", defaultValue = "3") int maxDrivers) {
		ResponseResult<List<Integer>> selectResult = dispatchService.selectDrivers(orderId, userLng, userLat, maxDrivers);
		if (selectResult == null || selectResult.getCode() != com.rydr.constatnt.BusinessInterfaceStatus.SUCCESS.getCode()
				|| selectResult.getData() == null || selectResult.getData().isEmpty()) {
			return ResponseResult.fail(com.rydr.constatnt.BusinessInterfaceStatus.FAIL.getCode(),
					"Failed to select candidate drivers for order " + orderId);
		}
		return dispatchService.dispatch(orderId, selectResult.getData());
	}
}
