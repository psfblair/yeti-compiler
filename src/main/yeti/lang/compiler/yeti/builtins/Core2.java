package yeti.lang.compiler.yeti.builtins;

abstract class Core2 extends StaticRef {
    boolean derivePolymorph;

    Core2(String coreFun, YType type, int line) {
        super("yeti/lang/std$" + coreFun, "_", type, null, true, line);
    }

    Code apply(final Code arg1, YType res, int line1) {
        return new Apply(res, this, arg1, line1) {
            Code apply(final Code arg2, final YType res,
                       final int line2) {
                return new Code() {
                    {
                        type = res;
                        polymorph = derivePolymorph && arg1.polymorph
                                                    && arg2.polymorph;
                    }

                    void gen(Ctx ctx) {
                        genApply2(ctx, arg1, arg2, line2);
                    }
                };
            }
        };
    }

    abstract void genApply2(Ctx ctx, Code arg1, Code arg2, int line);
}
