package com.github.ocaso1987.eater.exception;

/**
 * 数据源写入失败时抛出：位置越界、空间不足、只读等。
 * 可通过 {@link #addContextValue(String, Object)} 携带 position 等上下文，{@link #getMessage()} 会包含。
 */
public class WriteException extends ContextAwareException {

    public WriteException(String message) {
        super(message);
    }

    public WriteException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public WriteException addContextValue(String label, Object value) {
        super.addContextValue(label, value);
        return this;
    }
}
