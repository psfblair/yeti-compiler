package yeti.lang.compiler.code;

import yeti.lang.compiler.closure.CaptureWrapper;
import yeti.lang.compiler.closure.Closure;
import yeti.lang.compiler.closure.Function;
import yeti.renamed.asm3.Label;

public final class BindExpr extends SeqExpr implements Binder, CaptureWrapper {
    private int id;
    private int mvar = -1;
    private final boolean var;
    private String javaType;
    private String javaDescr;
    private Closure closure;

    private boolean assigned;
    private boolean captured;
    Ref refs;
    int evalId = -1;
    private boolean directBind;
    private String directField;
    private String myClass;

    public class Ref extends BindRef {
        private int arity;
        Ref next;

        public void gen(Ctx ctx) {
            if (directBind) {
                getSt().gen(ctx);
            } else {
                genPreGet(ctx);
                genGet(ctx);
            }
        }

        public void setArity(int arity) {
            this.arity = arity;
        }

        protected Code assign(final Code value) {
            if (!var) {
                return null;
            }
            assigned = true;
            return new Code() {
                public void gen(Ctx ctx) {
                    genLocalSet(ctx, value);
                    ctx.insn(ACONST_NULL);
                }
            };
        }

        protected boolean flagop(int fl) {
            if ((fl & ASSIGN) != 0)
                return var ? assigned = true : false;
            if ((fl & CONST) != 0)
                return directBind;
            if ((fl & DIRECT_BIND) != 0)
                return directBind || directField != null;
            if ((fl & MODULE_REQUIRED) != 0)
                return directField != null;
            return (fl & PURE) != 0 && !var;
        }

        CaptureWrapper capture() {
            captured = true;
            return var ? BindExpr.this : null;
        }

        Code unref(boolean force) {
            return force || directBind ? getSt() : null;
        }

        void forceDirect() {
            directField = "";
        }
    }

    BindExpr(Code expr, boolean var) {
        super(expr);
        this.var = var;
    }

    public BindRef getRef(int line) {
        //BindRef res = st.bindRef();
        //if (res == null)
        Ref res = new Ref();
        res.setBinder(this);
        res.type = getSt().type;
        res.polymorph = !var && getSt().polymorph;
        res.next = refs;
        if (getSt() instanceof Function)
            res.origin = res;
        return refs = res;
    }

    public Object captureIdentity() {
        return mvar == -1 ? (Object) this : closure;
    }

    public String captureType() {
        if (javaDescr == null)
            throw new IllegalStateException(toString());
        return mvar == -1 ? javaDescr : "[Ljava/lang/Object;";
    }

    public void genPreGet(Ctx ctx) {
        if (mvar == -1) {
            if (directField == null) {
                ctx.load(id).forceType(javaType);
            } else {
                ctx.fieldInsn(GETSTATIC, myClass, directField, javaDescr);
            }
        } else {
            ctx.load(mvar).forceType("[Ljava/lang/Object;");
        }
    }

    public void genGet(Ctx ctx) {
        if (mvar != -1) {
            ctx.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            ctx.intConst(id);
            ctx.insn(AALOAD);
        }
    }

    public void genSet(Ctx ctx, Code value) {
        if (directField == null) {
            ctx.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            ctx.intConst(id);
            value.gen(ctx);
            ctx.insn(AASTORE);
        } else {
            value.gen(ctx);
            ctx.fieldInsn(PUTSTATIC, myClass, directField, javaDescr);
        }
    }

    public boolean isAssigned() {
        return assigned;
    }

    public boolean isCaptured() {
        return captured;
    }

    public void setMVarId(Closure closure, int arrayId, int index) {
        this.closure = closure;
        mvar = arrayId;
        id = index;
    }

    private void genLocalSet(Ctx ctx, Code value) {
        if (mvar == -1) {
            value.gen(ctx);
            if (!javaType.equals("java/lang/Object"))
                ctx.typeInsn(CHECKCAST, javaType);
            if (directField == null) {
                ctx.varInsn(ASTORE, id);
            } else {
                ctx.fieldInsn(PUTSTATIC, myClass, directField, javaDescr);
            }
        } else {
            ctx.load(mvar).intConst(id);
            value.gen(ctx);
            ctx.insn(AASTORE);
        }
    }
    
    // called by Function.prepareConst when this bastard mutates into method
    void setCaptureType(String type) {
        javaDescr = javaType = "[Ljava/lang/Object;";
        javaType = type;
        javaDescr = type.charAt(0) == '[' ? type : 'L' + type + ';';
    }

    void genBind(Ctx ctx) {
        setCaptureType(javaType(getSt().type));
        if (ctx == null)
            return; // named lambdas use genBind for initializing the expr
        if (!var && getSt().prepareConst(ctx) && evalId == -1) {
            directBind = true;
            return;
        }
        if (directField == "") {
            myClass = ctx.className;
            directField = "$".concat(Integer.toString(ctx.constants.ctx.getFieldCounter()));
            ctx.constants.ctx.incrementFieldCounterBy(1);
            ctx.cw.visitField(ACC_STATIC | ACC_SYNTHETIC, directField,
                              javaDescr, null, null).visitEnd();
        } else if (mvar == -1) {
            id = ctx.getLocalVarCount();
            ctx.incrementFieldCounterBy(1);
        }
        genLocalSet(ctx, getSt());
        if (evalId != -1) {
            ctx.intConst(evalId);
            genPreGet(ctx);
            if (mvar != -1)
                ctx.intConst(id);
            ctx.methodInsn(INVOKESTATIC,
                "yeti/lang/compiler/YetiEval", "setBind",
                mvar == -1 ? "(ILjava/lang/Object;)V"
                           : "(I[Ljava/lang/Object;I)V");
        }
    }

    public void gen(Ctx ctx) {
        genBind(ctx);
        result.gen(ctx);
    }

    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        genBind(ctx);
        result.genIf(ctx, to, ifTrue);
    }
}

