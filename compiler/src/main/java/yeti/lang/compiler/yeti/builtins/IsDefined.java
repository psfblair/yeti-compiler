package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class IsDefined extends IsNullPtr {
    IsDefined(int line) {
        super(YetiType.A_TO_BOOL, "defined$q", line);
    }

    void genIf(Ctx ctx, Code arg, Label to, boolean ifTrue, int line) {
        Label isNull = new Label(), end = new Label();
        arg.gen(ctx);
        ctx.insn(DUP);
        ctx.jumpInsn(IFNULL, isNull);
        ctx.fieldInsn(GETSTATIC, "yeti/lang/Core",
                             "UNDEF_STR", "Ljava/lang/String;");
        ctx.jumpInsn(IF_ACMPEQ, ifTrue ? end : to);
        ctx.jumpInsn(GOTO, ifTrue ? to : end);
        ctx.visitLabel(isNull);
        ctx.insn(POP);
        if (!ifTrue) {
            ctx.jumpInsn(GOTO, to);
        }
        ctx.visitLabel(end);
    }
}
