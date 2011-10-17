package yeti.lang.compiler.yeti.builtins;

final class StrChar extends BinOpRef {
    private int line;

    StrChar(int line) {
        coreFun = "strChar";
        type = YetiType.STR_TO_NUM_TO_STR;
        this.line = line;
    }

    void binGen(Ctx ctx, Code arg1, Code arg2) {
        arg1.gen(ctx);
        ctx.typeInsn(CHECKCAST, "java/lang/String");
        ctx.genInt(arg2, line);
        ctx.insn(DUP);
        ctx.intConst(1);
        ctx.insn(IADD);
        ctx.methodInsn(INVOKEVIRTUAL, "java/lang/String",
                       "substring", "(II)Ljava/lang/String;");
        ctx.forceType("java/lang/String");
    }
}
