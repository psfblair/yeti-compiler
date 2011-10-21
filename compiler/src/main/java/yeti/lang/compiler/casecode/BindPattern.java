package yeti.lang.compiler.casecode;

import yeti.lang.compiler.code.BindRef;
import yeti.lang.compiler.code.Binder;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.yeti.type.YType;
import yeti.renamed.asm3.Label;

final class BindPattern extends CasePattern implements Binder {
    private CaseExpr caseExpr;
    private int nth;

    BindRef param = new BindRef() {
        public void gen(Ctx ctx) {
            ctx.load(caseExpr.paramStart + nth);
        }
    };

    BindPattern(CaseExpr caseExpr, YType type) {
        this.caseExpr = caseExpr;
        param.setBinder(this);
        param.setType(type);
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
