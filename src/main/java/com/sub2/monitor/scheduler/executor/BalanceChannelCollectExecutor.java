package com.sub2.monitor.scheduler.executor;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.sub2.monitor.dto.newapi.NewApiRequest;
import com.sub2.monitor.dto.sub2api.Sub2ApiRequest;
import com.sub2.monitor.entity.Platform;
import com.sub2.monitor.service.PlatformService;
import com.sub2.monitor.strategy.newapi.NewApiStrategy;
import com.sub2.monitor.strategy.sub2api.Sub2ApiStrategy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Component
@Slf4j
public class BalanceChannelCollectExecutor {

    private static final String PLATFORM_TYPE_SUB2_API = "sub2Api";
    private static final String PLATFORM_TYPE_NEW_API = "newApi";

    private final PlatformService platformService;
    private final Sub2ApiStrategy sub2ApiStrategy;
    private final NewApiStrategy newApiStrategy;

    public BalanceChannelCollectExecutor(PlatformService platformService,
                                         Sub2ApiStrategy sub2ApiStrategy,
                                         NewApiStrategy newApiStrategy) {
        this.platformService = platformService;
        this.sub2ApiStrategy = sub2ApiStrategy;
        this.newApiStrategy = newApiStrategy;
    }

    public void collectAll() {
        List<Platform> platforms = platformService.list(new LambdaQueryWrapper<Platform>()
                .eq(Platform::getIsEnabled, true)
                .orderByAsc(Platform::getId));
        if (platforms.isEmpty()) {
            log.info("未找到启用平台，跳过余额和渠道采集");
            return;
        }

        log.info("开始并发采集平台余额和渠道, platformCount={}", platforms.size());
        List<PlatformCollectFailure> failures = new ArrayList<>();
        try (ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor()) {
            List<Future<PlatformCollectResult>> futures = executorService.invokeAll(toTasks(platforms));
            for (Future<PlatformCollectResult> future : futures) {
                try {
                    PlatformCollectResult result = future.get();
                    if (result.failure() == null) {
                        log.info("平台采集完成, platformId={}, name={}, type={}, baseUrl={}",
                                result.platform().getId(),
                                result.platform().getName(),
                                result.platform().getType(),
                                result.platform().getBaseUrl());
                    } else {
                        failures.add(result.failure());
                    }
                } catch (ExecutionException exception) {
                    failures.add(new PlatformCollectFailure(null, exception.getCause()));
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("balance channel collection interrupted", exception);
        }

        if (!failures.isEmpty()) {
            failures.forEach(failure -> {
                Platform platform = failure.platform();
                if (platform == null) {
                    log.error("平台采集异常", failure.throwable());
                    return;
                }
                log.error("平台采集失败, platformId={}, name={}, type={}, baseUrl={}",
                        platform.getId(),
                        platform.getName(),
                        platform.getType(),
                        platform.getBaseUrl(),
                        failure.throwable());
            });
            throw new IllegalStateException(buildFailureMessage(failures));
        }

        log.info("平台余额和渠道采集完成, platformCount={}", platforms.size());
    }

    public void collectOneAsync(Long platformId) {
        Platform platform = platformService.getById(platformId);
        if (platform == null) {
            throw new IllegalArgumentException("platform not found, platformId=" + platformId);
        }

        Thread.startVirtualThread(() -> {
            log.info("开始单平台采集, platformId={}, name={}, type={}, baseUrl={}",
                    platform.getId(), platform.getName(), platform.getType(), platform.getBaseUrl());
            PlatformCollectResult result = collectPlatform(platform);
            if (result.failure() == null) {
                log.info("单平台采集完成, platformId={}, name={}, type={}, baseUrl={}",
                        platform.getId(), platform.getName(), platform.getType(), platform.getBaseUrl());
                return;
            }

            log.error("单平台采集失败, platformId={}, name={}, type={}, baseUrl={}",
                    platform.getId(), platform.getName(), platform.getType(), platform.getBaseUrl(),
                    result.failure().throwable());
        });
    }

    private List<Callable<PlatformCollectResult>> toTasks(List<Platform> platforms) {
        return platforms.stream()
                .map(platform -> (Callable<PlatformCollectResult>) () -> collectPlatform(platform))
                .toList();
    }

    private PlatformCollectResult collectPlatform(Platform platform) {
        try {
            if (PLATFORM_TYPE_SUB2_API.equals(platform.getType())) {
                Sub2ApiRequest request = new Sub2ApiRequest();
                request.setBaseUrl(platform.getBaseUrl());
                sub2ApiStrategy.execute(request);
                return PlatformCollectResult.success(platform);
            }

            if (PLATFORM_TYPE_NEW_API.equals(platform.getType())) {
                NewApiRequest request = new NewApiRequest();
                request.setBaseUrl(platform.getBaseUrl());
                newApiStrategy.execute(request);
                return PlatformCollectResult.success(platform);
            }

            throw new IllegalArgumentException("unsupported platform type: " + platform.getType());
        } catch (Exception exception) {
            return PlatformCollectResult.failure(platform, exception);
        }
    }

    private String buildFailureMessage(List<PlatformCollectFailure> failures) {
        return failures.stream()
                .map(failure -> {
                    Platform platform = failure.platform();
                    String causeMessage = failure.throwable() == null ? "unknown" : failure.throwable().getMessage();
                    if (platform == null) {
                        return "unknown platform: " + causeMessage;
                    }
                    return "platformId=" + platform.getId()
                            + ", name=" + platform.getName()
                            + ", type=" + platform.getType()
                            + ", baseUrl=" + platform.getBaseUrl()
                            + ", reason=" + causeMessage;
                })
                .collect(java.util.stream.Collectors.joining("; ",
                        "platform collection failed, failureCount=" + failures.size() + ", failures=[",
                        "]"));
    }

    private record PlatformCollectResult(Platform platform, PlatformCollectFailure failure) {

        private static PlatformCollectResult success(Platform platform) {
            return new PlatformCollectResult(platform, null);
        }

        private static PlatformCollectResult failure(Platform platform, Throwable throwable) {
            return new PlatformCollectResult(platform, new PlatformCollectFailure(platform, throwable));
        }
    }

    private record PlatformCollectFailure(Platform platform, Throwable throwable) {
    }
}
