package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class NotOp extends StaticRef {
    NotOp(int line) {
        super("yeti/lang/std$not", "_",
              YetiType.BOOL_TO_BOOL, null, false, line);
    }

    Code apply(final Code arg, YType res, int line) {
        return new Code() {
            { type = YetiType.BOOL_TYPE; }

            void genIf(Ctx ctx, Label to, boolean ifTrue) {
                arg.genIf(ctx, to, !ifTrue);
            }

            void gen(Ctx ctx) {
                Label label = new Label();
                arg.genIf(ctx, label, true);
                ctx.genBoolean(label);
            }
        };
    }
}
