package yeti.lang.compiler.yeti.builtins;

final class Compose extends Core2 {
    Compose(int line) {
        super("$d", YetiType.COMPOSE_TYPE, line);
        derivePolymorph = true;
    }

    void genApply2(Ctx ctx, Code arg1, Code arg2, int line) {
        ctx.typeInsn(NEW, "yeti/lang/Compose");
        ctx.insn(DUP);
        arg1.gen(ctx);
        arg2.gen(ctx);
        ctx.visitLine(line);
        ctx.visitInit("yeti/lang/Compose",
                      "(Ljava/lang/Object;Ljava/lang/Object;)V");
    }
}
