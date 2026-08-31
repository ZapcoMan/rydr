package com.rydr.controller;

import com.rydr.common.constant.RedisKeyConstant;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Pushes a driver's live location to the passenger over Server-Sent Events.
 *
 * The driver reports its coordinates via {@code api-driver /driver/location}, which writes them
 * to Redis under {@code driver_location_<driverId>}. This endpoint streams those coordinates
 * until the driver goes offline or the client disconnects.
 *
 * @author oi
 */
@RestController
@RequestMapping("/location")
@Slf4j
public class DriverLocationController {

	private static final long SSE_TIMEOUT_MS = 30_000L;
	private static final long POLL_INTERVAL_MS = 1_000L;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

	@GetMapping(value = "/driver/{driverId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter track(@PathVariable("driverId") int driverId) {
		SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
		AtomicReference<ScheduledFuture<?>> taskRef = new AtomicReference<>();

		ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
			try {
				String loc = redisTemplate.opsForValue().get(RedisKeyConstant.DRIVER_LOCATION_PRE + driverId);
				if (loc != null && !loc.isBlank()) {
					emitter.send(SseEmitter.event().name("location").data(loc));
				}
			} catch (Exception e) {
				log.error("Failed to push location for driverId=" + driverId, e);
				emitter.completeWithError(e);
			}
		}, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
		taskRef.set(task);

		emitter.onCompletion(() -> cancelTask(taskRef));
		emitter.onTimeout(() -> {
			cancelTask(taskRef);
			emitter.complete();
		});
		emitter.onError(e -> cancelTask(taskRef));

		return emitter;
	}

	private void cancelTask(AtomicReference<ScheduledFuture<?>> taskRef) {
		ScheduledFuture<?> task = taskRef.get();
		if (task != null) {
			task.cancel(false);
		}
	}

	@PreDestroy
	public void shutdown() {
		scheduler.shutdownNow();
	}
}
