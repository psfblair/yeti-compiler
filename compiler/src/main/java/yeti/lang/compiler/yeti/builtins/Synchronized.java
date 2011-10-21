// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler java bytecode generator.
 *
 * Copyright (c) 2007,2008,2009 Madis Janson
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

package yeti.lang.compiler.yeti.builtins;

import yeti.lang.compiler.closure.Apply;
import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Label;

final class Synchronized extends Core2 {
    Synchronized(int line) {
        super("synchronized", YetiType.SYNCHRONIZED_TYPE, line);
    }

    void genApply2(Ctx ctx, Code monitor, Code block, int line) {
        monitor.gen(ctx);
        int monitorVar = ctx.getLocalVarCount();
        ctx.incrementLocalVarCountBy(1);
        ctx.visitLine(line);
        ctx.insn(DUP);
        ctx.varInsn(ASTORE, monitorVar);
        ctx.insn(MONITORENTER);

        Label startBlock = new Label(), endBlock = new Label();
        ctx.visitLabel(startBlock);
        new Apply(type, block, new UnitConstant(null), line).gen(ctx);
        ctx.visitLine(line);
        ctx.load(monitorVar).insn(MONITOREXIT);
        ctx.visitLabel(endBlock);
        Label end = new Label();
        ctx.jumpInsn(GOTO, end);

        Label startCleanup = new Label(), endCleanup = new Label();
        ctx.tryCatchBlock(startBlock, endBlock, startCleanup, null);
        // I have no fucking idea, what this second catch is supposed
        // to be doing. javac generates it, so it has to be good.
        // yeah, sure...
        ctx.tryCatchBlock(startCleanup, endCleanup, startCleanup, null);

        int exceptionVar = ctx.getLocalVarCount();
        ctx.incrementLocalVarCountBy(1);
        ctx.visitLabel(startCleanup);
        ctx.varInsn(ASTORE, exceptionVar);
        ctx.load(monitorVar).insn(MONITOREXIT);
        ctx.visitLabel(endCleanup);
        ctx.load(exceptionVar).insn(ATHROW);
        ctx.visitLabel(end);
    }
}


