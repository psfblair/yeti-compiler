package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;

final class KeyRefExpr extends Code implements CodeGen {
    Code val;
    Code key;
    int line;

    KeyRefExpr(YType type, Code val, Code key, int line) {
        setType(type);
        this.val = val;
        this.key = key;
        this.line = line;
    }

    public void gen(Ctx ctx) {
        val.gen(ctx);
        if (ctx.compilation.isGCJ) {
            ctx.typeInsn(CHECKCAST, "yeti/lang/ByKey");
        }
        key.gen(ctx);
        ctx.visitLine(line);
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/ByKey", "vget",
                              "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    public void gen2(Ctx ctx, Code setValue, int _) {
        val.gen(ctx);
        if (ctx.compilation.isGCJ) {
            ctx.typeInsn(CHECKCAST, "yeti/lang/ByKey");
        }
        key.gen(ctx);
        setValue.gen(ctx);
        ctx.visitLine(line);
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/ByKey",
                "put", "(Ljava/lang/Object;Ljava/lang/Object;)" +
                "Ljava/lang/Object;");
    }

    protected Code assign(final Code setValue) {
        return new SimpleCode(this, setValue, null, 0);
    }
}
