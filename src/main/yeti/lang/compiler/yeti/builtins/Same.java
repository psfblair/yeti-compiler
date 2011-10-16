package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class Same extends BoolBinOp {
    Same() {
        type = YetiType.EQ_TYPE;
        polymorph = true;
        coreFun = "same$q";
    }

    void binGenIf(Ctx ctx, Code arg1, Code arg2,
                  Label to, boolean ifTrue) {
        arg1.gen(ctx);
        arg2.gen(ctx);
        ctx.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }
}
