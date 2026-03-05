/**
 * 解析上下文与解析目标：{@link com.github.ocaso1987.eater.context.ParseContext} 仅持有当前解析状态（如 currentReadPosition/currentWritePosition、currentInputScope/currentOutputScope）与
 * {@link com.github.ocaso1987.eater.context.ParseTarget} 解析目标（输入源），不暴露源的读取方式。
 * {@link com.github.ocaso1987.eater.context.ParseScope} 表示对象源解析的当前范围（parent + target），通过 {@link ParseContext#getCurrentInputScope()}、{@link ParseContext#enterInputScope(Object)}、{@link ParseContext#exitInputScope()} 及对应的 output 方法进入/退出子属性解析。
 * 解析目标由 {@link com.github.ocaso1987.eater.context.ParseTarget} 统一抽象，持有 {@link ParseTarget#getTarget() target}：输入实现 {@link com.github.ocaso1987.eater.context.ByteSource}、{@link com.github.ocaso1987.eater.context.CharSource}、{@link com.github.ocaso1987.eater.context.ValueTarget} 提供字节/字符/对象读方法（ValueTarget 持 Java Object，用 OGNL 按表达式读取）；输出实现 {@link com.github.ocaso1987.eater.context.ByteSink}、{@link com.github.ocaso1987.eater.context.CharSink} 持有字节（ByteBuffer）与字符（Appendable）输出流信息。
 */
package com.github.ocaso1987.eater.context;
