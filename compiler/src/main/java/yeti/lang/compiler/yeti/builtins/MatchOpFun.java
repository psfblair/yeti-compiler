package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

final class MatchOpFun extends BinOpRef implements CodeGen {
    private int line;
    private boolean yes;

    MatchOpFun(int line, boolean yes) {
        type = YetiType.STR2_PRED_TYPE;
        coreFun = mangle(yes ? "=~" : "!~");
        this.line = line;
        this.yes = yes;
    }

    void binGen(Ctx ctx, Code arg1, final Code arg2) {
        apply2nd(arg2, YetiType.STR2_PRED_TYPE, line).gen(ctx);
        ctx.visitApply(arg1, line);
    }

    public void gen2(Ctx ctx, Code arg2, int line) {
        ctx.typeInsn(NEW, "yeti/lang/Match");
        ctx.insn(DUP);
        arg2.gen(ctx);
        ctx.intConst(yes ? 1 : 0);
        ctx.visitLine(line);
        ctx.visitInit("yeti/lang/Match", "(Ljava/lang/Object;Z)V");
    }

    Code apply2nd(final Code arg2, final YType t, final int line) {
        if (line == 0) {
            throw new NullPointerException();
        }
        final Code matcher = new SimpleCode(this, arg2, t, line);
        if (!(arg2 instanceof StringConstant))
            return matcher;
        try {
            Pattern.compile(((StringConstant) arg2).str, Pattern.DOTALL);
        } catch (PatternSyntaxException ex) {
            throw new CompileException(line, 0,
                        "Bad pattern syntax: " + ex.getMessage());
        }
        return new Code() {
            { type = t; }

            void gen(Ctx ctx) {
                ctx.constant((yes ? "MATCH-FUN:" : "MATCH!FUN:")
                    .concat(((StringConstant) arg2).str), matcher);
            }
        };
    }

    void binGenIf(Ctx ctx, Code arg1, Code arg2, Label to, boolean ifTrue) {
        binGen(ctx, arg1, arg2);
        ctx.fieldInsn(GETSTATIC, "java/lang/Boolean",
                      "TRUE", "Ljava/lang/Boolean;");
        ctx.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }
}
