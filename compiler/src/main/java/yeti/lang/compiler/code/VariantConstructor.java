package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;

import java.util.Arrays;

final class VariantConstructor extends Code implements CodeGen {
    String name;

    VariantConstructor(YType type, String name) {
        setType(type);
        this.name = name;
    }

    public void gen2(Ctx ctx, Code param, int line) {
        ctx.typeInsn(NEW, "yeti/lang/TagCon");
        ctx.insn(DUP);
        ctx.ldcInsn(name);
        ctx.visitInit("yeti/lang/TagCon", "(Ljava/lang/String;)V");
    }

    public void gen(Ctx ctx) {
        ctx.constant("TAG:".concat(name), new SimpleCode(this, null, getType(), 0));
    }

    Code apply(final Code arg, YType res, int line) {
        class Tag extends Code implements CodeGen {
            Object key;

            public void gen2(Ctx ctx, Code param, int line_) {
                ctx.typeInsn(NEW, "yeti/lang/Tag");
                ctx.insn(DUP);
                arg.gen(ctx);
                ctx.ldcInsn(name);
                ctx.visitInit("yeti/lang/Tag",
                              "(Ljava/lang/Object;Ljava/lang/String;)V");
            }

            public void gen(Ctx ctx) {
                if (key != null)
                    ctx.constant(key, new SimpleCode(this, null, getType(), 0));
                else
                    gen2(ctx, null, 0);
            }

            protected boolean flagop(int fl) {
               return (fl & STD_CONST) != 0 && key != null;
            }

            Object valueKey() {
                return key == null ? this : key;
            }
        };
        Tag tag = new Tag();
        tag.setType(res);
        tag.polymorph = arg.polymorph;
        if (arg.flagop(CONST)) {
            Object[] key = {"TAG", name, arg.valueKey()};
            tag.key = Arrays.asList(key);
        }
        return tag;
    }
}
