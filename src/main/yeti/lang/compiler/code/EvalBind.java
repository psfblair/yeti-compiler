package yeti.lang.compiler.code;

import yeti.lang.compiler.closure.CaptureWrapper;
import yeti.lang.compiler.yeti.YetiEval;
import yeti.renamed.asm3.Opcodes;

final class EvalBind implements Binder, CaptureWrapper, Opcodes, CodeGen {
    YetiEval.Binding bind;

    EvalBind(YetiEval.Binding bind) {
        this.bind = bind;
    }

    public void gen2(Ctx ctx, Code value, int line) {
        genPreGet(ctx);
        genSet(ctx, value);
        ctx.insn(ACONST_NULL);
    }

    public BindRef getRef(int line) {
        return new BindRef() {
            {
                setType(bind.getType());
                setBinder(EvalBind.this);
                polymorph = !bind.isMutable() && bind.isPolymorph();
            }

            public void gen(Ctx ctx) {
                genPreGet(ctx);
                genGet(ctx);
            }

            protected Code assign(final Code value) {
                return bind.isMutable() ?
                        new SimpleCode(EvalBind.this, value, null, 0) : null;
            }

            protected boolean flagop(int fl) {
                return (fl & ASSIGN) != 0 && bind.isMutable();
            }

            CaptureWrapper capture() {
                return EvalBind.this;
            }
        };
    }

    public void genPreGet(Ctx ctx) {
        ctx.intConst(bind.getBindId());
        ctx.methodInsn(INVOKESTATIC, "yeti/lang/compiler/YetiEval",
                       "getBind", "(I)[Ljava/lang/Object;");
    }

    public void genGet(Ctx ctx) {
        ctx.intConst(bind.getIndex());
        ctx.insn(AALOAD);
    }

    public void genSet(Ctx ctx, Code value) {
        ctx.intConst(bind.getIndex());
        value.gen(ctx);
        ctx.insn(AASTORE);
    }

    public Object captureIdentity() {
        return this;
    }

    public String captureType() {
        return "[Ljava/lang/Object;";
    }
}
