// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler java bytecode generator.
 *
 * Copyright (c) 2007,2008,2009,2010 Madis Janson
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

package yeti.lang.compiler.closure;

import yeti.lang.compiler.code.BindRef;
import yeti.lang.compiler.code.Binder;
import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.yeti.type.YType;
import yeti.renamed.asm3.Label;

import java.util.ArrayList;
import java.util.List;

/*
 * Since the stupid JVM discards local stack when catching exceptions,
 * try-catch blocks have to be converted into fucking closures
 * (at least for the generic case).
 */
final class TryCatch extends CapturingClosure {
    private List catches = new ArrayList();
    private int exVar;
    Code block;
    Code cleanup;

    final class Catch extends BindRef implements Binder {
        Code handler;

        public BindRef getRef(int line) {
            return this;
        }

        public void gen(Ctx ctx) {
            ctx.load(exVar);
        }
    }

    void setBlock(Code block) {
        setType(block.getType());
        this.block = block;
    }

    Catch addCatch(YType ex) {
        Catch c = new Catch();
        c.setType(ex);
        catches.add(c);
        return c;
    }

    void captureInit(Ctx ctx, Capture c, int n) {
        c.localVar = n;
        c.captureGen(ctx);
    }

    public void gen(Ctx ctx) {
        int argc = mergeCaptures(ctx, true);
        StringBuffer sigb = new StringBuffer("(");
        for (Capture c = getCaptures(); c != null; c = c.getNext()) {
            sigb.append(c.captureType());
        }
        sigb.append(")Ljava/lang/Object;");
        String sig = sigb.toString();
        String name = "_" + ctx.getUsedMethodNames().size();
        ctx.getUsedMethodNames().put(name, null);
        ctx.methodInsn(INVOKESTATIC, ctx.getClassName(), name, sig);
        Ctx mc = ctx.newMethod(ACC_PRIVATE | ACC_STATIC, name, sig);
        mc.setLocalVarCount(argc);

        Label codeStart = new Label(), codeEnd = new Label();
        Label cleanupStart = cleanup == null ? null : new Label();
        Label cleanupEntry = cleanup == null ? null : new Label();
        genClosureInit(mc);
        int retVar = -1;
        if (cleanupStart != null) {
            retVar = mc.getLocalVarCount();
            mc.incrementLocalVarCountBy(1);
            mc.insn(ACONST_NULL);
            mc.varInsn(ASTORE, retVar); // silence the JVM verifier...
        }
        mc.visitLabel(codeStart);
        block.gen(mc);
        mc.visitLabel(codeEnd);
        exVar = mc.getLocalVarCount();
        mc.incrementLocalVarCountBy(1);
        if (cleanupStart != null) {
            Label goThrow = new Label();
            mc.visitLabel(cleanupEntry);
            mc.varInsn(ASTORE, retVar);
            mc.insn(ACONST_NULL);
            mc.visitLabel(cleanupStart);
            mc.varInsn(ASTORE, exVar);
            cleanup.gen(mc);
            mc.insn(POP); // cleanup's null
            mc.load(exVar).jumpInsn(IFNONNULL, goThrow);
            mc.load(retVar).insn(ARETURN);
            mc.visitLabel(goThrow);
            mc.load(exVar).insn(ATHROW);
        } else {
            mc.insn(ARETURN);
        }
        for (int i = 0, cnt = catches.size(); i < cnt; ++i) {
            Catch c = (Catch) catches.get(i);
            Label catchStart = new Label();
            mc.tryCatchBlock(codeStart, codeEnd, catchStart,
                                  c.getType().getJavaType().className());
            Label catchEnd = null;
            if (cleanupStart != null) {
                catchEnd = new Label();
                mc.tryCatchBlock(catchStart, catchEnd, cleanupStart, null);
            }
            mc.visitLabel(catchStart);
            mc.varInsn(ASTORE, exVar);
            c.handler.gen(mc);
            if (catchEnd != null) {
                mc.visitLabel(catchEnd);
                mc.jumpInsn(GOTO, cleanupEntry);
            } else {
                mc.insn(ARETURN);
            }
        }
        if (cleanupStart != null)
            mc.tryCatchBlock(codeStart, codeEnd, cleanupStart, null);
        mc.closeMethod();
    }
}
