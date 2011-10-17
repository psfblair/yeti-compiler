package yeti.lang.compiler.yeti.builtins;

final class Cons extends BinOpRef {
    private int line;

    Cons(int line) {
        type = YetiType.CONS_TYPE;
        coreFun = "$c$c";
        polymorph = true;
        this.line = line;
    }

    void binGen(Ctx ctx, Code arg1, Code arg2) {
        String lclass = "yeti/lang/LList";
        if (arg2.type.deref().param[1].deref()
                != YetiType.NO_TYPE) {
            lclass = "yeti/lang/LMList";
        }
        ctx.visitLine(line);
        ctx.typeInsn(NEW, lclass);
        ctx.insn(DUP);
        arg1.gen(ctx);
        arg2.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
        ctx.visitInit(lclass,
                      "(Ljava/lang/Object;Lyeti/lang/AList;)V");
        ctx.forceType("yeti/lang/AList");
    }
}
