// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler java bytecode generator.
 *
 * Copyright (c) 2008 Madis Janson
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package yeti.lang.compiler.java;

import yeti.lang.compiler.CompileException;
import yeti.lang.compiler.closure.*;
import yeti.lang.compiler.code.*;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

import java.util.*;

public final class JavaClass extends CapturingClosure implements Runnable {
    private String className;
    private String[] implement;
    private YetiType.ClassBinding parentClass;
    private List fields = new ArrayList();
    private List methods = new ArrayList();
    private Field serialVersion;
    private JavaExpr superInit;
    private final boolean isPublic;
    private int captureCount;
    private Map accessors;
    private Ctx classCtx;
    YType classType;
    final Meth constr = new Meth();
    final Binder self;
    Binder superRef;

    static class Arg extends BindRef implements Binder {
        int argn;
        final YType javaType;
        private boolean isSuper;

        Arg(YType type, boolean isSuper) {
            this.javaType = type;
            this.setType(JavaType.convertValueType(type));
            this.isSuper = isSuper;
            setBinder(this);
        }

        public BindRef getRef(int line) {
            if (isSuper && line >= 0)
                throw new CompileException(line, 0,
                        "super cannot be used as a value");
            return this;
        }

        void gen(Ctx ctx) {
            ctx.load(argn);
            if (javaType.getType() == YetiType.JAVA_ARRAY) {
                ctx.forceType(JavaType.descriptionOf(javaType));
            } else if (javaType.getJavaType().getDescription().charAt(0) == 'L') {
                ctx.forceType(javaType.getJavaType().className());
            }
        }

        protected boolean flagop(int flag) {
            return (flag & DIRECT_THIS) != 0 && argn == 0;
        }
    }

    public static class Meth extends JavaType.Method implements Closure {
        private List args = new ArrayList();
        private AClosure closure; // just for closure init
        private int line;
        Capture captures;
        Code code;

        public Meth() {
            closure = new RootClosure();
        }

        Binder addArg(YType type) {
            Arg arg = new Arg(type, false);
            args.add(arg);
            arg.argn = (access & ACC_STATIC) == 0
                            ? args.size() : args.size() - 1;
            return arg;
        }

        public BindRef refProxy(BindRef code) {
            return code; // method don't capture - this is outer classes job
        }

        public void addVar(BindExpr binder) {
            closure.addVar(binder);
        }
        
        void init() {
            arguments = new YType[args.size()];
            for (int i = 0; i < arguments.length; ++i) {
                Arg arg = (Arg) args.get(i);
                arguments[i] = arg.javaType;
            }
            sig = name.concat(super.descr(null));
            descr = null;
        }

        String descr(String extra) {
            if (descr != null)
                return descr;
            StringBuffer additionalArgs = new StringBuffer();
            for (Capture c = captures; c != null; c = c.next)
                additionalArgs.append(c.captureType());
            return super.descr(additionalArgs.toString());
        }

        void convertArgs(Ctx ctx) {
            int n = (access & ACC_STATIC) == 0 ? 1 : 0;
            ctx.localVarCount = args.size() + n;
            for (int i = 0; i < arguments.length; ++i) {
                if (arguments[i].type != YetiType.JAVA)
                    continue;
                String descr = arguments[i].getJavaType().getDescription();
                if (descr != "Ljava/lang/String;" && descr.charAt(0) == 'L')
                    continue;
                loadArg(ctx, arguments[i], i + n);
                JavaExpr.convertValue(ctx, arguments[i]);
                ctx.varInsn(ASTORE, i + n);
            }
        }

        void gen(Ctx ctx) {
            ctx = ctx.newMethod(access, name, descr(null));
            if ((access & ACC_ABSTRACT) != 0) {
                ctx.closeMethod();
                return;
            }
            convertArgs(ctx);
            closure.genClosureInit(ctx);
            JavaExpr.convertedArg(ctx, code, returnType, line);
            if (returnType.getType() == YetiType.UNIT) {
                ctx.insn(POP);
                ctx.insn(RETURN);
            } else {
                genRet(ctx, returnType);
            }
            ctx.closeMethod();
        }
    }

    final class Field extends Code implements Binder, CaptureWrapper, CodeGen {
        private String name; // mangled name
        private String javaType;
        private String descr;
        Code value;
        private final boolean var;
        private int access = ACC_PRIVATE;
        private boolean directConst;

        Field(String name, Code value, boolean var) {
            this.name = name;
            this.value = value;
            this.var = var;
        }

        public void genPreGet(Ctx ctx) {
            if (!directConst)
                ctx.load(0);
        }

        public void genGet(Ctx ctx) {
            if (directConst)
                value.gen(ctx);
            else
                ctx.fieldInsn(GETFIELD, className, name, descr);
        }

        public void genSet(Ctx ctx, Code value) {
            value.gen(ctx);
            ctx.typeInsn(CHECKCAST, javaType);
            ctx.fieldInsn(PUTFIELD, className, name, descr);
        }

        public Object captureIdentity() {
            return JavaClass.this;
        }

        public String captureType() {
            return classType.getJavaType().getDescription();
        }

        public void gen2(Ctx ctx, Code value, int _) {
            genPreGet(ctx);
            genSet(ctx, value);
            ctx.insn(ACONST_NULL);
        }

        public BindRef getRef(int line) {
            if (javaType == null) {
                if (name == "_")
                    throw new IllegalStateException("NO _ REF");
                javaType = Code.javaType(value.getType());
                descr = 'L' + javaType + ';';
            }
            BindRef ref = new BindRef() {
                void gen(Ctx ctx) {
                    genPreGet(ctx);
                    genGet(ctx);
                }

                Code assign(final Code value) {
                    return var ?
                            new SimpleCode(Field.this, value, null, 0) : null;
                }

                boolean flagop(int fl) {
                    return (fl & ASSIGN) != 0 && var ||
                           (fl & (CONST | DIRECT_BIND)) != 0 && directConst ||
                           (fl & PURE) != 0 && !var;
                }

                CaptureWrapper capture() {
                    if (!var)
                        return null;
                    access = ACC_SYNTHETIC; // clear private
                    return Field.this;
                }
            };
            ref.setType(value.getType());
            ref.setBinder(this);
            return ref;
        }

        void gen(Ctx ctx) {
            if (this == serialVersion) {
                // hack to allow defining serialVersionUID
                Long v = new Long((((NumericConstant) value).num).longValue());
                ctx.cw.visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL,
                        name, "J", null, v);
                directConst = true;
            } else if (javaType == null) {
                // _ = or just unused binding
                value.gen(ctx);
                ctx.insn(POP);
            } else if (!var && value.prepareConst(ctx)) {
                directConst = true;
            } else {
                ctx.cw.visitField(var ? access : access | ACC_FINAL,
                                  name, descr, null, null).visitEnd();
                genPreGet(ctx);
                genSet(ctx, value);
            }
        }
    }

    JavaClass(String className, boolean isPublic) {
        setType(YetiType.UNIT_TYPE);
        this.className = className;
        classType = new YType(YetiType.JAVA, YetiType.NO_PARAM);
        classType.javaType = JavaType.createNewClass(className, this);
        self = new Arg(classType, false);
        constr.name = "<init>";
        constr.returnType = YetiType.UNIT_TYPE;
        constr.className = className;
        constr.access = isPublic ? ACC_PUBLIC : 0;
        this.isPublic = isPublic;
    }

    static void loadArg(Ctx ctx, YType argType, int n) {
        int ins = ALOAD;
        if (argType.getType() == YetiType.JAVA) {
            switch (argType.getJavaType().getDescription().charAt(0)) {
                case 'D': ins = DLOAD; break;
                case 'F': ins = FLOAD; break;
                case 'J': ins = LLOAD; break;
                case 'L': break;
                default : ins = ILOAD;
            }
        }
        ctx.varInsn(ins, n);
    }

    static void genRet(Ctx ctx, YType returnType) {
        int ins = ARETURN;
        if (returnType.type == YetiType.JAVA) {
            switch (returnType.getJavaType().getDescription().charAt(0)) {
                case 'D': ins = DRETURN; break;
                case 'F': ins = FRETURN; break;
                case 'J': ins = LRETURN; break;
                case 'L': break;
                case 'V': ins = RETURN; break;
                default : ins = IRETURN;
            }
        }
        ctx.insn(ins);
    }

    void init(YetiType.ClassBinding parentClass, String[] interfaces) {
        implement = interfaces;
        this.parentClass = parentClass;
        YType t = new YType(YetiType.JAVA, YetiType.NO_PARAM);
        t.javaType = parentClass.type.javaType.dup();
        t.javaType.implementation = this;
        t.javaType.publicMask = ACC_PUBLIC | ACC_PROTECTED;
        superRef = new Arg(t, true);
    }

    Meth addMethod(String name, YType returnType,
                   String mod, int line) {
        Meth m = new Meth();
        m.name = name;
        m.returnType = returnType;
        m.className = className;
        m.access = mod == "static-method" ? ACC_PUBLIC + ACC_STATIC
                 : mod == "abstract-method" ? ACC_PUBLIC + ACC_ABSTRACT
                 : ACC_PUBLIC;
        m.line = line;
        methods.add(m);
        return m;
    }

    Binder addField(Code value, boolean var, String name) {
        Field field;
        if (name == "serialVersionUID" && !var &&
                serialVersion == null && value instanceof NumericConstant) {
            serialVersion = field = new Field(name, value, false);
        } else {
            field = new Field("$" + fields.size(), value, var);
        }
        fields.add(field);
        return field;
    }

    public BindRef refProxy(BindRef code) {
        if (code.flagop(DIRECT_BIND))
            return code;
        if (!isPublic)
            return captureRef(code);
        code.forceDirect();
        return code;
    }

    void superInit(JavaType.Method init, Code[] args, int line) {
        superInit = new JavaExpr(null, init, args, line);
    }

    void close() throws JavaClassNotFoundException {
        constr.init();
        JavaTypeReader t = new JavaTypeReader();
        t.constructors.add(constr);
        for (int i = 0, cnt = methods.size(); i < cnt; ++i) {
            Meth m = (Meth) methods.get(i);
            m.init();
            ((m.access & ACC_STATIC) != 0 ? t.staticMethods : t.methods).add(m);
        }
        t.parent = parentClass.type.javaType;
        t.className = className;
        t.interfaces = implement;
        t.access = isPublic ? ACC_PUBLIC : 0;
        classType.javaType.publicMask = ACC_PUBLIC | ACC_PROTECTED;
        classType.javaType.resolve(t);
    }

    // must be called after close
    BindRef[] getCaptures() {
        captureCount = mergeCaptures(null, true);
        BindRef[] r = new BindRef[captureCount];
        int n = 0;
        for (Capture c = captures; c != null; c = c.next) {
            r[n++] = c.ref;
        }
        return r;
    }

    // called by mergeCaptures
    void captureInit(Ctx fun, Capture c, int n) {
        c.id = "_" + n;
        // for super arguments
        c.localVar = n + constr.args.size() + 1;
    }

    String getAccessor(JavaType.Method method, String descr,
                       boolean invokeSuper) {
        if (accessors == null)
            accessors = new HashMap();
        String sig = method.sig;
        if (invokeSuper)
            sig = "*".concat(method.sig);
        Object[] accessor = (Object[]) accessors.get(sig);
        if (accessor == null) {
            accessor = new Object[] { "access$" + accessors.size(), method,
                                      descr, invokeSuper ? "" : null };
            accessors.put(method.sig, accessor);
        }
        return (String) accessor[0];
    }

    String getAccessor(JavaType.Field field, String descr, boolean write) {
        String key = (write ? "{" : "}").concat(field.name);
        if (accessors == null)
            accessors = new HashMap();
        Object[] accessor = (Object[]) accessors.get(key);
        if (accessor == null) {
            accessor = new Object[] { "access$" + accessors.size(), field,
                                      descr, write ? "" : null, null };
            accessors.put(key, accessor);
        }
        return (String) accessor[0];
    }

    void gen(Ctx ctx) {
        int i, cnt;
        constr.captures = captures;
        ctx.insn(ACONST_NULL);
        Ctx clc = ctx.newClass(classType.javaType.access | ACC_SUPER,
                        className, parentClass.type.javaType.className(),
                        implement);
        clc.fieldCounter = captureCount;
        // block using our method names ;)
        for (i = 0, cnt = methods.size(); i < cnt; ++i)
            clc.usedMethodNames.put(((Meth) methods.get(i)).name, null);
        if (!isPublic)
            clc.markInnerClass(ctx.constants.ctx, ACC_STATIC);
        Ctx init = clc.newMethod(constr.access, "<init>", constr.descr(null));
        constr.convertArgs(init);
        genClosureInit(init);
        superInit.genCall(init.load(0), parentClass.getCaptures(),
                          INVOKESPECIAL);
        // extra arguments are used for smuggling in captured bindings
        int n = constr.arguments.length;
        for (Capture c = captures; c != null; c = c.next) {
            c.localVar = -1; // reset to using this
            clc.cw.visitField(0, c.id, c.captureType(), null, null).visitEnd();
            init.load(0).load(++n)
                .fieldInsn(PUTFIELD, className, c.id, c.captureType());
        }
        for (i = 0, cnt = fields.size(); i < cnt; ++i)
            ((Code) fields.get(i)).gen(init);
        init.insn(RETURN);
        init.closeMethod();
        for (i = 0, cnt = methods.size(); i < cnt; ++i)
            ((Meth) methods.get(i)).gen(clc);
        if (isPublic) {
            Ctx clinit = clc.newMethod(ACC_STATIC, "<clinit>", "()V");
            clinit.methodInsn(INVOKESTATIC, ctx.className,
                              "eval", "()Ljava/lang/Object;");
            clinit.insn(POP);
            clinit.insn(RETURN);
            clinit.closeMethod();
        }
        classCtx = clc;
        ctx.compilation.postGen.add(this);
    }

    // postGen hook. accessors can be added later than the class gen is called
    public void run() {
        if (accessors == null)
            return;
        for (Iterator i = accessors.values().iterator(); i.hasNext(); ) {
            Object[] accessor = (Object[]) i.next();
            int acc = ACC_STATIC;
            JavaType.Method m = null;
            if (accessor.length == 4) { // method
                m = (JavaType.Method) accessor[1];
                acc = m.access & ACC_STATIC;
            }
            Ctx mc = classCtx.newMethod(acc | ACC_SYNTHETIC,
                                       (String) accessor[0],
                                       (String) accessor[2]);
            if (m != null) { // method
                int start = 0;
                int insn = INVOKESTATIC;
                if ((acc & ACC_STATIC) == 0) {
                    insn = accessor[3] == null ? INVOKEVIRTUAL : INVOKESPECIAL;
                    start = 1;
                    mc.load(0);
                }
                for (int j = 0; j < m.arguments.length; ++j)
                    loadArg(mc, m.arguments[j], j + start);
                mc.methodInsn(insn, accessor[3] == null ? className :
                                    parentClass.type.javaType.className(),
                              m.name, m.descr(null));
                genRet(mc, m.returnType);
            } else { // field
                JavaType.Field f = (JavaType.Field) accessor[1];
                int insn = GETSTATIC, reg = 0;
                if ((f.access & ACC_STATIC) == 0) {
                    mc.load(reg++);
                    insn = GETFIELD;
                }
                if (accessor[3] != null) {
                    mc.load(reg);
                    insn = insn == GETFIELD ? PUTFIELD : PUTSTATIC;
                }
                mc.fieldInsn(insn, className, f.name,
                             JavaType.descriptionOf(f.type));
                if (accessor[3] != null) {
                    mc.insn(RETURN);
                } else {
                    genRet(mc, f.type);
                }
            }
            mc.closeMethod();
        }
    }
}
