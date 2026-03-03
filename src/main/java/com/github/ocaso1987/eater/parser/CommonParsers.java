package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;

/**
 * 通用解析：不区分字节/字符流的组合子（如预读）。
 */
public final class CommonParsers {

    private CommonParsers() {}

    /** 执行 p 但不消费输入；成功则恢复位置并返回结果，仅当 {@link ReadException} 时恢复并返回 null，{@link ParseException} 原样抛出。 */
    public static <R> Parser<R> peek(Parser<R> p) {
        return ctx -> {
            int pos = ctx.position();
            try {
                R result = p.parse(ctx);
                ctx.restorePosition(pos);
                return result;
            } catch (ReadException e) {
                ctx.restorePosition(pos);
                return null;
            }
        };
    }
}
