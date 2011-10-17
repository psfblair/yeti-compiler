package yeti.lang.compiler.java.code;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.CodeGen;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.code.SimpleCode;
import yeti.lang.compiler.java.JavaExpr;
import yeti.lang.compiler.java.JavaType;
import yeti.lang.compiler.yeti.type.YetiType;

final class ClassField extends JavaExpr implements CodeGen {
    private JavaType.Field field;

    ClassField(Code object, JavaType.Field field, int line) {
        super(object, null, null, line);
        this.type = field.convertedType();
        this.field = field;
    }

    void gen(Ctx ctx) {
        JavaType classType = field.classType.javaType;
        if (object != null) {
            object.gen(ctx);
            classType = object.type.deref().javaType;
        }
        ctx.visitLine(line);
        String descr = JavaType.descriptionOf(field.type);
        String className = classType.className();
        if (object != null)
            ctx.typeInsn(CHECKCAST, className);
        // XXX: not checking for package access. shouldn't matter.
        if ((field.access & ACC_PROTECTED) != 0
                && classType.implementation != null
                && !object.flagop(DIRECT_THIS)) {
            descr = (object == null ? "()" : '(' + classType.description + ')')
                    + descr;
            String name = classType.implementation
                                   .getAccessor(field, descr, false);
            ctx.methodInsn(INVOKESTATIC, className, name, descr);
        } else {
            ctx.fieldInsn(object == null ? GETSTATIC : GETFIELD,
                                 className, field.name, descr);
        }
        convertValue(ctx, field.type);
    }

    public void gen2(Ctx ctx, Code setValue, int _) {
        JavaType classType = field.classType.javaType;
        String className = classType.className();
        if (object != null) {
            object.gen(ctx);
            ctx.typeInsn(CHECKCAST, className);
            classType = object.type.deref().javaType;
        }
        genValue(ctx, setValue, field.type, line);
        String descr = JavaType.descriptionOf(field.type);
        if (descr.length() > 1) {
            ctx.typeInsn(CHECKCAST,
                field.type.type == YetiType.JAVA
                    ? field.type.javaType.className() : descr);
        }
        
        if ((field.access & ACC_PROTECTED) != 0
                && classType.implementation != null
                && !object.flagop(DIRECT_THIS)) {
            descr = (object != null ? "(".concat(classType.description)
                                    : "(") + descr + ")V";
            String name = classType.implementation
                                   .getAccessor(field, descr, true);
            ctx.methodInsn(INVOKESTATIC, className, name, descr);
        } else {
            ctx.fieldInsn(object == null ? PUTSTATIC : PUTFIELD,
                                 className, field.name, descr);
        }
        ctx.insn(ACONST_NULL);
    }

    Code assign(final Code setValue) {
        if ((field.access & ACC_FINAL) != 0)
            return null;
        return new SimpleCode(this, setValue, null, 0);
    }
}

