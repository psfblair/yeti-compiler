package yeti.lang.compiler.code;

public final class LoadVar extends Code {
    private int var;

    public void gen(Ctx ctx) {
        ctx.varInsn(ALOAD, var);
    }

    public int getVar() {
        return var;
    }

    public void setVar(int var) {
        this.var = var;
    }
}
