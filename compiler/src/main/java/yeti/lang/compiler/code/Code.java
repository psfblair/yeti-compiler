package yeti.lang.compiler.code;

import yeti.lang.compiler.closure.Apply;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Label;
import yeti.renamed.asm3.Opcodes;

public abstract class Code implements Opcodes {
    // constants used by flagop
    public static final int CONST      = 1;
    public static final int PURE       = 2;
    
    // for bindrefs, mark as used lvalue
    public static final int ASSIGN     = 4;
    static final int INT_NUM    = 8;

    // Comparision operators use this for some optimisation.
    static final int EMPTY_LIST = 0x10;

    // no capturing
    public static final int DIRECT_BIND = 0x20;

    // normal constant is also pure and don't need capturing
    static final int STD_CONST = CONST | PURE | DIRECT_BIND;
    
    // this which is not captured
    static final int DIRECT_THIS = 0x40;

    // capture that requires bounding function to initialize its module
    protected static final int MODULE_REQUIRED = 0x80;

    // code object is a list range
    static final int LIST_RANGE = 0x100;

    private YType type;
    private boolean polymorph;

    /**
     * Generates into ctx a bytecode that (when executed in the JVM)
     * results in a value pushed into stack.
     * That value is of course the value of that code snippet
     * after evaluation.
     */
    public abstract void gen(Ctx ctx);

    // Used to tell that this code is at tail position in a function.
    // Useful for doing tail call optimisations.
    protected void markTail() {
    }

    public boolean flagop(int flag) {
        return false;
    }

    public YType getType() {
        return type;
    }

    public void setType(YType type) {
        this.type = type;
    }

    public boolean isPolymorph() {
        return polymorph;
    }

    public void setPolymorph(boolean polymorph) {
        this.polymorph = polymorph;
    }

    // Some "functions" may have special kinds of apply
    Code apply(Code arg, YType res, int line) {
        return new Apply(res, this, arg, line);
    }

    Code apply2nd(final Code arg2, final YType t, int line) {
        return new Code() {
            { type = t; }

            public void gen(Ctx ctx) {
                ctx.typeInsn(NEW, "yeti/lang/Bind2nd");
                ctx.insn(DUP);
                Code.this.gen(ctx);
                arg2.gen(ctx);
                ctx.visitInit("yeti/lang/Bind2nd",
                              "(Ljava/lang/Object;Ljava/lang/Object;)V");
            }
        };
    }

    // Not used currently. Should allow some custom behaviour
    // on binding (possibly useful for inline-optimisations).
    /*BindRef bindRef() {
        return null;
    }*/

    // When the code is a lvalue, then this method returns code that
    // performs the lvalue assigment of the value given as argument.
    protected Code assign(Code value) {
        return null;
    }

    // Boolean codes have ability to generate jumps.
    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        gen(ctx);
        ctx.fieldInsn(GETSTATIC, "java/lang/Boolean",
                "TRUE", "Ljava/lang/Boolean;");
        ctx.jumpInsn(ifTrue ? IF_ACMPEQ : IF_ACMPNE, to);
    }

    // Used for sharing embedded constant objects
    Object valueKey() {
        return this;
    }

    // Called by bind for direct bindings
    // bindings can use this for "preparation"
    public boolean prepareConst(Ctx ctx) {
        return flagop(CONST);
    }

    protected static final String javaType(YType t) {
        t = t.deref();
        switch (t.getType()) {
            case YetiType.STR: return "java/lang/String";
            case YetiType.NUM: return "yeti/lang/Num";
            case YetiType.CHAR: return "java/lang/Character";
            case YetiType.FUN: return "yeti/lang/Fun";
            case YetiType.STRUCT: return "yeti/lang/Struct";
            case YetiType.VARIANT: return "yeti/lang/Tag";
            case YetiType.MAP: {
                int k = t.getParam()[2].deref().getType();
                if (k != YetiType.LIST_MARKER)
                    return "java/lang/Object";
                if (t.getParam()[1].deref().getType() == YetiType.NUM)
                    return "yeti/lang/MList";
                return "yeti/lang/AList";
            }
            case YetiType.JAVA: return t.getJavaType().className();
        }
        return "java/lang/Object";
    }

    static final char[] mangle =
        "jQh$oBz  apCmds          cSlegqt".toCharArray();

    protected static final String mangle(String s) {
        char[] a = s.toCharArray();
        char[] to = new char[a.length * 2];
        int l = 0;
        for (int i = 0, cnt = a.length; i < cnt; ++i, ++l) {
            char c = a[i];
            if (c > ' ' && c < 'A' && (to[l + 1] = mangle[c - 33]) != ' ') {
            } else if (c == '^') {
                to[l + 1] = 'v';
            } else if (c == '|') {
                to[l + 1] = 'I';
            } else if (c == '~') {
                to[l + 1] = '_';
            } else {
                to[l] = c;
                continue;
            }
            to[l++] = '$';
        }
        return new String(to, 0, l);
    }
}
