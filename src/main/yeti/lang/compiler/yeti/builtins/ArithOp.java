package yeti.lang.compiler.yeti.builtins;

final class ArithOp implements Binder {
    private String fun;
    private String method;
    private YType type;

    ArithOp(String op, String method, YType type) {
        fun = op == "+" ? "plus" : Code.mangle(op);
        this.method = method;
        this.type = type;
    }

    public BindRef getRef(int line) {
        return new ArithOpFun(fun, method, type, this, line);
    }
}
