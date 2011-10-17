package yeti.lang.compiler.yeti.builtins;

final class LazyCons extends BinOpRef {
    private int line;

    LazyCons(int line) {
        type = YetiType.LAZYCONS_TYPE;
        coreFun = "$c$d";
        polymorph = true;
        this.line = line;
    }

    void binGen(Ctx ctx, Code arg1, Code arg2) {
        ctx.visitLine(line);
        ctx.typeInsn(NEW, "yeti/lang/LazyList");
        ctx.insn(DUP);
        arg1.gen(ctx);
        arg2.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Fun");
        ctx.visitInit("yeti/lang/LazyList",
                      "(Ljava/lang/Object;Lyeti/lang/Fun;)V");
        ctx.forceType("yeti/lang/AList");
    }
}
