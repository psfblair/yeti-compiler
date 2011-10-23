package yeti.lang.compiler.yeti.type;

import yeti.lang.compiler.closure.Closure;
import yeti.lang.compiler.code.Binder;

public class Scope {
    private Scope outer;
    private String name;
    private YType[] typeDef;
    Binder binder;
    YType[] free;
    Closure closure; // non-null means outer scopes must be proxied
    YetiType.ClassBinding importClass;

    YetiType.ScopeCtx ctx;

    public Scope(Scope outer, String name, Binder binder) {
        this.outer = outer;
        this.name = name;
        this.binder = binder;
        ctx = outer == null ? null : outer.ctx;
    }

    public Scope getOuter() {
        return outer;
    }

    public String getName() {
        return name;
    }

    public YType[] getTypeDef() {
        return typeDef;
    }
}
