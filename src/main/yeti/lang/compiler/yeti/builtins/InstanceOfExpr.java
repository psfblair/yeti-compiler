package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class InstanceOfExpr extends Code {
    Code expr;
    String className;

    InstanceOfExpr(Code expr, JavaType what) {
        type = YetiType.BOOL_TYPE;
        this.expr = expr;
        className = what.className();
    }

    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        expr.gen(ctx);
        ctx.typeInsn(INSTANCEOF, className);
        ctx.jumpInsn(ifTrue ? IFNE : IFEQ, to);
    }

    void gen(Ctx ctx) {
        Label label = new Label();
        genIf(ctx, label, false);
        ctx.genBoolean(label);
    }
}
