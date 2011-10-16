package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;

final class SelectMemberFun extends Code implements CodeGen {
    String[] names;
    
    SelectMemberFun(YType type, String[] names) {
        setType(type);
        this.names = names;
        this.polymorph = true;
    }

    public void gen2(Ctx ctx, Code param, int line) {
        for (int i = 1; i < names.length; ++i) {
            ctx.typeInsn(NEW, "yeti/lang/Compose");
            ctx.insn(DUP);
        }
        for (int i = names.length; --i >= 0;) {
            ctx.typeInsn(NEW, "yeti/lang/Selector");
            ctx.insn(DUP);
            ctx.ldcInsn(names[i]);
            ctx.visitInit("yeti/lang/Selector",
                          "(Ljava/lang/String;)V");
            if (i + 1 != names.length)
                ctx.visitInit("yeti/lang/Compose",
                        "(Ljava/lang/Object;Ljava/lang/Object;)V");
        }
    }

    public void gen(Ctx ctx) {
        StringBuffer buf = new StringBuffer("SELECTMEMBER");
        for (int i = 0; i < names.length; ++i) {
            buf.append(':');
            buf.append(names[i]);
        }
        ctx.constant(buf.toString(), new SimpleCode(this, null, getType(), 0));
    }
}