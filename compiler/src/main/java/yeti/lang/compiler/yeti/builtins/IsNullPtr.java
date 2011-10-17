package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

class IsNullPtr extends StaticRef {
    private String libName;
    boolean normalIf;

    IsNullPtr(YType type, String fun, int line) {
        super("yeti/lang/std$" + fun, "_", type, null, true, line);
    }

    Code apply(final Code arg, final YType res,
               final int line) {
        return new Code() {
            { type = res; }

            void gen(Ctx ctx) {
                IsNullPtr.this.gen(ctx, arg, line);
            }

            void genIf(Ctx ctx, Label to, boolean ifTrue) {
                if (normalIf) {
                    super.genIf(ctx, to, ifTrue);
                } else {
                    IsNullPtr.this.genIf(ctx, arg, to, ifTrue, line);
                }
            }
        };
    }

    void gen(Ctx ctx, Code arg, int line) {
        Label label = new Label();
        genIf(ctx, arg, label, false, line);
        ctx.genBoolean(label);
    }

    void genIf(Ctx ctx, Code arg, Label to, boolean ifTrue, int line) {
        arg.gen(ctx);
        ctx.jumpInsn(ifTrue ? IFNULL : IFNONNULL, to);
    }
}
