package yeti.lang;

import yeti.renamed.asm3.ClassAdapter;
import yeti.renamed.asm3.ClassVisitor;
import yeti.renamed.asm3.Label;
import yeti.renamed.asm3.MethodVisitor;
import yeti.renamed.asm3.Opcodes;

class LListAdapter extends ClassAdapter implements Opcodes {
    LListAdapter(ClassVisitor cv) {
        super(cv);
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        return "length".equals(name) || "forEach".equals(name) ||
               "fold".equals(name) || "smap".equals(name) ||
               "copy".equals(name) ? null
                : cv.visitMethod(access, name, desc, signature, exceptions);
    }

    public void visitEnd() {
        MethodVisitor mv =
            cv.visitMethod(ACC_PUBLIC, "length", "()J", null, null);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ICONST_0);
        mv.visitVarInsn(ISTORE, 0);
        Label retry = new Label(), end = new Label();
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, end);
        mv.visitLabel(retry);
        mv.visitIincInsn(0, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "next", "()Lyeti/lang/AIter;");
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, retry);
        mv.visitLabel(end);
        mv.visitVarInsn(ILOAD, 0);
        mv.visitInsn(I2L);
        mv.visitInsn(LRETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cv.visitMethod(ACC_PUBLIC, "forEach",
                            "(Ljava/lang/Object;)V", null, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "yeti/lang/Fun");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, end = new Label());
        mv.visitLabel(retry = new Label());
        mv.visitInsn(DUP2); // fun iter fun iter
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "first", "()Ljava/lang/Object;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun",
                           "apply", "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitInsn(POP);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "next", "()Lyeti/lang/AIter;");
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, retry);
        mv.visitLabel(end);
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cv.visitMethod(ACC_PUBLIC, "fold",
                    "(Lyeti/lang/Fun;Ljava/lang/Object;)Ljava/lang/Object;",
                    null, null);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 2);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitJumpInsn(IFNULL, end = new Label());
        mv.visitLabel(retry = new Label());
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(SWAP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "first", "()Ljava/lang/Object;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun", "apply",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "next", "()Lyeti/lang/AIter;");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitJumpInsn(IFNONNULL, retry);
        mv.visitLabel(end);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cv.visitMethod(ACC_PUBLIC, "smap",
                            "(Lyeti/lang/Fun;)Lyeti/lang/AList;", null, null);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "yeti/lang/Fun");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNULL, end = new Label());
        mv.visitTypeInsn(NEW, "yeti/lang/MList");
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, "yeti/lang/MList",
                           "<init>", "()V");
        mv.visitVarInsn(ASTORE, 0);
        mv.visitLabel(retry = new Label());
        mv.visitInsn(DUP2); // fun iter fun iter
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "first", "()Ljava/lang/Object;"); // i -> v
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun", // f v -> v'
                           "apply", "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(SWAP);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/MList",
                           "add", "(Ljava/lang/Object;)V"); // l v' -> ()
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                           "next", "()Lyeti/lang/AIter;"); // i -> i'
        mv.visitInsn(DUP);
        mv.visitJumpInsn(IFNONNULL, retry);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ARETURN);
        mv.visitLabel(end);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cv.visitMethod(ACC_PUBLIC, "copy",
                            "()Ljava/lang/Object;", null, null);
        mv.visitTypeInsn(NEW, "yeti/lang/MList");
        mv.visitInsn(DUP);
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "yeti/lang/MList",
                           "<init>", "(Lyeti/lang/AIter;)V");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        cv.visitEnd();
    }
}
