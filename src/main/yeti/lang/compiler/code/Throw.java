package yeti.lang.compiler.code;

import yeti.lang.compiler.CompileException;
import yeti.lang.compiler.java.JavaType;
import yeti.lang.compiler.yeti.type.YType;

//SHOULD THIS BE A JAVAEXPR?
final class Throw extends Code {
    Code throwable;

    Throw(Code throwable, YType type) {
        setType(type);
        this.throwable = throwable;
    }

    public void gen(Ctx ctx) {
        throwable.gen(ctx);
        JavaType t = throwable.getType().deref().getJavaType();
        if (t == null)
            throw new CompileException(null, "Internal error - throw argument type is unknown");
        ctx.typeInsn(CHECKCAST, t.className());
        ctx.insn(ATHROW);
    }
}

