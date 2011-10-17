package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class InOpFun extends BoolBinOp {
    int line;

    InOpFun(int line) {
        type = YetiType.IN_TYPE;
        this.line = line;
        polymorph = true;
        coreFun = "in";
    }

    void binGenIf(Ctx ctx, Code arg1, Code arg2, Label to, boolean ifTrue) {
        arg2.gen(ctx);
        ctx.visitLine(line);
        if (ctx.compilation.isGCJ) {
            ctx.typeInsn(CHECKCAST, "yeti/lang/ByKey");
        }
        arg1.gen(ctx);
        ctx.visitLine(line);
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/ByKey",
                            "containsKey", "(Ljava/lang/Object;)Z");
        ctx.jumpInsn(ifTrue ? IFNE : IFEQ, to);
    }
}
