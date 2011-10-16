package yeti.lang.compiler.yeti.builtins;

final class Compare implements Binder {
    YType type;
    int op;
    String fun;

    public Compare(YType type, int op, String fun) {
        this.op = op;
        this.type = type;
        this.fun = Code.mangle(fun);
    }

    public BindRef getRef(int line) {
        CompareFun c = new CompareFun();
        c.binder = this;
        c.type = type;
        c.op = op;
        c.polymorph = true;
        c.line = line;
        c.coreFun = fun;
        return c;
    }
}
