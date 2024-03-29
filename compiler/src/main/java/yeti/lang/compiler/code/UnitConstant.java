package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

public final class UnitConstant extends BindRef {
    private final Object NULL = new Object();

    public UnitConstant(YType type) {
        setType(type == null ? YetiType.UNIT_TYPE : type);
    }

    public void gen(Ctx ctx) {
        ctx.insn(ACONST_NULL);
    }

    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    Object valueKey() {
        return NULL;
    }
}
