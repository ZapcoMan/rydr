package com.rydr.listen.controller;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.rydr.listen.response.PreGrabResponse;
import com.rydr.listen.service.ListenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;

/**
 * Pushes pending orders to the driver over a real Server-Sent Events stream.
 *
 * The connection is kept open and polled until an order is assigned to the driver,
 * so the client does not have to keep re-issuing the request.
 *
 * @author oi
 */
@RestController
@RequestMapping("/listen")
@Slf4j
public class ListenController {

    /** Give up if the driver gets no order within this window; the client reconnects. */
    private static final long SSE_TIMEOUT_MS = 30_000L;

    /** How often to look for a newly dispatched order. */
    private static final long POLL_INTERVAL_MS = 1_000L;

    @Autowired
    private ListenService listenService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @GetMapping(value = "/driver/{driverId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter getData(@PathVariable("driverId") int driverId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        AtomicReference<ScheduledFuture<?>> taskRef = new AtomicReference<>();

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                PreGrabResponse response = listenService.listen(driverId);
                if (response != null) {
                    emitter.send(SseEmitter.event().name("order").data(response));
                    log.info("Pushed order {} to driver {}", response.getOrderId(), driverId);
                    emitter.complete();
                }
            } catch (Exception e) {
                log.error("Failed to push order to driverId=" + driverId, e);
                emitter.completeWithError(e);
            }
        }, 0, POLL_INTERVAL_MS, TimeUnit.MILLISECONDS);
        taskRef.set(task);

        // Stop polling as soon as the stream ends, otherwise the task would run forever
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
