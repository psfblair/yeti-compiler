package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class BoolOpFun extends BoolBinOp implements Binder {
    boolean orOp;

    BoolOpFun(boolean orOp) {
        this.type = YetiType.BOOLOP_TYPE;
        this.orOp = orOp;
        this.binder = this;
        markTail2 = true;
        coreFun = orOp ? "or" : "and";
    }

    public BindRef getRef(int line) {
        return this;
    }

    void binGen(Ctx ctx, Code arg1, Code arg2) {
        if (arg2 instanceof CompareFun) {
            super.binGen(ctx, arg1, arg2);
        } else {
            Label label = new Label(), end = new Label();
            arg1.genIf(ctx, label, orOp);
            arg2.gen(ctx);
            ctx.jumpInsn(GOTO, end);
            ctx.visitLabel(label);
            ctx.fieldInsn(GETSTATIC, "java/lang/Boolean",
                    orOp ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
            ctx.visitLabel(end);
        }
    }

    void binGenIf(Ctx ctx, Code arg1, Code arg2, Label to, boolean ifTrue) {
        if (orOp == ifTrue) {
            arg1.genIf(ctx, to, orOp);
            arg2.genIf(ctx, to, orOp);
        } else {
            Label noJmp = new Label();
            arg1.genIf(ctx, noJmp, orOp);
            arg2.genIf(ctx, to, !orOp);
            ctx.visitLabel(noJmp);
        }
    }
}
