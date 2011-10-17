package yeti.lang.compiler.yeti.builtins;

final class Negate extends StaticRef implements CodeGen {
    Negate() {
        super("yeti/lang/std$negate", "_", YetiType.NUM_TO_NUM,
              null, false, 0);
    }

    public void gen2(Ctx ctx, Code arg, int line) {
        arg.gen(ctx);
        ctx.visitLine(line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Num");
        ctx.ldcInsn(new Long(0));
        ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Num",
                            "subFrom", "(J)Lyeti/lang/Num;");
        ctx.forceType("yeti/lang/Num");
    }

    Code apply(final Code arg1, final YType res1, final int line) {
        if (arg1 instanceof NumericConstant) {
            return new NumericConstant(((NumericConstant) arg1)
                        .num.subFrom(0));
        }
        return new SimpleCode(this, arg1, YetiType.NUM_TYPE, line);
    }
}
