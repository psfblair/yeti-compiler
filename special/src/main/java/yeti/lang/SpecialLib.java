// ex: se sts=4 sw=4 expandtab:

/**
 * Yeti special library utility.
 *
 * Copyright (c) 2008,2009 Madis Janson
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
package yeti.lang;

import yeti.renamed.asm3.ClassReader;
import yeti.renamed.asm3.ClassWriter;
import yeti.renamed.asm3.MethodVisitor;
import yeti.renamed.asm3.Opcodes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;

public class SpecialLib implements Opcodes {
    ClassWriter cw;
    String prefix;

    void transformLList() throws Exception {
        InputStream in = new FileInputStream(prefix + "yeti/lang/LList.class");
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        LListAdapter la = new LListAdapter(cw);
        new ClassReader(in).accept(la, 0);
        in.close();
        storeClass("yeti/lang/LList.class");
    }

    void storeClass(String name) throws Exception {
        cw.visitEnd();
        name = prefix + name;
        int p = name.lastIndexOf('/');
        new java.io.File(name.substring(0, p)).mkdirs();
        FileOutputStream os = new FileOutputStream(name);
        os.write(cw.toByteArray());
        os.close();
    }

    void fun2_() throws Exception {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_SUPER,
                 "yeti/lang/Fun2_", null, "yeti/lang/Fun", null);
        cw.visitField(0, "fun", "Lyeti/lang/Fun2;", null, null).visitEnd();
        cw.visitField(0, "arg", "Ljava/lang/Object;", null, null).visitEnd();
        MethodVisitor mv = cw.visitMethod(0, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "yeti/lang/Fun", "<init>", "()V");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        mv = cw.visitMethod(ACC_PUBLIC, "apply",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "yeti/lang/Fun2_",
                          "fun", "Lyeti/lang/Fun2;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 0);
        mv.visitFieldInsn(GETFIELD, "yeti/lang/Fun2_",
                          "arg", "Ljava/lang/Object;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun2", "apply",
            "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        storeClass("yeti/lang/Fun2_.class");
    }

    void compose() throws Exception {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_PUBLIC | ACC_FINAL | ACC_SUPER,
                 "yeti/lang/Compose", null, "yeti/lang/Fun", null);
        cw.visitField(ACC_FINAL | ACC_PRIVATE,
                      "f", "Lyeti/lang/Fun;", null, null).visitEnd();
        cw.visitField(ACC_FINAL | ACC_PRIVATE,
                      "g", "Lyeti/lang/Fun;", null, null).visitEnd();
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>",
            "(Ljava/lang/Object;Ljava/lang/Object;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "yeti/lang/Fun", "<init>", "()V");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 1);
        mv.visitTypeInsn(CHECKCAST, "yeti/lang/Fun");
        mv.visitFieldInsn(PUTFIELD, "yeti/lang/Compose",
                          "f", "Lyeti/lang/Fun;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitTypeInsn(CHECKCAST, "yeti/lang/Fun");
        mv.visitFieldInsn(PUTFIELD, "yeti/lang/Compose",
                          "g", "Lyeti/lang/Fun;");
        mv.visitInsn(RETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();

        mv = cw.visitMethod(ACC_PUBLIC, "apply",
                    "(Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "yeti/lang/Compose",
                          "f", "Lyeti/lang/Fun;");
        mv.visitVarInsn(ALOAD, 0);
        mv.visitFieldInsn(GETFIELD, "yeti/lang/Compose",
                          "g", "Lyeti/lang/Fun;");
        mv.visitVarInsn(ALOAD, 1);
        mv.visitInsn(ACONST_NULL);
        mv.visitVarInsn(ASTORE, 1);
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun", "apply",
            "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitMethodInsn(INVOKEVIRTUAL, "yeti/lang/Fun", "apply",
            "(Ljava/lang/Object;)Ljava/lang/Object;");
        mv.visitInsn(ARETURN);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        storeClass("yeti/lang/Compose.class");
    }

    void unsafe() throws Exception {
        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        cw.visit(V1_4, ACC_SUPER, "yeti/lang/Unsafe",
                 null, "java/lang/Object", null);
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC | ACC_STATIC,
                "unsafeThrow", "(Ljava/lang/Throwable;)V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitInsn(ATHROW);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        storeClass("yeti/lang/Unsafe.class");
    }

    public static void main(String[] args) throws Exception {
        SpecialLib l = new SpecialLib();
        l.prefix = args[1] + '/';
        if (args[0].equals("tr")) {
            l.transformLList();
        } else if (args[0].equals("pre")) {
            l.fun2_();
            l.compose();
            l.unsafe();
        } else {
            System.err.println(args[0] + ": WTF?");
        }
    }
}
