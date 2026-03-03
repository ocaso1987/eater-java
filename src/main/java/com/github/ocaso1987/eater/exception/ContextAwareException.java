package com.github.ocaso1987.eater.exception;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * 带上下文键值对的异常基类。子类通过 {@link #addContextValue(String, Object)} 添加诊断信息，
 * {@link #getMessage()} 会将上下文追加到消息后。
 */
abstract class ContextAwareException extends Exception {

    protected final Map<String, Object> contextValues = new LinkedHashMap<>();

    protected ContextAwareException(String message) {
        super(message);
    }

    protected ContextAwareException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * 添加一条上下文键值，支持链式调用。子类应重写并返回自身类型。
     */
    public ContextAwareException addContextValue(String label, Object value) {
        contextValues.put(label, value);
        return this;
    }

    /** 返回已添加的上下文（只读）。 */
    public Map<String, Object> getContextValues() {
        return Collections.unmodifiableMap(contextValues);
    }

    /** 返回所有上下文键名。 */
    public Set<String> getContextLabels() {
        return Collections.unmodifiableSet(contextValues.keySet());
    }

    @Override
    public String getMessage() {
        String base = super.getMessage();
        if (contextValues.isEmpty()) {
            return base;
        }
        StringBuilder sb = new StringBuilder();
        if (base != null && !base.isEmpty()) {
            sb.append(base);
        }
        sb.append(" [context: ");
        int i = 0;
        for (Map.Entry<String, Object> e : contextValues.entrySet()) {
            if (i++ > 0) sb.append(", ");
            sb.append(e.getKey()).append('=').append(e.getValue());
        }
        sb.append(']');
        return sb.toString();
    }
}
