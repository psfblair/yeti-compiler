package yeti.lang.compiler.casecode;

import yeti.renamed.asm3.Label;

final class ConstPattern extends CasePattern {
    Code v;

    ConstPattern(Code value) {
        v = value;
    }

    void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
        if (preserve) {
            ctx.insn(DUP);
        }
        v.gen(ctx);
        ctx.methodInsn(INVOKEVIRTUAL, "java/lang/Object",
                            "equals", "(Ljava/lang/Object;)Z");
        ctx.jumpInsn(IFEQ, onFail);
    }
}
