package yeti.lang.compiler.java.code;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.java.JavaExpr;
import yeti.lang.compiler.java.JavaType;
import yeti.lang.compiler.yeti.type.YetiType;

final class MethodCall extends JavaExpr {
    private JavaType classType;
    private boolean useAccessor, invokeSuper;

    MethodCall(Code object, JavaType.Method method, Code[] args, int line) {
        super(object, method, args, line);
        type = method.convertedReturnType();
    }

    void visitInvoke(Ctx ctx, int invokeInsn) {
        String descr = method.descr(null);
        String name = method.name;
        if (useAccessor) {
            if (invokeInsn == INVOKEINTERFACE)
                invokeInsn = INVOKEVIRTUAL;
            name = classType.implementation.getAccessor(method, descr,
                                                        invokeSuper);
        }
        ctx.methodInsn(invokeInsn, classType.className(), name, descr);
    }

    private void _gen(Ctx ctx) {
        classType = method.classType.javaType;
        int ins = object == null ? INVOKESTATIC : classType.isInterface()
                                 ? INVOKEINTERFACE : INVOKEVIRTUAL;
        if (object != null) {
            object.gen(ctx);
            if (ins != INVOKEINTERFACE)
                classType = object.type.deref().javaType;
        }
        if (classType.implementation != null) {
            JavaType ct = classType.implementation.classType.deref().javaType;
            invokeSuper = classType != ct;
            // XXX: not checking for package access. shouldn't matter.
            useAccessor = (invokeSuper || (method.access & ACC_PROTECTED) != 0)
                && !object.flagop(DIRECT_THIS);
            if (useAccessor) {
                classType = ct;
            } else if (ins == INVOKEVIRTUAL && invokeSuper) {
                ins = INVOKESPECIAL;
            }
        }
        if (object != null &&
                (ins != INVOKEINTERFACE || ctx.compilation.isGCJ)) {
            ctx.typeInsn(CHECKCAST, classType.className());
        }
        genCall(ctx, null, ins);
    }

    void gen(Ctx ctx) {
        _gen(ctx);
        if (method.returnType.type == YetiType.UNIT) {
            ctx.insn(ACONST_NULL);
        } else {
            convertValue(ctx, method.returnType);
        }
    }

    void genIf(Ctx ctx, Label to, boolean ifTrue) {
        if (method.returnType.javaType != null &&
                method.returnType.javaType.description == "Z") {
            _gen(ctx);
            ctx.jumpInsn(ifTrue ? IFNE : IFEQ, to);
        } else {
            super.genIf(ctx, to, ifTrue);
        }
    }
}

