package yeti.lang.compiler.yeti.builtins;

final class Escape extends IsNullPtr {
    Escape(int line) {
        super(YetiType.WITH_EXIT_TYPE, "withExit", line);
        normalIf = true;
    }

    void gen(Ctx ctx, Code block, int line) {
        block.gen(ctx);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Fun");
        ctx.methodInsn(INVOKESTATIC, "yeti/lang/EscapeFun", "with",
                            "(Lyeti/lang/Fun;)Ljava/lang/Object;");
    }
}
