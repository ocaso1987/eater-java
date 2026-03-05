package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.parser.ByteSinkParsers;
import com.github.ocaso1987.eater.parser.ByteSourceParsers;
import com.github.ocaso1987.eater.parser.CharSinkParsers;
import com.github.ocaso1987.eater.parser.CharSourceParsers;
import com.github.ocaso1987.eater.parser.ComboParsers;
import com.github.ocaso1987.eater.parser.CommonParsers;
import com.github.ocaso1987.eater.parser.StringParsers;
import com.github.ocaso1987.eater.parser.ValueTargetParsers;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

/**
 * 解析器门面：聚合字节/字符/组合/通用解析器的静态方法，便于 {@code import static Parsers.*} 使用。
 */
public final class Parsers {

    private Parsers() {}

    // ---------- 字节 ----------

    public static Parser<byte[]> byte_n(int n) {
        return ByteSourceParsers.n(n);
    }

    public static Parser<byte[]> byte_one() {
        return ByteSourceParsers.one();
    }

    public static Parser<byte[]> byte_expect(byte[] expected) {
        return ByteSourceParsers.expect(expected);
    }

    public static Parser<byte[]> byte_until(byte delimiter) {
        return ByteSourceParsers.until(delimiter);
    }

    // ---------- 字节汇（写） ----------

    public static Parser<byte[]> byte_sink_one(byte b) {
        return ByteSinkParsers.one(b);
    }

    public static Parser<byte[]> byte_sink_n(byte[] data) {
        return ByteSinkParsers.n(data);
    }

    public static Parser<byte[]> byte_sink_write(byte[] data) {
        return ByteSinkParsers.write(data);
    }

    public static Parser<byte[]> byte_sink_write(byte[] src, int offset, int length) {
        return ByteSinkParsers.write(src, offset, length);
    }

    public static Parser<String> byte_asString(int n, Charset charset) {
        return StringParsers.asStr(n, charset);
    }

    public static Parser<String> byte_utf8(int n) {
        return StringParsers.asUtf8(n);
    }

    // ---------- 字符/字符串 ----------

    /** 从 CharSource 读取 n 个字符为字符串。 */
    public static Parser<String> str_n(int n) {
        return CharSourceParsers.n(n);
    }

    public static Parser<Character> char_one() {
        return CharSourceParsers.one();
    }

    public static Parser<String> str_expect(String expected) {
        return CharSourceParsers.expect(expected);
    }

    public static Parser<String> str_until(char delimiter) {
        return CharSourceParsers.until(delimiter);
    }

    // ---------- 字符汇（写） ----------

    public static Parser<Character> char_sink_one(char c) {
        return CharSinkParsers.one(c);
    }

    public static Parser<String> char_sink_n(String s) {
        return CharSinkParsers.n(s);
    }

    public static Parser<String> char_sink_write(String data) {
        return CharSinkParsers.write(data);
    }

    public static Parser<String> char_sink_write(char[] chars, int offset, int length) {
        return CharSinkParsers.write(chars, offset, length);
    }

    // ---------- 值源 ----------

    /** 按 OGNL 表达式从 ValueTarget 取值。 */
    public static Parser<Object> val(String expression) {
        return ValueTargetParsers.value(expression);
    }

    /** 按 OGNL 表达式从 ValueTarget 取值并转为指定类型。 */
    public static <R> Parser<R> val(String expression, Class<R> type) {
        return ValueTargetParsers.value(expression, type);
    }

    // ---------- 组合 ----------

    public static <R> Parser<R> optional(Parser<R> p) {
        return ComboParsers.optional(p);
    }

    public static <R> Parser<List<R>> many(Parser<R> p) {
        return ComboParsers.many(p);
    }

    @SafeVarargs
    public static <R> Parser<List<R>> many(Parser<R>... parsers) {
        return ComboParsers.many(parsers);
    }

    @SafeVarargs
    public static <R> Parser<R> choose(Parser<R>... parsers) {
        return ComboParsers.choose(parsers);
    }

    public static <R> Parser<List<R>> repeat(Parser<R> p, int n) {
        return ComboParsers.repeat(p, n);
    }

    public static <A, B> Parser<B> map(Parser<A> p, Function<A, B> f) {
        return ComboParsers.map(p, f);
    }

    // ---------- 通用 ----------

    public static <R> Parser<R> peek(Parser<R> p) {
        return CommonParsers.peek(p);
    }

    /** 与 peek 相同（执行 p 但不消费输入），区别是遇到 ParseException 时也恢复位置并返回 null。 */
    public static <R> Parser<R> peekAny(Parser<R> p) {
        return CommonParsers.peekAny(p);
    }
}
