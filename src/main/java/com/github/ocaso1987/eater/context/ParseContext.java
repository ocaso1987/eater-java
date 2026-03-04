package com.github.ocaso1987.eater.context;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

/**
 * 解析上下文：仅持有源对象与当前解析状态（如 currentPosition、currentScope），不提供源的读取方式；
 * 具体读取由调用方通过 {@link #getSource()} 获取源后自行按类型调用 ByteSource/CharSource/ObjectSource 的方法。
 * {@link #getCurrentScope()} 表示对象源解析时的当前范围（含 parent 与 target），主要用于 {@link ObjectSource} 的层级解析。
 */
public final class ParseContext {

    private final ParseSource<?> source;
    private int currentPosition;
    private ParseScope currentScope;

    ParseContext(ParseSource<?> source) {
        this.source = source;
        this.currentPosition = 0;
        this.currentScope = null;
    }

    ParseContext(ParseSource<?> source, ParseScope initialScope) {
        this.source = source;
        this.currentPosition = 0;
        this.currentScope = initialScope;
    }

    /** 当前持有的源对象（{@link ByteSource} / {@link CharSource} / {@link ObjectSource}）。 */
    public ParseSource<?> getSource() {
        return source;
    }

    /** 当前解析位置（仅字节源/字符源有意义）。 */
    public int currentPosition() {
        return currentPosition;
    }

    /**
     * 设置当前解析位置（用于回退或推进；仅字节源/字符源支持，对象源抛 {@link UnsupportedOperationException}）。
     */
    public void setCurrentPosition(int position) {
        if (source instanceof ByteSource b) {
            b.validatePosition(position);
        } else if (source instanceof CharSource c) {
            c.validatePosition(position);
        } else {
            throw new UnsupportedOperationException("setCurrentPosition not supported for " + source.getClass().getSimpleName());
        }
        this.currentPosition = position;
    }

    // ---------- 当前范围（主要用于 ObjectSource） ----------

    /** 当前解析范围（含 parent 与 target）；非对象源或未设置时为 null。 */
    public ParseScope getCurrentScope() {
        return currentScope;
    }

    /** 设置当前解析范围。 */
    public void setCurrentScope(ParseScope scope) {
        this.currentScope = scope;
    }

    /**
     * 进入子范围：以当前 scope 为父、以 childTarget 为 target 创建新 scope 并设为当前范围。
     * 当前 scope 为 null 时，创建根级 scope（parent 为 null）。
     *
     * @param childTarget 下级解析目标对象
     * @return 新的当前 scope
     */
    public ParseScope enterScope(Object childTarget) {
        if (currentScope == null) {
            currentScope = new ParseScope(null, childTarget);
        } else {
            currentScope = currentScope.enter(childTarget);
        }
        return currentScope;
    }

    /**
     * 退出当前范围，恢复到父 scope；若当前已是根或 null，则设为 null。
     */
    public void exitScope() {
        currentScope = currentScope == null ? null : currentScope.exit();
    }

    // ---------- 工厂：字节流 ----------

    public static ParseContext fromBytes(byte[] data) {
        return fromBytes(data, 0, data.length);
    }

    public static ParseContext fromBytes(byte[] data, int offset, int length) {
        return new ParseContext(ByteSource.fromBytes(data, offset, length));
    }

    public static ParseContext fromByteBuffer(ByteBuffer buffer) {
        return new ParseContext(ByteSource.fromByteBuffer(buffer));
    }

    public static ParseContext fromEncodedBytes(CharSequence text, Charset charset) {
        return new ParseContext(ByteSource.fromString(
            Objects.requireNonNull(text, "text"),
            Objects.requireNonNull(charset, "charset")));
    }

    public static ParseContext fromEncodedBytesUtf8(CharSequence text) {
        return fromEncodedBytes(text, StandardCharsets.UTF_8);
    }

    // ---------- 工厂：字符流 ----------

    public static ParseContext fromString(CharSequence text) {
        return new ParseContext(CharSource.fromChars(Objects.requireNonNull(text, "text")));
    }

    public static ParseContext fromChars(CharSequence text) {
        return new ParseContext(CharSource.fromChars(Objects.requireNonNull(text, "text")));
    }

    // ---------- 工厂：对象源 ----------

    /** 从根对象创建上下文，初始 currentScope 为以 root 为 target 的根 scope。 */
    public static ParseContext fromObject(Object root) {
        ObjectSource src = new ObjectSource(Objects.requireNonNull(root, "root"));
        return new ParseContext(src, new ParseScope(null, root));
    }
}
