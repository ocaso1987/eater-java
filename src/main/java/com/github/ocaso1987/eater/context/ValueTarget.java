package com.github.ocaso1987.eater.context;

import com.github.ocaso1987.eater.exception.ReadException;

import ognl.Ognl;
import ognl.OgnlException;

/**
 * 值目标：持有一个 Java 普通 Object 作为根对象，使用 OGNL 进行管理以支持按表达式快速读取。
 */
public final class ValueTarget extends ParseTarget<Object> {

    public ValueTarget(Object root) {
        super(root);
    }

    /**
     * 使用 OGNL 表达式从根对象取值，支持快速读取（如 "name"、"user.address.city"、"items[0]"）。
     *
     * @param expression OGNL 表达式
     * @return 表达式求值结果
     * @throws ReadException 表达式解析或求值失败时
     */
    public Object getValue(String expression) throws ReadException {
        try {
            Object tree = Ognl.parseExpression(expression);
            return Ognl.getValue(tree, getTarget());
        } catch (OgnlException e) {
            ReadException ex = new ReadException("OGNL getValue failed: " + e.getMessage(), e);
            ex.addContextValue("expression", expression);
            throw ex;
        }
    }

    /**
     * 使用 OGNL 表达式取值并转为指定类型。
     */
    @SuppressWarnings("unchecked")
    public <R> R getValue(String expression, Class<R> type) throws ReadException {
        Object v = getValue(expression);
        if (v == null) {
            return null;
        }
        if (type.isInstance(v)) {
            return (R) v;
        }
        throw new ReadException("OGNL value type mismatch: expected " + type.getSimpleName() + ", got " + v.getClass().getSimpleName());
    }
}
