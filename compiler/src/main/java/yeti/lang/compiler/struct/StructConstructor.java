// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler java bytecode generator - structures.
 *
 * Copyright (c) 2010 Madis Janson
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

package yeti.lang.compiler.struct;

import yeti.lang.compiler.closure.*;
import yeti.lang.compiler.code.*;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Label;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/*
 * Being a closure allows inlining property getters/setters.
 */
public final class StructConstructor extends CapturingClosure implements Comparator {
    StructField[] fields;
    int fieldCount;
    StructField properties;
    String impl;
    int arrayVar = -1;
    private boolean mustGen;
    private Code withParent;
    private String[] withFields;

    private class Bind extends BindRef implements Binder, CaptureWrapper {
        private StructField field;
        private boolean fun;
        private boolean direct;
        private boolean mutable;
        private int var;

        Bind(StructField sf) {
            setType(sf.value.getType());
            setBinder(this);
            mutable = sf.isMutable();
            fun = sf.value instanceof Function;
            field = sf;
        }

        void initGen(Ctx ctx) {
            if (prepareConst(ctx)) {
                direct = true;
                field.binder = null;
                return;
            }
            Function f;
            if (!mutable && fun) {
                ((Function) field.value).prepareGen(ctx);
                ctx.varInsn(ASTORE, var = ctx.getLocalVarCount());
                ctx.incrementLocalVarCountBy(1);
            } else {
                if (arrayVar == -1) {
                    arrayVar = ctx.getLocalVarCount();
                    ctx.incrementLocalVarCountBy(1);
                }
                var = arrayVar;
                field.binder = null;
            }
        }

        public BindRef getRef(int line) {
            field.binder = this;
            return this;
        }

        public CaptureWrapper capture() {
            return !fun || mutable ? this : null;
        }

        public boolean flagop(int fl) {
            if ((fl & ASSIGN) != 0)
                return mutable;
            if ((fl & PURE) != 0)
                return !mutable;
            if ((fl & DIRECT_BIND) != 0)
                return direct;
            if ((fl & CONST) != 0)
                return direct || !mutable && field.value.flagop(CONST);
            return false;
        }

        public boolean prepareConst(Ctx ctx) {
            return direct || !mutable && field.value.prepareConst(ctx);
        }

        public void gen(Ctx ctx) {
            if (direct)
                field.value.gen(ctx);
            else
                ctx.load(var);
        }

        public void genPreGet(Ctx ctx) {
            if (direct)
                ctx.insn(ACONST_NULL); // wtf
            else
                ctx.load(var);
        }

        public void genGet(Ctx ctx) {
            if (direct) {
                ctx.insn(POP);
                field.value.gen(ctx);
            } else if (impl == null) {
                // GenericStruct
                ctx.ldcInsn(field.getName());
                ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct", "get",
                               "(Ljava/lang/String;)Ljava/lang/Object;");
            } else if (field.getProperty() != 0) {
                // Property accessor
                ctx.intConst(field.index);
                ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                               "get", "(I)Ljava/lang/Object;");
            } else {
                ctx.fieldInsn(GETFIELD, impl, field.javaName,
                              "Ljava/lang/Object;");
            }
        }

        public void genSet(Ctx ctx, Code value) {
            if (impl != null && field.getProperty() == 0) {
                value.gen(ctx);
                ctx.fieldInsn(PUTFIELD, impl, field.javaName,
                              "Ljava/lang/Object;");
                return;
            }
            ctx.ldcInsn(field.getName());
            value.gen(ctx);
            ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct", "set",
                           "(Ljava/lang/String;Ljava/lang/Object;)V");
        }

        public Object captureIdentity() {
            return direct ? null : StructConstructor.this;
        }

        public String captureType() {
            return impl != null ? 'L' + impl + ';' : "Lyeti/lang/Struct;";
        }
    }

    StructConstructor(int maxBinds) {
        fields = new StructField[maxBinds];
    }

    public void publish() {
        for (int i = 0; i < fieldCount; ++i) {
            if (fields[i].getProperty() <= 0) {
                Code v = fields[i].value;
                while (v instanceof BindRef)
                    v = ((BindRef) v).unref(true);
                if (v instanceof Function)
                    ((Function) v).setPublish(true);
            }
        }
    }

    // for some reason, binds are only for non-property fields
    Binder bind(StructField sf) {
        return new Bind(sf);
    }

    void add(StructField field) {
        if (field.getName() == null)
            throw new IllegalArgumentException();
        fields[fieldCount++] = field;
        if (field.getProperty() != 0) {
            field.nextProperty = properties;
            properties = field;
        }
    }

    public int compare(Object a, Object b) {
        return ((StructField) a).getName().compareTo(((StructField) b).getName());
    }

    void close() {
        // warning - close can be called second time by `with'
        Arrays.sort(fields, 0, fieldCount, this);
        for (int i = 0; i < fieldCount; ++i) {
            StructField field = fields[i];
            field.javaName = "_".concat(Integer.toString(i));
            field.index = i;
            if (field.getProperty() != 0)
                mustGen = true;
        }
    }

    public Map getDirect(Constants constants) {
        Map r = new HashMap(fieldCount);
        for (int i = 0; i < fieldCount; ++i) {
            if (fields[i].isMutable() || fields[i].getProperty() > 0) {
                r.put(fields[i].getName(), null);
                continue;
            }
            if (fields[i].binder != null)
                continue;
            Code v = fields[i].value;
            while (v instanceof BindRef)
                v = ((BindRef) v).unref(false);
            if (v != null && v.flagop(CONST)) {
                if (v instanceof Function) {
                    r.put(fields[i].getName(), ((Function) v).getName());
                } else {
                    String descr = 'L' + Code.javaType(v.getType()) + ';';
                    constants.constField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                                         mangle(fields[i].getName()), v, descr);
                    r.put(fields[i].getName(), ".");
                }
            }
        }
        return r;
    }

/*    public BindRef refProxy(BindRef code) {
        return code;
//        return code.flagop(DIRECT_BIND) ? code : captureRef(code);
    }*/

    void captureInit(Ctx st, Capture c, int n) {
        // c.getId() initialises the captures id as a side effect
        st.getClassWriter().visitField(ACC_SYNTHETIC, c.getId(st), c.captureType(),
                null, null).visitEnd();
    }

    public void gen(Ctx ctx) {
        boolean generated = false;
        // default: null - GenericStruct
        if (mustGen || fieldCount > 6 && fieldCount <= 15) {
            impl = genStruct(ctx);
            generated = true;
        } else if (fieldCount <= 3) {
            impl = "yeti/lang/Struct3";
        } else if (fieldCount <= 6) {
            impl = "yeti/lang/Struct6";
        }
        for (int i = 0; i < fieldCount; ++i)
            if (fields[i].binder != null)
                ((Bind) fields[i].binder).initGen(ctx);
        String implClass = impl != null ? impl : "yeti/lang/GenericStruct";
        ctx.typeInsn(NEW, implClass);
        ctx.insn(DUP);
        if (withParent != null) {
            withParent.gen(ctx);
            ctx.visitInit(implClass, "(Lyeti/lang/Struct;)V");
        } else if (generated) {
            ctx.visitInit(implClass, "()V");
        } else {
            ctx.getConstants().structInitArg(ctx, fields, fieldCount, false);
            ctx.visitInit(implClass, "([Ljava/lang/String;[Z)V");
        }
        if (arrayVar != -1)
            ctx.varInsn(ASTORE, arrayVar);
        for (int i = 0, cnt = fieldCount; i < cnt; ++i) {
            if (fields[i].getProperty() != 0 || fields[i].inherited)
                continue;
            if (arrayVar != -1) {
                ctx.load(arrayVar);
            } else {
                ctx.insn(DUP);
            }
            if (impl == null)
                ctx.ldcInsn(fields[i].getName());
            if (fields[i].binder != null) {
                fields[i].binder.gen(ctx);
                ((Function) fields[i].value).finishGen(ctx);
            } else {
                fields[i].value.gen(ctx);
            }
            if (impl != null) {
                ctx.fieldInsn(PUTFIELD, impl, fields[i].javaName,
                                   "Ljava/lang/Object;");
            } else {
                ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/GenericStruct",
                        "set", "(Ljava/lang/String;Ljava/lang/Object;)V");
            }
        }
        if (arrayVar != -1)
            ctx.load(arrayVar);
        for (Capture c = getCaptures(); c != null; c = c.getNext()) {
            ctx.insn(DUP);
            c.captureGen(ctx);
            ctx.fieldInsn(PUTFIELD, impl, c.getId(), c.captureType());
        }
    }

    String genStruct(Ctx ctx) {
        String cn, structKey = null;
        StructField field;
        Label next, dflt = null, jumps[];
        int i;

        if (mustGen) {
            /*
             * Have to generate our own struct class anyway, so take advantage
             * and change constant fields into inlined properties, eliminating
             * actual field stores for these fields in the structure.
             */
            for (i = 0; i < fieldCount; ++i) {
                field = fields[i];
                if (!field.isMutable() && field.getProperty() == 0 &&
                    !field.inherited && field.value.prepareConst(ctx))
                    field.setProperty(-1);
            }
        } else {
            StringBuffer buf = new StringBuffer();
            for (i = 0; i < fields.length; ++i) {
                buf.append(fields[i].isMutable() ? ';' : ',')
                   .append(fields[i].getName());
            }
            structKey = buf.toString();
            cn = (String) ctx.getConstants().getStructClasses().get(structKey);
            if (cn != null)
                return cn;
        }

        cn = ctx.getCompilation().createClassName(ctx, ctx.getClassName(), "");
        if (structKey != null) {
            ctx.getConstants().getStructClasses().put(structKey, cn);
        }
        Ctx st = ctx.newClass(ACC_SUPER | ACC_FINAL, cn,
                              "yeti/lang/AStruct", null);
        st.setFieldCounter(fieldCount);
        mergeCaptures(st, true);
        Ctx m = st.newMethod(ACC_PUBLIC, "<init>",
                    withParent == null ? "()V" : "(Lyeti/lang/Struct;)V");
        m.load(0).getConstants()
                 .structInitArg(m, fields, fieldCount, withParent != null);
        m.visitInit("yeti/lang/AStruct", "([Ljava/lang/String;[Z)V");
        if (withParent != null) {
            // generates code for joining super fields
            m.intConst(1);
            m.visitIntInsn(NEWARRAY, T_INT);
            m.varInsn(ASTORE, 4); // index - int[]
            m.intConst(withFields.length - 2);
            m.varInsn(ISTORE, 3); // j = NAMES.length - 1
            // ext (extended struct)
            m.load(1).methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                                 "count", "()I");
            m.varInsn(ISTORE, 2); // i - field counter
            Label retry = new Label(), cont = new Label(), exit = new Label();
            next = new Label();
            m.visitLabel(retry);
            m.visitIntInsn(IINC, 2); // --i

            // if (ext.name(i) != NAMES[j]) goto next;
            m.load(1).varInsn(ILOAD, 2);
            m.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                         "name", "(I)Ljava/lang/String;");
            m.getConstants().stringArray(m, withFields);
            m.varInsn(ILOAD, 3);
            m.insn(AALOAD); // NAMES[j]
            m.jumpInsn(IF_ACMPNE, cont);

            // this ext.ref(i, index, 0)
            m.load(0).load(1).varInsn(ILOAD, 2);
            m.load(4).intConst(0);
            m.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                         "ref", "(I[II)Ljava/lang/Object;");

            jumps = new Label[withFields.length - 1];
            for (i = 0; i < jumps.length; ++i)
                jumps[i] = new Label();
            m.load(0).load(4).intConst(0);
            m.insn(IALOAD); // index[0]
            if (jumps.length > 1) {
                dflt = new Label();
                m.varInsn(ILOAD, 3); // switch (j)
                m.switchInsn(0, jumps.length - 1, dflt, null, jumps);
            }
            i = 0;
            for (int j = 0; j < jumps.length; ++i)
                if (fields[i].inherited) {
                    m.visitLabel(jumps[j++]);
                    m.fieldInsn(PUTFIELD, cn, "i" + i, "I");
                    m.fieldInsn(PUTFIELD, cn, fields[i].javaName,
                                "Ljava/lang/Object;");
                    m.jumpInsn(GOTO, next);
                }
            if (jumps.length > 1) {
                m.visitLabel(dflt);
                m.popn(4); // this ref this index
            }
            m.visitLabel(next);
            m.visitIntInsn(IINC, 3); // --j
            m.varInsn(ILOAD, 3);
            m.jumpInsn(IFLT, exit);

            m.visitLabel(cont);
            m.varInsn(ILOAD, 2);
            m.jumpInsn(IFGT, retry);
            m.visitLabel(exit);
        }
        m.insn(RETURN);
        m.closeMethod();

        // fields
        for (i = 0; i < fieldCount; ++i) {
            field = fields[i];
            if (field.getProperty() == 0)
                st.getClassWriter().visitField(field.inherited ? ACC_PRIVATE : ACC_SYNTHETIC,
                                 field.javaName, "Ljava/lang/Object;",
                                 null, null).visitEnd();
            if (field.inherited)
                st.getClassWriter().visitField(ACC_PRIVATE, "i" + i, "I", null, null)
                     .visitEnd();
        }

        // get(String)
        m = st.newMethod(ACC_PUBLIC, "get",
                         "(Ljava/lang/String;)Ljava/lang/Object;");
        m.load(0);
        Label withMutable = null;
        for (i = 0; i < fieldCount; ++i) {
            next = new Label();
            m.load(1).ldcInsn(fields[i].getName());
            m.jumpInsn(IF_ACMPNE, next);
            if (fields[i].getProperty() != 0) {
                m.intConst(i);
                m.methodInsn(INVOKEVIRTUAL, cn, "get", "(I)Ljava/lang/Object;");
            } else {
                m.fieldInsn(GETFIELD, cn, fields[i].javaName,
                            "Ljava/lang/Object;");
            }
            if (fields[i].inherited) {
                if (withMutable == null)
                    withMutable = new Label();
                m.load(0).fieldInsn(GETFIELD, cn, "i" + i, "I");
                m.insn(DUP);
                m.jumpInsn(IFGE, withMutable);
                m.insn(POP);
            }
            m.insn(ARETURN);
            m.visitLabel(next);
        }
        m.typeInsn(NEW, "java/lang/NoSuchFieldException");
        m.insn(DUP);
        m.load(1).visitInit("java/lang/NoSuchFieldException",
                            "(Ljava/lang/String;)V");
        m.insn(ATHROW);
        if (withMutable != null) {
            m.visitLabel(withMutable);
            m.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                         "get", "(I)Ljava/lang/Object;");
            m.insn(ARETURN);
        }
        m.closeMethod();

        // get(int)
        m = st.newMethod(ACC_PUBLIC, "get", "(I)Ljava/lang/Object;");
        m.setLocalVarCount(2);
        m.load(0).varInsn(ILOAD, 1);
        jumps = new Label[fieldCount];
        int mutableCount = 0;
        for (i = 0; i < fieldCount; ++i) {
            jumps[i] = new Label();
            if (fields[i].isMutable())
                ++mutableCount;
        }
        dflt = new Label();
        m.switchInsn(0, fieldCount - 1, dflt, null, jumps);
        if (withMutable != null)
            withMutable = new Label();
        for (i = 0; i < fieldCount; ++i) {
            field = fields[i];
            m.visitLabel(jumps[i]);
            if (field.getProperty() > 0) {
                new Apply(null, field.value,
                          new UnitConstant(null), field.line).gen(m);
            } else if (field.getProperty() < 0) {
                field.value.gen(m);
            } else {
                m.fieldInsn(GETFIELD, cn, field.javaName,
                            "Ljava/lang/Object;");
            }
            if (field.inherited) {
                m.load(0).fieldInsn(GETFIELD, cn, "i" + i, "I");
                m.insn(DUP);
                m.jumpInsn(IFGE, withMutable);
                m.insn(POP);
            }
            m.insn(ARETURN);
        }
        m.visitLabel(dflt);
        m.insn(ACONST_NULL);
        m.insn(ARETURN);
        if (withMutable != null) {
            m.visitLabel(withMutable);
            m.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                         "get", "(I)Ljava/lang/Object;");
            m.insn(ARETURN);
        }
        m.closeMethod();

        if (withParent != null) {
            m = st.newMethod(ACC_PUBLIC, "ref",
                             "(I[II)Ljava/lang/Object;");
            Label isConst = null;
            Label isVar = null;
            jumps = new Label[fieldCount];
            for (i = 0; i < fieldCount; ++i) {
                if (fields[i].inherited) {
                    jumps[i] = new Label();
                } else if (fields[i].isMutable() || fields[i].getProperty() > 0) {
                    if (isVar == null)
                        isVar = new Label();
                    jumps[i] = isVar;
                } else {
                    if (isConst == null)
                        isConst = new Label();
                    jumps[i] = isConst;
                }
            }
            dflt = new Label();
            m.load(2).varInsn(ILOAD, 3);
            m.varInsn(ILOAD, 1);
            m.switchInsn(0, fieldCount - 1, dflt, null, jumps);
            if (isConst != null) {
                m.visitLabel(isConst);
                m.intConst(-1);
                m.insn(IASTORE);
                m.load(0).varInsn(ILOAD, 1);
                m.methodInsn(INVOKEVIRTUAL, cn, "get",
                             "(I)Ljava/lang/Object;");
                m.insn(ARETURN);
            }
            if (isVar != null) {
                m.visitLabel(isVar);
                m.varInsn(ILOAD, 1);
                m.insn(IASTORE);
                m.load(0).insn(ARETURN);
            }
            for (i = 0; i < fieldCount; ++i) {
                if (!fields[i].inherited)
                    continue;
                m.visitLabel(jumps[i]);
                m.load(0).fieldInsn(GETFIELD, cn, "i" + i, "I");
                m.insn(IASTORE);
                m.load(0).fieldInsn(GETFIELD, cn, fields[i].javaName,
                                    "Ljava/lang/Object;");
                m.insn(ARETURN);
            }
            m.visitLabel(dflt);
            m.insn(POP2);
            m.insn(ACONST_NULL);
            m.insn(ARETURN);
            m.closeMethod();
        }

        if (mutableCount == 0)
            return cn;
        // set(String, Object)
        m = st.newMethod(ACC_PUBLIC, "set",
                         "(Ljava/lang/String;Ljava/lang/Object;)V");
        m.setLocalVarCount(3);
        m.load(0);
        for (i = 0; i < fieldCount; ++i) {
            field = fields[i];
            if (!field.isMutable())
                continue;
            next = new Label();
            m.load(1).ldcInsn(field.getName());
            m.jumpInsn(IF_ACMPNE, next);
            if (field.getProperty() != 0) {
                LoadVar var = new LoadVar();
                var.setVar(2);
                new Apply(null, field.setter, var, field.line).gen(m);
                m.insn(POP2);
            } else if (field.inherited) {
                m.fieldInsn(GETFIELD, cn, field.javaName,
                                 "Ljava/lang/Object;");
                m.load(1).load(2)
                 .methodInsn(INVOKEINTERFACE,  "yeti/lang/Struct", "set",
                             "(Ljava/lang/String;Ljava/lang/Object;)V");
            } else {
                m.load(2).fieldInsn(PUTFIELD, cn, field.javaName,
                                    "Ljava/lang/Object;");
            }
            m.insn(RETURN);
            m.visitLabel(next);
        }
        m.insn(POP);
        m.insn(RETURN);
        m.closeMethod();
        return cn;
    }

    void genWith(Ctx ctx, Code src, Map srcFields) {
        srcFields = new HashMap(srcFields);
        for (int i = 0; i < fieldCount; ++i)
            srcFields.remove(fields[i].getName());
        if (srcFields.isEmpty()) { // everything has been overrided
            gen(ctx);
            return;
        }
        mustGen = true;
        withParent = src;
        StructField[] fields = new StructField[fieldCount + srcFields.size()];
        Object[] withFields = srcFields.keySet().toArray();
        Arrays.sort(withFields);
        for (int i = 0; i < withFields.length; ++i) {
            StructField sf = new StructField();
            sf.setName((String) withFields[i]);
            sf.inherited = true;
            // whether to generate the setter code
            sf.setMutable(((YType) srcFields.get(sf.getName())).getField()
                    == YetiType.FIELD_MUTABLE);
            fields[i] = sf;
        }
        this.withFields = new String[withFields.length + 1];
        System.arraycopy(withFields, 0, this.withFields, 1, withFields.length);
        System.arraycopy(this.fields, 0, fields, srcFields.size(), fieldCount);
        this.fields = fields;
        fieldCount = fields.length;
        close();
        gen(ctx);
    }
}

