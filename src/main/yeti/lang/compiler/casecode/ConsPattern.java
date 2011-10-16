package yeti.lang.compiler.casecode;

import yeti.renamed.asm3.Label;

final class ConsPattern extends AListPattern {
    private CasePattern hd;
    private CasePattern tl;

    ConsPattern(CasePattern hd, CasePattern tl) {
        this.hd = hd;
        this.tl = tl;
    }

    boolean listMatch(Ctx ctx, Label onFail, Label dropFail) {
        if (hd != ANY_PATTERN) {
            if (tl != ANY_PATTERN) {
                ctx.insn(DUP);
            } else {
                dropFail = onFail;
            }
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                                "first", "()Ljava/lang/Object;");
            hd.preparePattern(ctx);
            hd.tryMatch(ctx, dropFail, false);
        }
        if (tl != ANY_PATTERN) {
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                                "rest", "()Lyeti/lang/AList;");
            tl.preparePattern(ctx);
            tl.tryMatch(ctx, onFail, false);
        } else if (hd == ANY_PATTERN) {
            ctx.insn(POP);
        }
        return dropFail != onFail && !hd.irrefutable();
    }
}
