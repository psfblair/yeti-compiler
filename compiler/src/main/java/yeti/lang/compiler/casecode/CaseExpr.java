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

package yeti.lang.compiler.casecode;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.renamed.asm3.*;
import java.util.*;

final class CaseExpr extends Code {
    private int totalParams;
    private Code caseValue;
    private List choices = new ArrayList();
    int paramStart;
    int paramCount;

    CaseExpr(Code caseValue) {
        this.caseValue = caseValue;
    }

    private static final class Choice {
        CasePattern pattern;
        Code expr;
    }

    void resetParams() {
        if (totalParams < paramCount) {
            totalParams = paramCount;
        }
        paramCount = 0;
    }

    void addChoice(CasePattern pattern, Code code) {
        Choice c = new Choice();
        c.pattern = pattern;
        c.expr = code;
        choices.add(c);
    }

    public void gen(Ctx ctx) {
        caseValue.gen(ctx);
        paramStart = ctx.getLocalVarCount();
        ctx.incrementLocalVarCountBy(totalParams);
        Label next = null, end = new Label();
        CasePattern lastPattern = ((Choice) choices.get(0)).pattern;
        int patternStack = lastPattern.preparePattern(ctx);

        for (int last = choices.size() - 1, i = 0; i <= last; ++i) {
            Choice c = (Choice) choices.get(i);
            if (lastPattern.getClass() != c.pattern.getClass()) {
                ctx.popn(patternStack - 1);
                patternStack = c.pattern.preparePattern(ctx);
            }
            lastPattern = c.pattern;
            next = new Label();
            c.pattern.tryMatch(ctx, next, true);
            ctx.popn(patternStack);
            c.expr.gen(ctx);
            ctx.jumpInsn(GOTO, end);
            ctx.visitLabel(next);
        }
        ctx.visitLabel(next);
        ctx.popn(patternStack - 1);
        ctx.methodInsn(INVOKESTATIC, "yeti/lang/Core",
                       "badMatch", "(Ljava/lang/Object;)Ljava/lang/Object;");
        ctx.visitLabel(end);
    }

    @Override
    protected void markTail() {
        for (int i = choices.size(); --i >= 0;) {
            ((Choice) choices.get(i)).expr.markTail();
        }
    }
}
