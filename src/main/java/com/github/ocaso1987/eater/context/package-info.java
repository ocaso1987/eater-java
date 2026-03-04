/**
 * 解析上下文与数据源：{@link com.github.ocaso1987.eater.context.ParseContext} 仅持有当前解析状态（如 currentPosition）与
 * {@link com.github.ocaso1987.eater.context.ParseSource} 源对象，不暴露源的读取方式。
 * {@link com.github.ocaso1987.eater.context.ParseScope} 表示对象源解析的当前范围（parent + target），通过 {@link ParseContext#getCurrentScope()}、{@link ParseContext#enterScope(Object)}、{@link ParseContext#exitScope()} 进入/退出子属性解析。
 * ParseSource 为泛型抽象类，仅暴露公共的 {@link ParseSource#getSource() source} 泛型对象，无公共 getLength/validatePosition；
 * 具体实现 {@link com.github.ocaso1987.eater.context.ByteSource}、
 * {@link com.github.ocaso1987.eater.context.CharSource}、
 * {@link com.github.ocaso1987.eater.context.ObjectSource} 各自提供字节/字符/对象的读方法；
 * ObjectSource 持有一个 Java Object，使用 OGNL 管理以支持按表达式快速读取。
 */
package com.github.ocaso1987.eater.context;
