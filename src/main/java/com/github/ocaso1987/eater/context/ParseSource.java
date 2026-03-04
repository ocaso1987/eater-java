package com.github.ocaso1987.eater.context;

/**
 * 解析数据源泛型抽象：仅暴露公共的 source 泛型对象，不约定 getLength/validatePosition 等公共方法。
 * 具体能力由子类提供：{@link ByteSource}、{@link CharSource}、{@link ObjectSource}。
 *
 * @param <T> 底层源对象类型（如 ByteBuffer、CharSequence、Object）
 */
public abstract class ParseSource<T> {

    /** 底层源对象，由子类构造时传入并暴露给调用方。 */
    private final T source;

    protected ParseSource(T source) {
        this.source = source;
    }

    /** 返回公共的 source 泛型对象。 */
    public T getSource() {
        return source;
    }
}
