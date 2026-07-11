package io.github.glioympas.service;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Runs the feed service repeatedly on a fixed schedule and keeps the
 * application alive until it is shut down.
 */
public class SchedulerService {

    private final FeedService feedService;
    private final Duration interval;
    private final ScheduledExecutorService executor;

    public SchedulerService(FeedService feedService, Duration interval) {
        this.feedService = feedService;
        this.interval = interval;
        this.executor = Executors.newSingleThreadScheduledExecutor();
    }

    /**
     * Starts the schedule: runs one cycle immediately, then again `interval`
     * after each cycle finishes. Blocks forever so the app keeps running.
     */
    public void start() {
        System.out.println("Scheduler started. Interval: " + interval);

        executor.scheduleWithFixedDelay(
                this::runCycleSafely,
                0,                        // run the first cycle immediately
                interval.toMinutes(),     // then wait this long after each cycle
                TimeUnit.MINUTES
        );

        awaitForever();
    }

    /**
     * Runs one cycle, catching everything. Critical: if the scheduled task
     * ever throws an uncaught exception, the scheduler silently stops
     * rescheduling it forever.
     */
    private void runCycleSafely() {
        try {
            System.out.println("--- Starting feed cycle ---");
            feedService.runFetching();
            System.out.println("--- Feed cycle complete ---");
        } catch (Exception e) {
            System.out.println("Feed cycle failed (will retry next interval): " + e.getMessage());
        }
    }

    public void stop() {
        System.out.println("Scheduler stopping...");
        executor.shutdown();
    }

    private void awaitForever() {
        try {
            // Park the main thread so the JVM stays alive while the
            // scheduled task runs on its own background thread.
            Thread.currentThread().join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}