package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;

abstract class SelectMember extends BindRef implements CodeGen {
    private boolean assigned = false;
    Code st;
    String name;
    int line;

    SelectMember(YType type, Code st, String name, int line, boolean polymorph) {
        setType(type);
        setPolymorph(polymorph);
        this.st = st;
        this.name = name;
        this.line = line;
    }

    public void gen(Ctx ctx) {
        st.gen(ctx);
        ctx.visitLine(line);
        if (ctx.getCompilation().isGCJ)
            ctx.typeInsn(CHECKCAST, "yeti/lang/Struct");
        ctx.ldcInsn(name);
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                "get", "(Ljava/lang/String;)Ljava/lang/Object;");
    }

    public void gen2(Ctx ctx, Code setValue, int _) {
        st.gen(ctx);
        ctx.visitLine(line);
        if (ctx.getCompilation().isGCJ)
            ctx.typeInsn(CHECKCAST, "yeti/lang/Struct");
        ctx.ldcInsn(name);
        setValue.gen(ctx);
        ctx.visitLine(line);
        ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                "set", "(Ljava/lang/String;Ljava/lang/Object;)V");
        ctx.insn(ACONST_NULL);
    }

    public Code assign(final Code setValue) {
        if (!assigned && !mayAssign()) {
            return null;
        }
        assigned = true;
        return new SimpleCode(this, setValue, null, 0);
    }

    abstract boolean mayAssign();
}

