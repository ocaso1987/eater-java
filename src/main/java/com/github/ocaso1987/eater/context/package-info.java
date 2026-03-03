/**
 * 解析上下文与数据源：{@link com.github.ocaso1987.eater.context.ParseContext} 持有位置与
 * {@link com.github.ocaso1987.eater.context.ParseSource}，提供顺序读与 restorePosition；
 * ParseSource 封装 ByteBuffer/CharSequence 按索引读取，字节/字符模式互斥，模式混用抛
 * {@link UnsupportedOperationException}。
 */
package com.github.ocaso1987.eater.context;
