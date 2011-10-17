package yeti.lang.compiler.yeti.builtins;

final class BuiltIn implements Binder {
    int op;

    public BuiltIn(int op) {
        this.op = op;
    }

    static BindRef undef_str(Binder binder, int line) {
        return new StaticRef("yeti/lang/Core", "UNDEF_STR",
                             YetiType.STR_TYPE, binder, true, line);
    }

    public BindRef getRef(int line) {
        BindRef r = null;
        switch (op) {
        case 1:
            r = new Argv();
            r.type = YetiType.STRING_ARRAY;
            break;
        case 2:
            r = new InOpFun(line);
            break;
        case 3:
            r = new Cons(line);
            break;
        case 4:
            r = new LazyCons(line);
            break;
        case 5:
            r = new For(line);
            break;
        case 6:
            r = new Compose(line);
            break;
        case 7:
            r = new Synchronized(line);
            break;
        case 8:
            r = new IsNullPtr(YetiType.A_TO_BOOL, "nullptr$q", line);
            break;
        case 9:
            r = new IsDefined(line);
            break;
        case 10:
            r = new IsEmpty(line);
            break;
        case 11:
            r = new Head(line);
            break;
        case 12:
            r = new Tail(line);
            break;
        case 13:
            r = new MatchOpFun(line, true);
            break;
        case 14:
            r = new MatchOpFun(line, false);
            break;
        case 15:
            r = new NotOp(line);
            break;
        case 16:
            r = new StrChar(line);
            break;
        case 17:
            r = new UnitConstant(YetiType.BOOL_TYPE);
            break;
        case 18:
            r = new BooleanConstant(false);
            break;
        case 19:
            r = new BooleanConstant(true);
            break;
        case 20:
            r = new Negate();
            break;
        case 21:
            r = new Same();
            break;
        case 22:
            r = new StaticRef("yeti/lang/Core", "RANDINT",
                              YetiType.NUM_TO_NUM, this, true, line);
            break;
        case 23:
            r = undef_str(this, line);
            break;
        case 24:
            r = new Escape(line);
            break;
        }
        r.binder = this;
        return r;
    }
}
