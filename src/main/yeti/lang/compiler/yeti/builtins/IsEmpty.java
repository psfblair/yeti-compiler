package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class IsEmpty extends IsNullPtr {
    IsEmpty(int line) {
        super(YetiType.MAP_TO_BOOL, "empty$q", line);
    }

    void genIf(Ctx ctx, Code arg, Label to, boolean ifTrue, int line) {
        Label isNull = new Label(), end = new Label();
        arg.gen(ctx);
        ctx.visitLine(line);
        ctx.insn(DUP);
        ctx.jumpInsn(IFNULL, isNull);
        if (ctx.compilation.isGCJ)
            ctx.typeInsn(CHECKCAST, "yeti/lang/Coll");
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Coll",
                            "isEmpty", "()Z");
        ctx.jumpInsn(IFNE, ifTrue ? to : end);
        ctx.jumpInsn(GOTO, ifTrue ? end : to);
        ctx.visitLabel(isNull);
        ctx.insn(POP);
        if (ifTrue) {
            ctx.jumpInsn(GOTO, to);
        }
        ctx.visitLabel(end);
    }
}
