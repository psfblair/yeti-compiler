package yeti.lang.compiler.casecode;

import yeti.renamed.asm3.Label;

final class BindPattern extends CasePattern implements Binder {
    private yeti.lang.compiler.CaseExpr caseExpr;
    private int nth;

    BindRef param = new BindRef() {
        void gen(Ctx ctx) {
            ctx.load(caseExpr.paramStart + nth);
        }
    };

    BindPattern(yeti.lang.compiler.CaseExpr caseExpr, YType type) {
        this.caseExpr = caseExpr;
        param.binder = this;
        param.type = type;
        nth = caseExpr.paramCount++;
    }

    public BindRef getRef(int line) {
        return param;
    }

    void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
        if (preserve) {
            ctx.insn(DUP);
        }
        ctx.varInsn(ASTORE, caseExpr.paramStart + nth);
    }

    boolean irrefutable() {
        return true;
    }
}
