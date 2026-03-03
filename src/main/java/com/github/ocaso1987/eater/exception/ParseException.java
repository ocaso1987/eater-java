package com.github.ocaso1987.eater.exception;

/**
 * 解析/转换失败时抛出：语义不匹配、格式错误或转换异常。数据源读取失败由 {@link ReadException} 表示。
 * 可通过 {@link #addContextValue(String, Object)} 携带上下文，{@link #getMessage()} 会包含。
 */
public class ParseException extends ContextAwareException {

    public ParseException(String message) {
        super(message);
    }

    public ParseException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public ParseException addContextValue(String label, Object value) {
        super.addContextValue(label, value);
        return this;
    }
}
