package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;
import yeti.renamed.asm3.Label;

final class ConditionalExpr extends Code {
    Code[][] choices;

    ConditionalExpr(YType type, Code[][] choices, boolean poly) {
        this.setType(type);
        this.choices = choices;
        this.setPolymorph(poly);
    }

    public void gen(Ctx ctx) {
        Label end = new Label();
        for (int i = 0, last = choices.length - 1; i <= last; ++i) {
            Label jmpNext = i < last ? new Label() : end;
            if (choices[i].length == 2) {
                choices[i][1].genIf(ctx, jmpNext, false); // condition
                choices[i][0].gen(ctx); // body
                ctx.jumpInsn(GOTO, end);
            } else {
                choices[i][0].gen(ctx);
            }
            ctx.visitLabel(jmpNext);
        }
        ctx.insn(-1); // reset type
    }

    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        Label end = new Label();
        for (int i = 0, last = choices.length - 1; i <= last; ++i) {
            Label jmpNext = i < last ? new Label() : end;
            if (choices[i].length == 2) {
                choices[i][1].genIf(ctx, jmpNext, false); // condition
                choices[i][0].genIf(ctx, to, ifTrue); // body
                ctx.jumpInsn(GOTO, end);
            } else {
                choices[i][0].genIf(ctx, to, ifTrue);
            }
            ctx.visitLabel(jmpNext);
        }
    }

    protected void markTail() {
        for (int i = choices.length; --i >= 0;) {
            choices[i][0].markTail();
        }
    }
}
