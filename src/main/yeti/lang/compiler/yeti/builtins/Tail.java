package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class Tail extends IsNullPtr {
    Tail(int line) {
        super(YetiType.LIST_TO_LIST, "tail", line);
    }

    void gen(Ctx ctx, Code arg, int line) {
        arg.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
        ctx.insn(DUP);
        Label end = new Label();
        ctx.jumpInsn(IFNULL, end);
        ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                            "rest", "()Lyeti/lang/AList;");
        ctx.visitLabel(end);
        ctx.forceType("yeti/lang/AList");
    }
}
