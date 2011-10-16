package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

import java.util.ArrayList;
import java.util.List;

final class StrOp extends StaticRef implements Binder {
    final static Code NOP_CODE = new Code() {
        void gen(Ctx ctx) {
        }
    };

    String method;
    String sig;
    YType argTypes[];

    final class StrApply extends Apply {
        StrApply prev;

        StrApply(Code arg, YType type, StrApply prev, int line) {
            super(type, NOP_CODE, arg, line);
            this.prev = prev;
        }

        Code apply(Code arg, YType res, int line) {
            return new StrApply(arg, res, this, line);
        }

        void genApply(Ctx ctx) {
            super.gen(ctx);
        }

        void gen(Ctx ctx) {
            genIf(ctx, null, false);
        }

        void genIf(Ctx ctx, Label to, boolean ifTrue) {
            List argv = new ArrayList();
            for (StrApply a = this; a != null; a = a.prev) {
                argv.add(a);
            }
            if (argv.size() != argTypes.length) {
                StrOp.this.gen(ctx);
                for (int i = argv.size() - 1; --i >= 0;)
                    ((StrApply) argv.get(i)).genApply(ctx);
                return;
            }
            ((StrApply) argv.get(argv.size() - 1)).arg.gen(ctx);
            ctx.visitLine(line);
            ctx.typeInsn(CHECKCAST, "java/lang/String");
            for (int i = 0, last = argv.size() - 2; i <= last; ++i) {
                StrApply a = (StrApply) argv.get(last - i);
                if (a.arg.type.deref().type == YetiType.STR) {
                    a.arg.gen(ctx);
                    ctx.typeInsn(CHECKCAST, "java/lang/String");
                } else {
                    JavaExpr.convertedArg(ctx, a.arg, argTypes[i], a.line);
                }
            }
            ctx.visitLine(line);
            ctx.methodInsn(INVOKEVIRTUAL, "java/lang/String",
                                method, sig);
            if (to != null) { // really genIf
                ctx.jumpInsn(ifTrue ? IFNE : IFEQ, to);
            } else if (type.deref().type == YetiType.STR) {
                ctx.forceType("java/lang/String;");
            } else {
                JavaExpr.convertValue(ctx, argTypes[argTypes.length - 1]);
            }
        }
    }

    StrOp(String fun, String method, String sig, YType type) {
        super("yeti/lang/std$" + mangle(fun), "_", type, null, false, 0);
        this.method = method;
        this.sig = sig;
        binder = this;
        argTypes = JavaTypeReader.parseSig1(1, sig);
    }

    public BindRef getRef(int line) {
        return this;
    }

    Code apply(final Code arg, final YType res, final int line) {
        return new StrApply(arg, res, null, line);
    }

    boolean flagop(int fl) {
        return (fl & STD_CONST) != 0;
    }
}
