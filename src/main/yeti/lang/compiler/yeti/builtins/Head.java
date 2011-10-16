package yeti.lang.compiler.yeti.builtins;

final class Head extends IsNullPtr {
    Head(int line) {
        super(YetiType.LIST_TO_A, "head", line);
    }

    void gen(Ctx ctx, Code arg, int line) {
        arg.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
        ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                            "first", "()Ljava/lang/Object;");
    }
}
