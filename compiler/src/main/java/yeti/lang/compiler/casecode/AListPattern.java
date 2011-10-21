package yeti.lang.compiler.casecode;

import yeti.lang.compiler.code.Ctx;
import yeti.renamed.asm3.Label;

abstract class AListPattern extends CasePattern {
    static final CasePattern EMPTY_PATTERN = new CasePattern() {
        void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
            ctx.insn(DUP);
            Label cont = new Label();
            ctx.jumpInsn(IFNULL, cont);
            if (preserve) {
                ctx.insn(DUP);
            }
            ctx.typeInsn(CHECKCAST, "yeti/lang/AIter");
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                                "isEmpty", "()Z");
            ctx.jumpInsn(IFEQ, onFail);
            if (preserve) {
                ctx.visitLabel(cont);
            } else {
                Label end = new Label();
                ctx.jumpInsn(GOTO, end);
                ctx.visitLabel(cont);
                ctx.insn(POP);
                ctx.visitLabel(end);
            }
        }
    };

    abstract boolean listMatch(Ctx ctx, Label onFail, Label dropFail);

    void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
        Label dropFail = preserve ? onFail : new Label();
        ctx.insn(DUP);
        ctx.jumpInsn(IFNULL, dropFail);
        ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
        ctx.insn(DUP);
        ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                            "isEmpty", "()Z");
        ctx.jumpInsn(IFNE, dropFail);
        if (preserve) {
            ctx.insn(DUP);
            dropFail = new Label();
        }
        if (listMatch(ctx, onFail, dropFail) || !preserve) {
            Label cont = new Label();
            ctx.jumpInsn(GOTO, cont);
            ctx.visitLabel(dropFail);
            ctx.insn(POP);
            ctx.jumpInsn(GOTO, onFail);
            ctx.visitLabel(cont);
        }
    }
}
