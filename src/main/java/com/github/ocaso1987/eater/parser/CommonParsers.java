package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 通用解析：预读及异常处理（peek、peekAny）。
 */
public final class CommonParsers {

    private CommonParsers() {}

    /** 执行 p 但不消费输入；成功则恢复位置并返回结果，仅当 {@link ReadException} 时恢复并返回 null，{@link ParseException} 原样抛出。 */
    public static <R> Parser<R> peek(Parser<R> p) {
        return ctx -> {
            int pos = ctx.currentReadPosition();
            try {
                R result = p.parse(ctx);
                ctx.setCurrentReadPosition(pos);
                return result;
            } catch (ReadException e) {
                ctx.setCurrentReadPosition(pos);
                return null;
            }
        };
    }

    /** 与 {@link #peek(Parser)} 相同（执行 p 但不消费输入），区别是遇到 {@link ParseException} 时也恢复位置并返回 null，而不是抛出。 */
    public static <R> Parser<R> peekAny(Parser<R> p) {
        return ctx -> {
            int pos = ctx.currentReadPosition();
            try {
                R result = p.parse(ctx);
                ctx.setCurrentReadPosition(pos);
                return result;
            } catch (ReadException | ParseException e) {
                ctx.setCurrentReadPosition(pos);
                return null;
            }
        };
    }
}
