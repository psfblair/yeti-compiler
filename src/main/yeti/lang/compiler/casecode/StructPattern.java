package yeti.lang.compiler.casecode;

import yeti.renamed.asm3.Label;

final class StructPattern extends CasePattern {
    private String[] names;
    private CasePattern[] patterns;

    StructPattern(String[] names, CasePattern[] patterns) {
        this.names = names;
        this.patterns = patterns;
    }

    int preparePattern(Ctx ctx) {
        return 1;
    }

    void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
        boolean dropped = false;
        Label failed = preserve ? onFail : new Label();
        for (int i = 0; i < names.length; ++i) {
            if (patterns[i] == ANY_PATTERN)
                continue;
            if (preserve || i != names.length - 1)
                ctx.insn(DUP);
            else dropped = true;
            ctx.ldcInsn(names[i]);
            ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                      "get", "(Ljava/lang/String;)Ljava/lang/Object;");
            patterns[i].preparePattern(ctx);
            patterns[i].tryMatch(ctx, i < names.length - 1
                                        ? failed : onFail, false);
        }
        if (!preserve && names.length > 1) {
            Label ok = new Label();
            ctx.jumpInsn(GOTO, ok);
            ctx.visitLabel(failed);
            ctx.insn(POP);
            ctx.jumpInsn(GOTO, onFail);
            ctx.visitLabel(ok);
            if (!dropped)
                ctx.insn(POP);
        }
    }
}
