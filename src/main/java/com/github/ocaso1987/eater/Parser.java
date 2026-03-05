package com.github.ocaso1987.eater;

import com.github.ocaso1987.eater.context.ParseContext;
import com.github.ocaso1987.eater.exception.ParseException;
import com.github.ocaso1987.eater.exception.ReadException;
import com.github.ocaso1987.eater.exception.WriteException;

/**
 * 解析器：从 {@link ParseContext} 消费输入并返回结构化结果；写汇解析器会向 {@link ParseContext#getSink()} 写出并推进写位置。
 *
 * @param <R> 解析结果类型
 */
@FunctionalInterface
public interface Parser<R> {

    /**
     * 在上下文中执行解析，消费输入并推进位置（读或写）。
     *
     * @param context 解析上下文（字节或字符流、当前位置）
     * @return 解析结果
     * @throws ReadException  数据不足、越界等读取失败
     * @throws WriteException 写入越界、只读等写入失败
     * @throws ParseException 格式或语义解析失败
     */
    R parse(ParseContext context) throws ReadException, WriteException, ParseException;
}
