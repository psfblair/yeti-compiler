package yeti.lang.compiler.yeti.builtins;

final class Argv extends BindRef implements CodeGen {
    void gen(Ctx ctx) {
        ctx.fieldInsn(GETSTATIC, "yeti/lang/Core",
                             "ARGV", "Ljava/lang/ThreadLocal;");
        ctx.methodInsn(INVOKEVIRTUAL,
            "java/lang/ThreadLocal",
            "get", "()Ljava/lang/Object;");
    }

    public void gen2(Ctx ctx, Code value, int line) {
        ctx.fieldInsn(GETSTATIC, "yeti/lang/Core",
                     "ARGV", "Ljava/lang/ThreadLocal;");
        value.gen(ctx);
        ctx.methodInsn(INVOKEVIRTUAL,
            "java/lang/ThreadLocal",
            "get", "(Ljava/lang/Object;)V");
        ctx.insn(ACONST_NULL);
    }

    Code assign(final Code value) {
        return new SimpleCode(this, value, null, 0);
    }

    boolean flagop(int fl) {
        return (fl & (ASSIGN | DIRECT_BIND)) != 0;
    }
}
