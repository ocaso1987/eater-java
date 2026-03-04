package com.github.ocaso1987.eater.exception;

/**
 * 数据源读取失败时抛出：无剩余字节/字符、位置越界、长度不足等。
 * 可通过 {@link #addContextValue(String, Object)} 携带 position 等上下文，{@link #getMessage()} 会包含。
 */
public class ReadException extends ContextAwareException {

    public ReadException(String message) {
        super(message);
    }

    public ReadException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ReadException addContextValue(String label, Object value) {
        super.addContextValue(label, value);
        return this;
    }
}
