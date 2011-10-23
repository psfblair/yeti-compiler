package yeti.lang.compiler.code;

import yeti.renamed.asm3.Label;

public class SeqExpr extends Code {
    private Code st;
    private Code result;

    SeqExpr(Code statement) {
        st = statement;
    }

    public void gen(Ctx ctx) {
        st.gen(ctx);
        ctx.insn(POP); // ignore the result of st expr
        result.gen(ctx);
    }

    public Code getSt() {
        return st;
    }

    public Code getResult() {
        return result;
    }

    public void genIf(Ctx ctx, Label to, boolean ifTrue) {
        st.gen(ctx);
        ctx.insn(POP); // ignore the result of st expr
        result.genIf(ctx, to, ifTrue);
    }

    public void markTail() {
        result.markTail();
    }
}

