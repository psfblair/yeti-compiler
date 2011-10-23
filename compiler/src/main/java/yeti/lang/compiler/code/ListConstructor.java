package yeti.lang.compiler.code;

import java.util.Arrays;
import java.util.List;

final class ListConstructor extends Code implements CodeGen {
    private Code[] items;
    private List key;

    ListConstructor(Code[] items) {
        int i;
        this.items = items;
        for (i = 0; i < items.length; ++i)
            if (!items[i].flagop(CONST))
                return;
        // good, got constant list
        Object[] ak = new Object[items.length + 1];
        ak[0] = "LIST";
        for (i = 0; i < items.length; ++i)
            ak[i + 1] = items[i].valueKey();
        key = Arrays.asList(ak);
    }

    public void gen2(Ctx ctx, Code param, int line) {
        for (int i = 0; i < items.length; ++i) {
            if (!(items[i] instanceof Range)) {
                ctx.typeInsn(NEW, "yeti/lang/LList");
                ctx.insn(DUP);
            }
            items[i].gen(ctx);
        }
        ctx.insn(ACONST_NULL);
        for (int i = items.length; --i >= 0;) {
            if (items[i] instanceof Range) {
                ctx.methodInsn(INVOKESTATIC, "yeti/lang/ListRange",
                        "range", "(Ljava/lang/Object;Ljava/lang/Object;"
                                + "Lyeti/lang/AList;)Lyeti/lang/AList;");
            } else {
                ctx.visitInit("yeti/lang/LList",
                              "(Ljava/lang/Object;Lyeti/lang/AList;)V");
            }
        }
    }

    public void gen(Ctx ctx) {
        if (items.length == 0) {
            ctx.insn(ACONST_NULL);
            return;
        }
        if (key == null) {
            gen2(ctx, null, 0);
        } else {
            ctx.constant(key, new SimpleCode(this, null, getType(), 0));
        }
        ctx.forceType("yeti/lang/AList");
    }

    Object valueKey() {
        return key;
    }

    public boolean flagop(int fl) {
        return (fl & STD_CONST) != 0 && (key != null || items.length == 0) ||
               (fl & EMPTY_LIST) != 0 && items.length == 0 ||
               (fl & LIST_RANGE) != 0 && items.length != 0
                    && items[0] instanceof Range;
    }
}

