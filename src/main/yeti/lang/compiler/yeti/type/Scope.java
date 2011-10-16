package yeti.lang.compiler.yeti.type;

class Scope {
    yeti.lang.compiler.Scope outer;
    String name;
    Binder binder;
    yeti.lang.compiler.YType[] free;
    Closure closure; // non-null means outer scopes must be proxied
    yeti.lang.compiler.YetiType.ClassBinding importClass;
    yeti.lang.compiler.YType[] typeDef;

    yeti.lang.compiler.YetiType.ScopeCtx ctx;

    Scope(yeti.lang.compiler.Scope outer, String name, Binder binder) {
        this.outer = outer;
        this.name = name;
        this.binder = binder;
        ctx = outer == null ? null : outer.ctx;
    }
}
