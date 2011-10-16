package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

public class SimpleCode extends Code {
    private Code param;
    private int line;
    private CodeGen impl;

    public SimpleCode(CodeGen impl, Code param, YType type, int line) {
        this.impl = impl;
        this.param = param;
        this.line = line;
        setType(type == null ? YetiType.UNIT_TYPE : type);
    }

    public void gen(Ctx ctx) {
        impl.gen2(ctx, param, line);
    }
}
