package com.github.ocaso1987.eater.context;

/**
 * 解析目标：持有一个底层目标对象，用于读取或写出。
 * 具体实现：{@link ByteSource}、{@link CharSource}、{@link ValueTarget} 为输入源；{@link ByteSink}、{@link CharSink} 为输出汇。
 *
 * @param <T> 底层目标对象类型（如 ByteBuffer、CharSequence、Appendable、Object）
 */
public abstract class ParseTarget<T> {

    private final T target;

    protected ParseTarget(T target) {
        this.target = target;
    }

    /** 返回底层目标对象。 */
    public T getTarget() {
        return target;
    }
}
