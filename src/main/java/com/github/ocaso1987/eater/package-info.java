/**
 * Eater 解析器核心：{@link com.github.ocaso1987.eater.Parser} 从
 * {@link com.github.ocaso1987.eater.context.ParseContext} 消费输入并返回结果；
 * 失败时抛出 {@link com.github.ocaso1987.eater.exception.ReadException} 或
 * {@link com.github.ocaso1987.eater.exception.ParseException}。
 * 使用 {@link com.github.ocaso1987.eater.Parsers} 组合解析器，例如
 * {@code ParseContext.fromChars(text)} 配合 {@code import static Parsers.*}。
 */
package com.github.ocaso1987.eater;
