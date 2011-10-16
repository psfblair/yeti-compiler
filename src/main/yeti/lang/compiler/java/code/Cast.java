package yeti.lang.compiler.java.code;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.java.JavaExpr;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

final class Cast extends JavaExpr {
    boolean convert;

    Cast(Code code, YType type, boolean convert, int line) {
        super(code, null, null, line);
        this.type = type;
        this.line = line;
        this.convert = convert;
    }

    void gen(Ctx ctx) {
        if (convert) {
            convertedArg(ctx, object, type.deref(), line);
            return;
        }
        object.gen(ctx);
        if (type.deref().type == YetiType.UNIT) {
            ctx.insn(POP);
            ctx.insn(ACONST_NULL);
        }
    }
}
