package yeti.lang.compiler.yeti.builtins;

final class ClassOfExpr extends Code implements CodeGen {
    String className;

    ClassOfExpr(JavaType what, int array) {
        type = YetiType.CLASS_TYPE;
        String cn = what.dottedName();
        if (array != 0) {
            cn = 'L' + cn + ';';
            do {
                cn = "[".concat(cn);
            } while (--array > 0);
        }
        className = cn;
    }

    public void gen2(Ctx ctx, Code param, int line) {
        ctx.ldcInsn(className);
        ctx.methodInsn(INVOKESTATIC, "java/lang/Class",
            "forName", "(Ljava/lang/String;)Ljava/lang/Class;");
        ctx.forceType("java/lang/Class");
    }

    void gen(Ctx ctx) {
        ctx.constant("CLASS-OF:".concat(className),
                new SimpleCode(this, null, YetiType.CLASS_TYPE, 0));
    }

    boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }
}
