package yeti.lang.compiler.yeti.builtins;

final class Regex implements Binder {
    private String fun, impl;
    private YType type;

    Regex(String fun, String impl, YType type) {
        this.fun = fun;
        this.impl = impl;
        this.type = type;
    }

    public BindRef getRef(int line) {
        return new RegexFun(fun, impl, type, this, line);
    }
}
