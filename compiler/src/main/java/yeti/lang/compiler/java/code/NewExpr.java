package yeti.lang.compiler.java.code;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.java.JavaExpr;
import yeti.lang.compiler.java.JavaType;
import yeti.lang.compiler.yeti.type.YetiType;

final class NewExpr extends JavaExpr {
    private YetiType.ClassBinding extraArgs;

    NewExpr(JavaType.Method init, Code[] args,
            YetiType.ClassBinding extraArgs, int line) {
        super(null, init, args, line);
        setType(init.classType);
        this.extraArgs = extraArgs;
    }

    public void gen(Ctx ctx) {
        String name = method.classType.javaType.className();
        ctx.typeInsn(NEW, name);
        ctx.insn(DUP);
        genCall(ctx, extraArgs.getCaptures(), INVOKESPECIAL);
        ctx.forceType(name);
    }
}