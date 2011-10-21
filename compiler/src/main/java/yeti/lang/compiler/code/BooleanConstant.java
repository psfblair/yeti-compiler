package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Label;

final class BooleanConstant extends BindRef {
    boolean val;

    BooleanConstant(boolean val) {
        setType(YetiType.BOOL_TYPE);
        this.val = val;
    }

    protected boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    public void gen(Ctx ctx) {
        ctx.fieldInsn(GETSTATIC, "java/lang/Boolean",
                val ? "TRUE" : "FALSE", "Ljava/lang/Boolean;");
    }

    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        if (val == ifTrue) {
            ctx.jumpInsn(GOTO, to);
        }
    }

    Object valueKey() {
        return Boolean.valueOf(val);
    }
}

