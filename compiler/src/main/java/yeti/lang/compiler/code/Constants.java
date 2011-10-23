package yeti.lang.compiler.code;

import yeti.lang.compiler.struct.StructField;
import yeti.renamed.asm3.Opcodes;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class Constants implements Opcodes {
    final Map constants = new HashMap();
    private Ctx sb;
    private Map structClasses = new HashMap();
    int anonymousClassCounter;
    String sourceName;
    private Ctx ctx;

    public Ctx getCtx() {
        return ctx;
    }

    void setCtx(Ctx ctx) {
        this.ctx = ctx;
    }

    public Map getStructClasses() {
        return structClasses;
    }

    public void constField(int mode, String name, Code code, String descr) {
        ctx.getClassWriter().visitField(mode, name, descr, null, null).visitEnd();
        if (sb == null)
            sb = ctx.newMethod(ACC_STATIC, "<clinit>", "()V");
        code.gen(sb);
        sb.fieldInsn(PUTSTATIC, ctx.getClassName(), name, descr);
    }

    void registerConstant(Object key, Code code, Ctx ctx_) {
        String descr = 'L' + Code.javaType(code.getType().deref()) + ';';
        String name = (String) constants.get(key);
        if (name == null) {
            name = "_".concat(Integer.toString(ctx.getFieldCounter()));
            ctx.incrementFieldCounterBy(1);
            constField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                    name, code, descr);
            constants.put(key, name);
        }
        ctx_.fieldInsn(GETSTATIC, ctx.getClassName(), name, descr);
    }

    void close() {
        if (sb != null) {
            sb.insn(RETURN);
            sb.closeMethod();
        }
    }

    // first value in array must be empty
    public void stringArray(Ctx ctx_, String[] array) {
        array[0] = "Strings";
        List key = Arrays.asList(array);
        String name = (String) constants.get(key);
        if (name == null) {
            name = "_".concat(Integer.toString(ctx.getFieldCounter()));
            ctx.incrementFieldCounterBy(1);
            ctx.getClassWriter().visitField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, name,
                    "[Ljava/lang/String;", null, null).visitEnd();
            sb.intConst(array.length - 1);
            sb.typeInsn(ANEWARRAY, "java/lang/String");
            for (int i = 1; i < array.length; ++i) {
                sb.insn(DUP);
                sb.intConst(i - 1);
                sb.ldcInsn(array[i]);
                sb.insn(AASTORE);
            }
            sb.fieldInsn(PUTSTATIC, ctx.getClassName(), name,
                             "[Ljava/lang/String;");
            constants.put(key, name);
        }
        ctx_.fieldInsn(GETSTATIC, ctx.getClassName(), name,
                            "[Ljava/lang/String;");
    }

    // generates [Ljava/lang/String;[Z into stack, using constant cache
    public void structInitArg(Ctx ctx_, StructField[] fields,
                              int fieldCount, boolean nomutable) {
        if (sb == null)
            sb = ctx.newMethod(ACC_STATIC, "<clinit>", "()V");
        String[] fieldNameArr = new String[fieldCount + 1];
        char[] mutableArr = new char[fieldNameArr.length];
        mutableArr[0] = '@';
        int i, mutableCount = 0;
        for (i = 1; i < fieldNameArr.length; ++i) {
            StructField f = fields[i - 1];
            fieldNameArr[i] = f.getName();
            if (f.isMutable() || f.getProperty() > 0) {
                mutableArr[i] = '\001';
                ++mutableCount;
            }
        }
        stringArray(ctx_, fieldNameArr);
        if (nomutable || mutableCount == 0) {
            ctx_.insn(ACONST_NULL);
            return;
        }
        String key = new String(mutableArr);
        String name = (String) constants.get(key);
        if (name == null) {
            name = "_".concat(Integer.toString(ctx.getFieldCounter()));
            ctx.incrementFieldCounterBy(1);
            ctx.getClassWriter().visitField(ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC,
                    name, "[Z", null, null).visitEnd();
            sb.intConst(fieldCount);
            sb.visitIntInsn(NEWARRAY, T_BOOLEAN);
            for (i = 0; i < fieldCount; ++i) {
                sb.insn(DUP);
                sb.intConst(i);
                sb.intConst(mutableArr[i + 1]);
                sb.insn(BASTORE);
            }
            sb.fieldInsn(PUTSTATIC, ctx.getClassName(), name, "[Z");
            constants.put(key, name);
        }
        ctx_.fieldInsn(GETSTATIC, ctx.getClassName(), name, "[Z");
    }
}
