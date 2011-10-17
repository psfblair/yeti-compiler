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

import java.util.ArrayList;
import java.util.List;

import yeti.lang.compiler.code.BindExpr;
import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;

abstract class AClosure extends Code implements Closure {
    private List closureVars = new ArrayList();

    public void addVar(BindExpr binder) {
        closureVars.add(binder);
    }

    public final void genClosureInit(Ctx ctx) {
        int id = -1, mvarcount = 0;
        for (int i = closureVars.size(); --i >= 0;) {
            BindExpr bind = (BindExpr) closureVars.get(i);
            if (bind.isAssigned() && bind.isCaptured()) {
                if (id == -1) {
                    id = ctx.getLocalVarCount();
                    ctx.incrementLocalVarCountBy(1);
                }
                bind.setMVarId(this, id, mvarcount++);
            }
        }
        if (mvarcount > 0) {
            ctx.intConst(mvarcount);
            ctx.typeInsn(ANEWARRAY, "java/lang/Object");
            ctx.varInsn(ASTORE, id);
        }
    }
}