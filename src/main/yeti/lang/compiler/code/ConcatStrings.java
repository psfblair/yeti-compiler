package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YetiType;

final class ConcatStrings extends Code {
    Code[] param;

    ConcatStrings(Code[] param) {
        setType(YetiType.STR_TYPE);
        this.param = param;
    }

    public void gen(Ctx ctx) {
        boolean arr = false;
        if (param.length > 2) {
            arr = true;
            ctx.intConst(param.length);
            ctx.typeInsn(ANEWARRAY, "java/lang/String");
        }
        for (int i = 0; i < param.length; ++i) {
            if (arr) {
                ctx.insn(DUP);
                ctx.intConst(i);
            }
            param[i].gen(ctx);
            if (param[i].getType().deref().getType() != YetiType.STR) {
                ctx.methodInsn(INVOKESTATIC, "java/lang/String",
                    "valueOf", "(Ljava/lang/Object;)Ljava/lang/String;");
            }
            if (arr)
                ctx.insn(AASTORE);
            else
                ctx.typeInsn(CHECKCAST, "java/lang/String");
        }
        if (arr) {
            ctx.methodInsn(INVOKESTATIC, "yeti/lang/Core",
                           "concat", "([Ljava/lang/String;)Ljava/lang/String;");
        } else if (param.length == 2) {
            ctx.methodInsn(INVOKEVIRTUAL, "java/lang/String",
                           "concat", "(Ljava/lang/String;)Ljava/lang/String;");
        }
        ctx.forceType("java/lang/String");
    }
}
