package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YetiType;

public final class StringConstant extends Code {
    String str;

    StringConstant(String str) {
        setType(YetiType.STR_TYPE);
        this.str = str;
    }

    public void gen(Ctx ctx) {
        ctx.ldcInsn(str);
    }

    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }

    Object valueKey() {
        return str;
    }
}
