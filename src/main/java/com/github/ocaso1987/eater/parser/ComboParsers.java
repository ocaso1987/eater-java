package com.github.ocaso1987.eater.parser;

import com.github.ocaso1987.eater.Parser;
import com.github.ocaso1987.eater.exception.ReadException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * 组合解析：控制解析顺序与组合（可选、多次、多选、顺序、重复、映射）。
 */
public final class ComboParsers {

    private ComboParsers() {}

    /** 成功则返回结果；仅当 {@link ReadException} 时回退并返回 null。 */
    public static <R> Parser<R> optional(Parser<R> p) {
        return ctx -> {
            int pos = ctx.currentReadPosition();
            try {
                return p.parse(ctx);
            } catch (ReadException e) {
                ctx.setCurrentReadPosition(pos);
                return null;
            }
        };
    }

    /** 将 p 执行零次或多次，遇 {@link ReadException} 回退并结束。 */
    public static <R> Parser<List<R>> many(Parser<R> p) {
        return ctx -> {
            List<R> list = new ArrayList<>();
            for (; ; ) {
                int pos = ctx.currentReadPosition();
                try {
                    list.add(p.parse(ctx));
                } catch (ReadException e) {
                    ctx.setCurrentReadPosition(pos);
                    break;
                }
            }
            return list;
        };
    }

    /** 按顺序依次执行多个解析器，返回结果列表。 */
    @SafeVarargs
    public static <R> Parser<List<R>> many(Parser<R>... parsers) {
        return ctx -> {
            List<R> list = new ArrayList<>(parsers.length);
            for (Parser<R> p : parsers) {
                list.add(p.parse(ctx));
            }
            return list;
        };
    }

    /** 依次尝试多个解析器，返回第一个成功结果；仅对 {@link ReadException} 回退并尝试下一个，全部失败则抛出最后一次的 ReadException。 */
    @SafeVarargs
    public static <R> Parser<R> choose(Parser<R>... parsers) {
        if (parsers == null || parsers.length == 0) {
            throw new IllegalArgumentException("choose: no parsers provided");
        }
        return ctx -> {
            ReadException lastRead = null;
            for (Parser<R> parser : parsers) {
                int pos = ctx.currentReadPosition();
                try {
                    return parser.parse(ctx);
                } catch (ReadException e) {
                    ctx.setCurrentReadPosition(pos);
                    lastRead = e;
                }
            }
            throw lastRead;
        };
    }

    /** 将 p 重复执行 n 次，返回结果列表。 */
    public static <R> Parser<List<R>> repeat(Parser<R> p, int n) {
        return ctx -> {
            List<R> list = new ArrayList<>(n);
            for (int i = 0; i < n; i++) {
                list.add(p.parse(ctx));
            }
            return list;
        };
    }

    /** 对解析结果做映射。 */
    public static <A, B> Parser<B> map(Parser<A> p, Function<A, B> f) {
        return ctx -> f.apply(p.parse(ctx));
    }
}
