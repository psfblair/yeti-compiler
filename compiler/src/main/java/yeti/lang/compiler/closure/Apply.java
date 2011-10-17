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

import yeti.lang.compiler.code.BindExpr;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.code.LoadVar;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.code.Code;

public class Apply extends Code {
    private BindExpr.Ref ref;

    final Code fun, arg;
    final int line;
    int arity = 1;

    public Apply(YType res, Code fun, Code arg, int line) {
        setType(res);
        this.fun = fun;
        this.arg = arg;
        this.line = line;
    }

    public void gen(Ctx ctx) {
        Function f;
        int argc = 0;

        // Function sets its methodImpl field, if it has determined that
        // it optimises itself into simple method.
        if (ref != null &&
               (f = (Function) ((BindExpr) ref.getBinder()).getSt()).methodImpl != null
               && arity == (argc = f.methodImpl.argVar)) {
            //System.err.println("A" + arity + " F" + argc);
            // first argument is function value (captures array really)
            StringBuffer sig = new StringBuffer(f.capture1 ? "(" : "([");
            sig.append("Ljava/lang/Object;");
            Apply a = this; // "this" is the last argument applied, so reverse
            Code[] args = new Code[argc];
            for (int i = argc; --i > 0; a = (Apply) a.fun)
                args[i] = a.arg;
            args[0] = a.arg; // out-of-cycle as we need "a" for fun
            a.fun.gen(ctx);
            if (!f.capture1)
                ctx.typeInsn(CHECKCAST, "[Ljava/lang/Object;");
            for (int i = 0; i < argc; ++i) {
                args[i].gen(ctx);
                sig.append("Ljava/lang/Object;");
            }
            sig.append(")Ljava/lang/Object;");
            ctx.visitLine(line);
            ctx.methodInsn(INVOKESTATIC, f.name,
                                f.bindName, sig.toString());
            return;
        }

        if (fun instanceof Function) {
            f = (Function) fun;
            LoadVar arg_ = new LoadVar();
            // inline direct calls
            // TODO: constants don't need a temp variable
            if (f.uncapture(arg_)) {
                arg.gen(ctx);
                arg_.setVar(ctx.getLocalVarCount());
                ctx.incrementLocalVarCountBy(1);
                ctx.varInsn(ASTORE, arg_.getVar());
                f.genClosureInit(ctx);
                f.body.gen(ctx);
                return;
            }
        }

        Apply to = (arity & 1) == 0 && arity - argc > 1 ? (Apply) fun : this;
        to.fun.gen(ctx);
        ctx.visitLine(to.line);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Fun");
        if (to == this) {
            ctx.visitApply(arg, line);
        } else {
            to.arg.gen(ctx);
            arg.gen(ctx);
            ctx.visitLine(line);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/Fun", "apply",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        }
    }

    public BindExpr.Ref getRef() {
        return ref;
    }

    public void setRef(BindExpr.Ref ref) {
        this.ref = ref;
    }

    Code apply(Code arg, final YType res, int line) {
        Apply a = new Apply(res, this, arg, line);
        a.arity = arity + 1;
        if (ref != null) {
            ref.setArity(a.arity);
            a.ref = ref;
        }
        return a;
    }
}
