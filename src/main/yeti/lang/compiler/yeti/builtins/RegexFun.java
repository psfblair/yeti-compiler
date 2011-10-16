package yeti.lang.compiler.yeti.builtins;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class RegexFun extends StaticRef implements CodeGen {
    private String impl;
    private String funName;

    RegexFun(String fun, String impl, YType type,
             Binder binder, int line) {
        super("yeti/lang/std$" + fun, "_", type, null, false, line);
        this.funName = fun;
        this.binder = binder;
        this.impl = impl;
    }

    public void gen2(Ctx ctx, Code arg, int line) {
        ctx.typeInsn(NEW, impl);
        ctx.insn(DUP);
        arg.gen(ctx);
        ctx.visitLine(line);
        ctx.visitInit(impl, "(Ljava/lang/Object;)V");
    }

    Code apply(final Code arg, final YType t, final int line) {
        final Code f = new SimpleCode(this, arg, t, line);
        if (!(arg instanceof StringConstant))
            return f;
        try {
            Pattern.compile(((StringConstant) arg).str, Pattern.DOTALL);
        } catch (PatternSyntaxException ex) {
            throw new CompileException(line, 0,
                        "Bad pattern syntax: " + ex.getMessage());
        }
        return new Code() {
            { type = t; }

            void gen(Ctx ctx) {
                ctx.constant(funName + ":regex:" +
                    ((StringConstant) arg).str, f);
            }
        };
    }
}
