package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.context.ValueTarget;

/**
 * 值目标解析：按 OGNL 表达式从 ValueTarget 取值。
 */
public final class ValueTargetParsers {

    private ValueTargetParsers() {}

    /** 按 OGNL 表达式取值，返回 Object。 */
    public static Parser<Object> value(String expression) {
        return ctx -> {
            ValueTarget s = (ValueTarget) ctx.getSource();
            return s.getValue(expression);
        };
    }

    /** 按 OGNL 表达式取值并转为指定类型。 */
    public static <R> Parser<R> value(String expression, Class<R> type) {
        return ctx -> {
            ValueTarget s = (ValueTarget) ctx.getSource();
            return s.getValue(expression, type);
        };
    }
}
