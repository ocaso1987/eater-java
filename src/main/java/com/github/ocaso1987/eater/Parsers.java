package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.parser.ByteParsers;
import com.github.ocaso1987.eater.parser.CharParsers;
import com.github.ocaso1987.eater.parser.ComboParsers;
import com.github.ocaso1987.eater.parser.CommonParsers;

import java.nio.charset.Charset;
import java.util.List;
import java.util.function.Function;

/**
 * 解析器门面：聚合字节/字符/组合/通用解析器的静态方法，便于 {@code import static Parsers.*} 使用。
 */
public final class Parsers {

    private Parsers() {}

    // ---------- 字节 ----------

    public static Parser<byte[]> bytes(int n) {
        return ByteParsers.bytes(n);
    }

    public static Parser<byte[]> oneByte() {
        return ByteParsers.oneByte();
    }

    public static Parser<byte[]> exactBytes(byte[] expected) {
        return ByteParsers.exactBytes(expected);
    }

    public static Parser<byte[]> bytesUntil(byte delimiter) {
        return ByteParsers.bytesUntil(delimiter);
    }

    public static Parser<String> bytesAsString(int n, Charset charset) {
        return ByteParsers.bytesAsString(n, charset);
    }

    public static Parser<String> bytesAsUtf8(int n) {
        return ByteParsers.bytesAsUtf8(n);
    }

    // ---------- 字符 ----------

    public static Parser<String> chars(int n) {
        return CharParsers.chars(n);
    }

    public static Parser<Character> oneChar() {
        return CharParsers.oneChar();
    }

    public static Parser<String> exactString(String expected) {
        return CharParsers.exactString(expected);
    }

    public static Parser<String> charsUntil(char delimiter) {
        return CharParsers.charsUntil(delimiter);
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
}
