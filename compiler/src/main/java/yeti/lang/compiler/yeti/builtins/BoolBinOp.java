package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

abstract class BoolBinOp extends BinOpRef {
    void binGen(Ctx ctx, Code arg1, Code arg2) {
        Label label = new Label();
        binGenIf(ctx, arg1, arg2, label, false);
        ctx.genBoolean(label);
    }
}
