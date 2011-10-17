package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

abstract class BinOpRef extends BindRef {
    boolean markTail2;
    String coreFun;

    class Result extends Code {
        private Code arg1;
        private Code arg2;

        Result(Code arg1, Code arg2, YType res) {
            type = res;
            this.arg1 = arg1;
            this.arg2 = arg2;
        }

        void gen(Ctx ctx) {
            binGen(ctx, arg1, arg2);
        }

        void genIf(Ctx ctx, Label to, boolean ifTrue) {
            binGenIf(ctx, arg1, arg2, to, ifTrue);
        }

        void markTail() {
            if (markTail2) {
                arg2.markTail();
            }
        }
    }

    Code apply(final Code arg1, final YType res1, final int line) {
        return new Code() {
            { type = res1; }

            Code apply(Code arg2, YType res, int line) {
                return new Result(arg1, arg2, res);
            }

            void gen(Ctx ctx) {
                BinOpRef.this.gen(ctx);
                ctx.visitApply(arg1, line);
            }
        };
    }

    void gen(Ctx ctx) {
        ctx.fieldInsn(GETSTATIC, "yeti/lang/std$" + coreFun,
                           "_", "Lyeti/lang/Fun;");
    }

    abstract void binGen(Ctx ctx, Code arg1, Code arg2);

    void binGenIf(Ctx ctx, Code arg1, Code arg2, Label to, boolean ifTrue) {
        throw new UnsupportedOperationException("binGenIf");
    }

    boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }
}
