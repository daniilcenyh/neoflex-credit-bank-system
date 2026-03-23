package com.neoflex.calculator_service.metrics.aspect;

import com.neoflex.calculator_service.metrics.annotation.BusinessMetric;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class BusinessMetricsAspect {

    private final MeterRegistry meterRegistry;

    private final ConcurrentMap<String, Timer> timerCache = new ConcurrentHashMap<>();

    @Around("@annotation(metric)")
    public Object measure(
            ProceedingJoinPoint joinPoint,
            BusinessMetric metric
    ) throws Throwable {
        long startNanos = System.nanoTime();

        String status = "success";

        try {
            return joinPoint.proceed();

        } catch (Exception e) {
            status = "error";
            throw e;

        } finally {
            long durationNanos = System.nanoTime() - startNanos;
            recordMetrics(metric, joinPoint, durationNanos, status);
        }
    }

    private void recordMetrics(
            BusinessMetric metric,
            ProceedingJoinPoint joinPoint,
            long durationNanos,
            String status
    ) {
        try {
            String metricName = metric.value();

            String className = joinPoint.getTarget().getClass().getSimpleName();

            Tags allTags = Tags.of("status", status, "class", className);
            allTags = addCustomTags(allTags, metric.tags());

            Counter.builder(metricName + ".total")
                    .tags(allTags)
                    .description("Total calls")
                    .register(meterRegistry)
                    .increment();

            String timerKey = buildTimerKey(metricName, className, metric.tags(), status);
            Timer timer = timerCache.computeIfAbsent(timerKey, key -> {
                        Tags timerTags = Tags.of("class", className, "status", status);

                        timerTags = addCustomTags(timerTags, metric.tags());
                        return Timer.builder(metricName + ".duration")
                                .tags(timerTags)
                                .description("Execution duration")
                                .publishPercentileHistogram()
                                .sla(
                                        Duration.ofMillis(50),
                                        Duration.ofMillis(100),
                                        Duration.ofMillis(500),
                                        Duration.ofSeconds(1),
                                        Duration.ofSeconds(2)
                                )
                                .register(meterRegistry);
                    }
            );
            timer.record(durationNanos, TimeUnit.NANOSECONDS);

        } catch (Exception e) {
            log.warn("Failed to record metrics for {}", metric.value(), e);
        }
    }

    private Tags addCustomTags(
            Tags base,
            String[] tagExpressions
    ) {
        if (tagExpressions == null) return base;
        Tags result = base;
        for (String tagExpr : tagExpressions) {
            int equalsIndex = tagExpr.indexOf('=');
            if (equalsIndex > 0) {
                String key = tagExpr.substring(0, equalsIndex).trim();
                String value = tagExpr.substring(equalsIndex + 1).trim();
                result = result.and(key, value);
            }
        }

        return result;
    }

    private String buildTimerKey(
            String metricName,
            String className,
            String[] customTags,
            String status
    ) {
        StringBuilder key = new StringBuilder();

        key.append(metricName).append('.').append(className);

        if (customTags != null && customTags.length > 0) {
            String[] sorted = customTags.clone();

            Arrays.sort(sorted);

            for (String tag : sorted) {
                key.append('.').append(tag);
            }
        }

        key.append('.').append(status);

        return key.toString();
    }
}