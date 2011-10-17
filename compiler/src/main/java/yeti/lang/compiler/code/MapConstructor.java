package yeti.lang.compiler.code;

final class MapConstructor extends Code {
    Code[] keyItems;
    Code[] items;

    MapConstructor(Code[] keyItems, Code[] items) {
        this.keyItems = keyItems;
        this.items = items;
    }

    public void gen(Ctx ctx) {
        ctx.typeInsn(NEW, "yeti/lang/Hash");
        ctx.insn(DUP);
        if (keyItems.length > 16) {
            ctx.intConst(keyItems.length);
            ctx.visitInit("yeti/lang/Hash", "(I)V");
        } else {
            ctx.visitInit("yeti/lang/Hash", "()V");
        }
        for (int i = 0; i < keyItems.length; ++i) {
            ctx.insn(DUP);
            keyItems[i].gen(ctx);
            items[i].gen(ctx);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Hash", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
            ctx.insn(POP);
        }
    }

    protected boolean flagop(int fl) {
        return (fl & EMPTY_LIST) != 0 && keyItems.length == 0;
    }
}

