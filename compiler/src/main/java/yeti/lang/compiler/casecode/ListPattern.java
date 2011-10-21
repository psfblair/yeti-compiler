package yeti.lang.compiler.casecode;

import yeti.lang.compiler.code.Ctx;
import yeti.renamed.asm3.Label;

final class ListPattern extends AListPattern {
    private CasePattern[] items;

    ListPattern(CasePattern[] items) {
        this.items = items;
    }

    boolean listMatch(Ctx ctx, Label onFail, Label dropFail) {
        boolean dropUsed = false;
        for (int i = 0; i < items.length; ++i) {
            if (i != 0) {
                ctx.insn(DUP);
                ctx.jumpInsn(IFNULL, dropFail);
                dropUsed = true;
            }
            if (items[i] != ANY_PATTERN) {
                ctx.insn(DUP);
                ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                                    "first", "()Ljava/lang/Object;");
                items[i].preparePattern(ctx);
                items[i].tryMatch(ctx, dropFail, false);
                dropUsed |= !items[i].irrefutable();
            }
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                                "next", "()Lyeti/lang/AIter;");
        }
        ctx.jumpInsn(IFNONNULL, onFail);
        return dropUsed;
    }
}
