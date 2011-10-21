package yeti.lang.compiler.yeti.type;

import yeti.lang.compiler.closure.Closure;
import yeti.lang.compiler.code.Binder;

public class Scope {
    Scope outer;
    String name;
    Binder binder;
    YType[] free;
    Closure closure; // non-null means outer scopes must be proxied
    YetiType.ClassBinding importClass;
    YType[] typeDef;

    YetiType.ScopeCtx ctx;

    Scope(Scope outer, String name, Binder binder) {
        this.outer = outer;
        this.name = name;
        this.binder = binder;
        ctx = outer == null ? null : outer.ctx;
    }
}
