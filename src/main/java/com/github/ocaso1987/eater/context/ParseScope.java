package com.github.ocaso1987.eater.context;

import java.util.Objects;

/**
 * 解析范围：表示对象源解析时的当前“位置”，即当前正在解析的 target 对象及其父级链。
 * 用于 {@link ValueTarget}：根 scope 的 target 为根对象，进入下级属性时创建子 scope（parent 指向当前 scope），
 * 子 scope 解析完成后退出并回到父 scope 继续解析。
 */
public final class ParseScope {

    private final ParseScope parent;
    private final Object target;

    /**
     * @param parent 父范围，根 scope 为 null
     * @param target 当前范围内的解析目标对象（即“当前解析位置”对应的对象）
     */
    public ParseScope(ParseScope parent, Object target) {
        this.parent = parent;
        this.target = target;
    }

    /** 父范围；根 scope 时为 null。 */
    public ParseScope getParent() {
        return parent;
    }

    /** 当前范围的解析目标对象。 */
    public Object getTarget() {
        return target;
    }

    /**
     * 创建以当前 scope 为父、以给定对象为 target 的子 scope；进入下级属性解析时使用。
     *
     * @param childTarget 下级解析目标对象
     * @return 新的子 scope，其 parent 为 this
     */
    public ParseScope enter(Object childTarget) {
        return new ParseScope(this, Objects.requireNonNull(childTarget, "childTarget"));
    }

    /**
     * 退出当前 scope，返回父 scope；若当前已是根 scope 则返回 null。
     */
    public ParseScope exit() {
        return parent;
    }

    /** 是否为根 scope（无父范围）。 */
    public boolean isRoot() {
        return parent == null;
    }
}
