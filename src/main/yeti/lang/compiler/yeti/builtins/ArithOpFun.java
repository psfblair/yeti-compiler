package yeti.lang.compiler.yeti.builtins;

final class ArithOpFun extends BinOpRef {
    private String method;
    private int line;

    public ArithOpFun(String fun, String method, YType type,
                      Binder binder, int line) {
        this.type = type;
        this.method = method;
        coreFun = fun;
        this.binder = binder;
        this.line = line;
    }

    public BindRef getRef(int line) {
        return this; // XXX should copy for type?
    }

    void binGen(Ctx ctx, Code arg1, Code arg2) {
        if (method == "and" && arg2 instanceof NumericConstant &&
            ((NumericConstant) arg2).flagop(INT_NUM)) {
            ctx.typeInsn(NEW, "yeti/lang/IntNum");
            ctx.insn(DUP);
            arg1.gen(ctx);
            ctx.visitLine(line);
            ctx.typeInsn(CHECKCAST, "yeti/lang/Num");
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Num",
                                "longValue", "()J");
            ((NumericConstant) arg2).genInt(ctx, false);
            ctx.insn(LAND);
            ctx.visitInit("yeti/lang/IntNum", "(J)V");
            ctx.forceType("yeti/lang/Num");
            return;
        }
        arg1.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Num");
        boolean ii = method == "intDiv" || method == "rem";
        if (method == "shl" || method == "shr") {
            ctx.genInt(arg2, line);
            if (method == "shr")
                ctx.insn(INEG);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Num",
                                "shl", "(I)Lyeti/lang/Num;");
        } else if (arg2 instanceof NumericConstant &&
                 ((NumericConstant) arg2).genInt(ctx, ii)) {
            ctx.visitLine(line);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Num",
                method, ii ? "(I)Lyeti/lang/Num;" : "(J)Lyeti/lang/Num;");
            ctx.forceType("yeti/lang/Num");
        } else {
            arg2.gen(ctx);
            ctx.visitLine(line);
            ctx.typeInsn(CHECKCAST, "yeti/lang/Num");
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Num",
                    method, "(Lyeti/lang/Num;)Lyeti/lang/Num;");
        }
        ctx.forceType("yeti/lang/Num");
    }
}
