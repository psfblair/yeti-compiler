package yeti.lang.compiler.code;

import yeti.renamed.asm3.ClassWriter;
import yeti.renamed.asm3.Label;
import yeti.renamed.asm3.MethodVisitor;
import yeti.renamed.asm3.Opcodes;

import java.util.HashMap;
import java.util.Map;

public final class Ctx implements Opcodes {
    private CompileCtx compilation;
    private String className;
    private ClassWriter cw;
    private MethodVisitor m;
    private int lastInsn = -1;
    private String lastType;
    private Constants constants;
    private Map usedMethodNames;
    private int localVarCount;
    private int fieldCounter;
    int lastLine;
    private int tainted; // you are inside loop, natural laws a broken

    public int getLocalVarCount() {
        return localVarCount;
    }

    public void setLocalVarCount(int localVarCount) {
        this.localVarCount = localVarCount;
    }

    public int incrementLocalVarCountBy(int increment) {
        localVarCount += increment;
        return localVarCount;
    }

    public int getFieldCounter() {
        return fieldCounter;
    }

    public int incrementFieldCounterBy(int increment) {
        fieldCounter += increment;
        return fieldCounter;
    }

    public void setFieldCounter(int fieldCounter) {
        this.fieldCounter = fieldCounter;
    }

    public String getClassName() {
        return className;
    }

    public ClassWriter getClassWriter() {
        return cw;
    }

    public CompileCtx getCompilation() {
        return compilation;
    }

    public Constants getConstants() {
        return constants;
    }

    public Map getUsedMethodNames() {
        return usedMethodNames;
    }

    public int getTainted() {
        return tainted;
    }

    public void incrementTainted() {
        ++tainted;
    }

    public void decrementTainted() {
        --tainted;
    }

    public void captureCast(String type) {
        if (type.charAt(0) == 'L')
            type = type.substring(1, type.length() - 1);
        if (!type.equals("java/lang/Object"))
            typeInsn(CHECKCAST, type);
    }

    public Ctx load(int var) {
        insn(-1);
        m.visitVarInsn(ALOAD, var);
        return this;
    }

    public void forceType(String type) {
        insn(-2);
        lastType = type;
    }

    public void intConst(int n) {
        if (n >= -1 && n <= 5) {
            insn(n + 3);
        } else {
            insn(-1);
            if (n >= -32768 && n <= 32767) {
                m.visitIntInsn(n >= -128 && n <= 127 ? BIPUSH : SIPUSH, n);
            } else {
                m.visitLdcInsn(new Integer(n));
            }
        }
    }

    public void insn(int opcode) {
        if (lastInsn != -1 && lastInsn != -2) {
            if (lastInsn == ACONST_NULL && opcode == POP) {
                lastInsn = -1;
                return;
            }
            m.visitInsn(lastInsn);
        }
        lastInsn = opcode;
    }

    public void fieldInsn(int opcode, String owner,
                              String name, String desc) {
        if (owner == null || name == null || desc == null)
            throw new IllegalArgumentException("fieldInsn(" + opcode +
                        ", " + owner + ", " + name + ", " + desc + ")");
        insn(-1);
        m.visitFieldInsn(opcode, owner, name, desc);
        if ((opcode == GETSTATIC || opcode == GETFIELD) &&
            desc.charAt(0) == 'L') {
            lastInsn = -2;
            lastType = desc.substring(1, desc.length() - 1);
        }
    }

    public void jumpInsn(int opcode, Label label) {
        insn(-1);
        m.visitJumpInsn(opcode, label);
    }

    public void methodInsn(int opcode, String owner,
                           String name, String desc) {
        insn(-1);
        m.visitMethodInsn(opcode, owner, name, desc);
    }

    public void popn(int n) {
        if ((n & 1) != 0) {
            insn(POP);
        }
        for (; n >= 2; n -= 2) {
            insn(POP2);
        }
    }

    public void typeInsn(int opcode, String type) {
        if (opcode == CHECKCAST &&
            (lastInsn == -2 && type.equals(lastType) ||
             lastInsn == ACONST_NULL)) {
            return; // no cast necessary
        }
        insn(-1);
        m.visitTypeInsn(opcode, type);
    }

    public void visitLabel(Label label) {
        if (lastInsn != -2)
            insn(-1);
        m.visitLabel(label);
    }

    public void visitLine(int line) {
        if (line != 0 && lastLine != line) {
            Label label = new Label();
            m.visitLabel(label);
            m.visitLineNumber(line, label);
            lastLine = line;
        }
    }

    public void varInsn(int opcode, int var) {
        insn(-1);
        m.visitVarInsn(opcode, var);
    }

    public void visitApply(Code arg, int line) {
        arg.gen(this);
        insn(-1);
        visitLine(line);
        m.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun",
                "apply", "(Ljava/lang/Object;)Ljava/lang/Object;");
    }

    Ctx(CompileCtx compilation, Constants constants,
            ClassWriter writer, String className) {
        this.compilation = compilation;
        this.constants = constants;
        this.cw = writer;
        this.className = className;
    }

    public Ctx newClass(int flags, String name, String extend, String[] interfaces) {
        Ctx ctx = new Ctx(compilation, constants,
                          new YClassWriter(compilation.classWriterFlags), name);
        ctx.usedMethodNames = new HashMap();
        ctx.cw.visit(V1_4, flags, name, null,
                extend == null ? "java/lang/Object" : extend, interfaces);
        ctx.cw.visitSource(constants.sourceName, null);
        compilation.addClass(name, ctx);
        return ctx;
    }

    public Ctx newMethod(int flags, String name, String type) {
        Ctx ctx = new Ctx(compilation, constants, cw, className);
        ctx.usedMethodNames = usedMethodNames;
        ctx.m = cw.visitMethod(flags, name, type, null, null);
        ctx.m.visitCode();
        return ctx;
    }

    public void markInnerClass(Ctx outer, int access) {
        String fn = className.substring(outer.className.length() + 1);
        outer.cw.visitInnerClass(className, outer.className, fn, access);
        cw.visitInnerClass(className, outer.className, fn, access);
    }

    public void closeMethod() {
        insn(-1);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    public void createInit(int mod, String parent) {
        MethodVisitor m = cw.visitMethod(mod, "<init>", "()V", null, null);
        // super()
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKESPECIAL, parent, "<init>", "()V");
        m.visitInsn(RETURN);
        m.visitMaxs(0, 0);
        m.visitEnd();
    }

    void genInt(Code arg, int line) {
        if (arg instanceof NumericConstant) {
            intConst(((NumericConstant) arg).getNum().intValue());
        } else {
            arg.gen(this);
            visitLine(line);
            typeInsn(CHECKCAST, "yeti/lang/Num");
            methodInsn(INVOKEVIRTUAL, "yeti/lang/Num", "intValue", "()I");
        }
    }

    void genBoolean(Label label) {
        fieldInsn(GETSTATIC, "java/lang/Boolean",
                "TRUE", "Ljava/lang/Boolean;");
        Label end = new Label();
        m.visitJumpInsn(GOTO, end);
        m.visitLabel(label);
        m.visitFieldInsn(GETSTATIC, "java/lang/Boolean",
                "FALSE", "Ljava/lang/Boolean;");
        m.visitLabel(end);
    }

    public void visitIntInsn(int opcode, int param) {
        insn(-1);
        if (opcode != IINC)
            m.visitIntInsn(opcode, param);
        else
            m.visitIincInsn(param, -1);
    }

    public void visitInit(String type, String descr) {
        insn(-2);
        m.visitMethodInsn(INVOKESPECIAL, type, "<init>", descr);
        lastType = type;
    }

    public void ldcInsn(Object cst) {
        insn(-1);
        m.visitLdcInsn(cst);
        if (cst instanceof String) {
            lastInsn = -2;
            lastType = "java/lang/String";
        }
    }
    
    public void tryCatchBlock(Label start, Label end, Label handler, String type) {
        insn(-1);
        m.visitTryCatchBlock(start, end, handler, type);
    }

    public void switchInsn(int min, int max, Label dflt,
                           int[] keys, Label[] labels) {
        insn(-1);
        if (keys == null) {
            m.visitTableSwitchInsn(min, max, dflt, labels);
        } else {
            m.visitLookupSwitchInsn(dflt, keys, labels);
        }
    }

    void constant(Object key, Code code) {
        constants.registerConstant(key, code, this);
    }
}