package com.ai.library.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.time.Instant;

public class MetricsInterceptor implements HandlerInterceptor {

    private final MeterRegistry registry;
    private final Counter borrowCounter;

    public MetricsInterceptor(MeterRegistry registry) {
        this.registry = registry;
        this.borrowCounter = Counter.builder("library.borrow.count").description("Number of borrow operations").register(registry);
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        request.setAttribute("_metrics_start", Instant.now());
        // increment simple counters for certain endpoints
        String path = request.getRequestURI();
        if (path.startsWith("/api/borrow") && "POST".equalsIgnoreCase(request.getMethod())) {
            borrowCounter.increment();
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        Object o = request.getAttribute("_metrics_start");
        if (o instanceof Instant) {
            Instant start = (Instant) o;
            long ms = Duration.between(start, Instant.now()).toMillis();
            Timer.builder("http.server.requests.custom").description("HTTP server request duration (custom)").tag("uri", request.getRequestURI()).register(registry).record(ms, java.util.concurrent.TimeUnit.MILLISECONDS);
        }
    }
}
