package de.hthoene.loralite.util;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Service
public class GpuMonitor {

    private final GpuStatsService gpuStatsService;
    private final ScheduledExecutorService scheduler;
    private volatile GpuStats lastStats;

    public GpuMonitor(GpuStatsService gpuStatsService) {
        this.gpuStatsService = gpuStatsService;
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
    }

    @PostConstruct
    void start() {
        scheduler.scheduleAtFixedRate(this::poll, 0, 3, TimeUnit.SECONDS);
    }

    @PreDestroy
    void stop() {
        scheduler.shutdownNow();
    }

    private void poll() {
        gpuStatsService.queryGpuStats().ifPresent(stats -> lastStats = stats);
    }

    public GpuStats getLastStats() {
        return lastStats;
    }
}
