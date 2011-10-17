package yeti.lang.compiler.casecode;

import yeti.lang.compiler.code.Ctx;
import yeti.renamed.asm3.Label;
import yeti.renamed.asm3.Opcodes;

abstract class CasePattern implements Opcodes {
    static final CasePattern ANY_PATTERN = new CasePattern() {
        void tryMatch(Ctx ctx, Label onFail, boolean preserve) {
            if (!preserve) {
                ctx.insn(POP);
            }
        }

        boolean irrefutable() {
            return true;
        }
    };

    int preparePattern(Ctx ctx) {
        return 1;
    }

    abstract void tryMatch(Ctx ctx, Label onFail, boolean preserve);

    boolean irrefutable() {
        return false;
    }
}
