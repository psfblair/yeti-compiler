package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Label;

final class LoopExpr extends Code {
    Code cond, body;

    LoopExpr(Code cond, Code body) {
        setType(YetiType.UNIT_TYPE);
        this.cond = cond;
        this.body = body;
    }

    public void gen(Ctx ctx) {
        Label start = new Label();
        Label end = new Label();
        ctx.visitLabel(start);
        ++ctx.tainted;
        cond.genIf(ctx, end, false);
        body.gen(ctx);
        --ctx.tainted;
        ctx.insn(POP);
        ctx.jumpInsn(GOTO, start);
        ctx.visitLabel(end);
        ctx.insn(ACONST_NULL);
    }
}

