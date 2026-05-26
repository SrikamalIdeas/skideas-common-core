package com.skideas.common.util;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Manages a per-request trace ID via SLF4J MDC.
 * <p>
 * Consumers call {@link #init()} at the start of a request (e.g. in a servlet filter)
 * and {@link #clear()} in the finally block.  {@link #get()} can be called anywhere in the
 * same thread to read the current trace ID.
 */
@Component
public class TraceIdProvider {

    public static final String MDC_KEY = "traceId";

    /**
     * Sets a fresh UUID trace ID on MDC and returns it.
     * Overwrites any existing trace ID for this thread.
     */
    public String init() {
        String traceId = UUID.randomUUID().toString();
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    /**
     * Sets a caller-supplied trace ID on MDC (e.g. propagated from an upstream request header).
     *
     * @throws IllegalArgumentException if traceId is null or blank
     */
    public String init(String traceId) {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be null or blank");
        }
        MDC.put(MDC_KEY, traceId);
        return traceId;
    }

    /**
     * Returns the current thread's trace ID, or {@code "no-trace"} if none is set.
     */
    public String get() {
        String id = MDC.get(MDC_KEY);
        return id != null ? id : "no-trace";
    }

    /**
     * Removes the trace ID from MDC. Always call in a finally block.
     */
    public void clear() {
        MDC.remove(MDC_KEY);
    }
}
