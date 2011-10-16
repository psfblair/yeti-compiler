package yeti.lang.compiler.casecode;

import yeti.renamed.asm3.Label;

final class VariantPattern extends CasePattern {
    String variantTag;
    CasePattern variantArg;

    VariantPattern(String tagName, CasePattern arg) {
        variantTag = tagName;
        variantArg = arg;
    }

    int preparePattern(Ctx ctx) {
        ctx.typeInsn(CHECKCAST, "yeti/lang/Tag");
        ctx.insn(DUP);
        ctx.fieldInsn(GETFIELD, "yeti/lang/Tag", "name",
                           "Ljava/lang/String;");
        return 2; // TN
    }

    void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
        Label jumpTo = onFail;
        if (preserve) {
            ctx.insn(DUP); // TNN
            ctx.ldcInsn(variantTag);
            ctx.jumpInsn(IF_ACMPNE, onFail); // TN
            if (variantArg == ANY_PATTERN) {
                return;
            }
            ctx.insn(SWAP); // NT
            ctx.insn(DUP_X1); // TNT
        } else if (variantArg == ANY_PATTERN) {
            ctx.insn(SWAP); // NT
            ctx.insn(POP); // N
            ctx.ldcInsn(variantTag);
            ctx.jumpInsn(IF_ACMPNE, onFail);
            return;
        } else {
            Label cont = new Label(); // TN
            ctx.ldcInsn(variantTag);
            ctx.jumpInsn(IF_ACMPEQ, cont); // T
            ctx.insn(POP);
            ctx.jumpInsn(GOTO, onFail);
            ctx.visitLabel(cont);
        }
        ctx.fieldInsn(GETFIELD, "yeti/lang/Tag", "value",
                             "Ljava/lang/Object;"); // TNt (t)
        variantArg.preparePattern(ctx);
        variantArg.tryMatch(ctx, onFail, false); // TN ()
    }
}
