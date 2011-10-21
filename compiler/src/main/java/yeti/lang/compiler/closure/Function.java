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

import yeti.lang.compiler.code.*;
import yeti.lang.compiler.yeti.type.YType;
import yeti.renamed.asm3.Label;

import java.util.IdentityHashMap;
import java.util.Map;

public final class Function extends CapturingClosure implements Binder {
    private static final Code NEVER = new Code() {
        public void gen(Ctx ctx) {
            throw new UnsupportedOperationException();
        }
    };

    private String name; // name of the generated function class
    Binder selfBind;
    Code body;
    String bindName; // function (self)binding name, if there is any

    // function body has asked self reference (and the ref is not mutable)
    private CaptureRef selfRef;
    Label restart; // used by tail-call optimizer
    Function outer; // outer function of directly-nested function
    // outer arguments to be saved in local registers (used for tail-call)
    Capture[] argCaptures;
    // argument value for inlined function
    private Code uncaptureArg;
    // register used by argument (2 for merged inner function)
    int argVar = 1;
    // Marks function optimised as method and points to it's inner-most lambda
    Function methodImpl;
    // Function has been merged with its inner function.
    private boolean merged; 
    // How many times the argument has been used.
    // This counter is also used by argument nulling to determine
    // when it safe to assume that argument value is no more needed.
    private int argUsed;
    // Function is constant that can be statically shared.
    // Stores function instance in static final _ field and allows
    // direct-ref no-capture optimisations for function binding.
    private boolean shared;
    // Module has asked function to be a public (inner) class.
    // Useful for making Java code happy, if it wants to call the function.
    boolean publish;
    // Function uses local bindings from its module. Published function
    // should ensure module initialisation in this case, when called.
    private boolean moduleInit;
    // methodImpl and only one live capture - carry it directly.
    boolean capture1;

    final BindRef arg = new BindRef() {
        void gen(Ctx ctx) {
            if (uncaptureArg != null) {
                uncaptureArg.gen(ctx);
            } else {
                ctx.load(argVar);
                // inexact nulling...
                if (--argUsed == 0 && ctx.getTainted() == 0) {
                    ctx.insn(ACONST_NULL);
                    ctx.varInsn(ASTORE, argVar);
                }
            }
        }

        boolean flagop(int fl) {
            return (fl & PURE) != 0;
        }
    };

    Function(YType type) {
        setType(type);
        arg.setBinder(this);
    }

    public BindRef getRef(int line) {
        ++argUsed;
        return arg;
    }

    public String getName() {
        return name;
    }

    // uncaptures captured variables if possible
    // useful for function inlineing, don't work with self-refs
    boolean uncapture(Code arg) {
        if (selfRef != null || merged)
            return false;
        for (Capture c = captures; c != null; c = c.next)
            c.uncaptured = true;
        uncaptureArg = arg;
        return true;
    }

    void setBody(Code body) {
        this.body = body;
        if (body instanceof Function) {
            Function bodyFun = (Function) body;
            bodyFun.outer = this;
            if (argVar == 1 && !bodyFun.merged && bodyFun.selfRef == null) {
                merged = true;
                ++bodyFun.argVar;
            }
        }
    }

    /*
     * When function body refers to bindings outside of it,
     * at each closure border on the way out (to the binding),
     * a refProxy (of the ending closure) is called, possibly
     * transforming the BindRef.
     */
    public BindRef refProxy(BindRef code) {
        if (code.flagop(DIRECT_BIND)) {
            if (code.flagop(MODULE_REQUIRED)) {
                moduleInit = true;
            }
            return code;
        }
        if (selfBind == code.getBinder() && !code.flagop(ASSIGN)) {
            if (selfRef == null) {
                selfRef = new CaptureRef() {
                    public void gen(Ctx ctx) {
                        ctx.load(0);
                    }
                };
                selfRef.setBinder(selfBind);
                selfRef.setType(code.getType());
                selfRef.ref = code;

                // Right place for this should be outside of if (so it would
                // be updated on multiple selfRefs), but allowing method-fun
                // in such case slows some code down (b/c array capture).
                // Having it here means non-first self-refs arity stays zero
                // and so these will be considered to be used as fun-values.
                selfRef.setOrigin(code.getOrigin());

                selfRef.capturer = this;
            }
            // selfRef.origin = code.origin;
            return selfRef;
        }
        if (merged) {
            return code;
        }
        Capture c = captureRef(code);
        c.capturer = this;
        //expecting max 2 merged
        if (outer != null && outer.merged &&
            (code == outer.selfRef || code == outer.arg)) {
            /*
             * It's actually simple - because nested functions are merged,
             * the parent argument is now real argument that can be
             * directly accessed. Therefore capture proxy would only
             * fuck things up - and so that proxy is marked uncaptured.
             * Same goes for the parent-self-ref - it is now our this.
             *
             * Only problem is that tail-rec optimisation generates code,
             * that wants to store into the "captured" variables copy
             * before jumping back into the start of the function.
             * The optimiser sets argCaptures which should be copied
             * into local vars by function class generator, but this
             * coping is skipped as pointless for uncaptured ones.
             *
             * Therefore the captures localVar is simply set here to 1,
             * which happens to be parent args register (and is ignored
             * by selfRefs). Probable alternative would be to set it
             * when the copy code generation is skipped.
             */
            c.localVar = 1; // really evil hack for tail-recursion.
            c.uncaptured = true;
        }
        return c;
    }

    // called by mergeCaptures
    void captureInit(Ctx fun, Capture c, int n) {
        if (methodImpl == null) {
            // c.getId() initialises the captures id as a side effect
            fun.cw.visitField(0, c.getId(fun), c.captureType(),
                              null, null).visitEnd();
        } else if (capture1) {
            assert (n == 0);
            c.localVar = 0;
            fun.load(0).captureCast(c.captureType());
            fun.varInsn(ASTORE, 0);
        } else {
            c.localVar = -2 - n;
        }
    }

    private void prepareMethod(Ctx ctx) {
        /*
         * The make-a-method trick is actually damn easy I think.
         * The captures of the innermost joined lambda must be set
         * to refer to the method arguments and closure array instead.
         * This is done by mapping our arguments and outer capture set
         * into good vars. After that the inner captures can be scanned
         * and made to point to those values.
         */
        // Map captures using binder as identity.
        Map captureMapping = null;

        /*
         * This has to be done before mergeCaptures to have all binders.
         * NOP for 1/2-arg functions - they don't have argument captures and
         * the outer captures localVar's will be set by mergeCaptures.
         */
        if (methodImpl != this && methodImpl != body) {
            captureMapping = new IdentityHashMap();

            // Function is binder for it's argument
            int argCounter = 0;
            for (Function f = this; f != methodImpl; f = (Function) f.body) {
                // just to hold localVar
                Capture tmp = new Capture();
                tmp.localVar = ++argCounter;
                f.argVar = argCounter; // merge fucks up the pre-last capture
                captureMapping.put(f, tmp);
            }
            methodImpl.argVar = ++argCounter;

            for (Capture c = captures; c != null; c = c.next)
                captureMapping.put(c.getBinder(), c);
            Capture tmp = new Capture();
            tmp.localVar = 0;
            captureMapping.put(selfBind, tmp);
        }

        // Create method
        Map usedNames = ctx.getUsedMethodNames();
        bindName = bindName != null ? mangle(bindName) : "_";
        if (usedNames.containsKey(bindName) || bindName.startsWith("_"))
            bindName += usedNames.size();
        usedNames.put(bindName, null);
        StringBuffer sig = new StringBuffer(capture1 ? "(" : "([");
        for (int i = methodImpl.argVar + 2; --i >= 0;) {
            if (i == 0)
                sig.append(')');
            sig.append("Ljava/lang/Object;");
        }
        Ctx m = ctx.newMethod(ACC_STATIC, bindName, sig.toString());
        
        // Removes duplicate captures and calls captureInit
        // (which sets captures localVar for our case).
        int captureCount = mergeCaptures(m, false);

        // Hijack the inner functions capture mapping...
        if (captureMapping != null)
            for (Capture c = methodImpl.captures; c != null; c = c.next) {
                Object mapped = captureMapping.get(c.getBinder());
                if (mapped != null) {
                    c.localVar = ((Capture) mapped).localVar;
                    c.ignoreGet = c.localVar > 0;
                } else { // Capture was stealed away by direct bind?
                    Capture x = c;
                    while (x.capturer != this && x.ref instanceof Capture)
                        x = (Capture) x.ref;
                    if (x.uncaptured) {
                        c.ref = x.ref;
                        c.uncaptured = true;
                    }
                }
            }

        // Generate method body
        name = ctx.getClassName();
        m.localVarCount = methodImpl.argVar + 1; // capturearray, args
        methodImpl.genClosureInit(m);
        m.visitLabel(methodImpl.restart = new Label());
        methodImpl.body.gen(m);
        methodImpl.restart = null;
        m.insn(ARETURN);
        m.closeMethod();

        if (!shared && !capture1) {
            ctx.intConst(captureCount);
            ctx.typeInsn(ANEWARRAY, "java/lang/Object");
        }
    }

    /*
     * For functions, this generates the function class.
     * An instance is also given, but capture fields are not initialised
     * (the captures are set later in the finishGen).
     */
    public void prepareGen(Ctx ctx) {
        if (methodImpl != null) {
            prepareMethod(ctx);
            return;
        }

        if (merged) { // 2 nested lambdas have been optimised into 1
            Function inner = (Function) body;
            inner.bindName = bindName;
            inner.prepareGen(ctx);
            name = inner.name;
            return;
        }

        if (bindName == null)
            bindName = "";
        name = ctx.compilation.createClassName(ctx,
                        ctx.getClassName(), mangle(bindName));

        publish &= shared;
        String funClass =
            argVar == 2 ? "yeti/lang/Fun2" : "yeti/lang/Fun";
        Ctx fun = ctx.newClass(publish ? ACC_PUBLIC | ACC_SUPER | ACC_FINAL
                                       : ACC_SUPER | ACC_FINAL,
                               name, funClass, null);

        if (publish)
            fun.markInnerClass(ctx, ACC_PUBLIC | ACC_STATIC | ACC_FINAL);
        mergeCaptures(fun, false);
        fun.createInit(shared ? ACC_PRIVATE : 0, funClass);

        Ctx apply = argVar == 2
            ? fun.newMethod(ACC_PUBLIC + ACC_FINAL, "apply",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;")
            : fun.newMethod(ACC_PUBLIC + ACC_FINAL, "apply",
                "(Ljava/lang/Object;)Ljava/lang/Object;");
        apply.localVarCount = argVar + 1; // this, arg
        
        if (argCaptures != null) {
            // Tail recursion needs all args to be in local registers
            // - otherwise it couldn't modify them safely before restarting
            for (int i = 0; i < argCaptures.length; ++i) {
                Capture c = argCaptures[i];
                if (c != null && !c.uncaptured) {
                    c.gen(apply);
                    c.localVar = apply.getLocalVarCount();
                    c.ignoreGet = true;
                    apply.varInsn(ASTORE, apply.getLocalVarCount());
                    apply.incrementLocalVarCountBy(1);
                }
            }
        }
        if (moduleInit && publish) {
            apply.methodInsn(INVOKESTATIC, ctx.getClassName(),
                             "eval", "()Ljava/lang/Object;");
            apply.insn(POP);
        }
        genClosureInit(apply);
        apply.visitLabel(restart = new Label());
        body.gen(apply);
        restart = null;
        apply.insn(ARETURN);
        apply.closeMethod();

        Ctx valueCtx =
            shared ? fun.newMethod(ACC_STATIC, "<clinit>", "()V") : ctx;
        valueCtx.typeInsn(NEW, name);
        valueCtx.insn(DUP);
        valueCtx.visitInit(name, "()V");
        if (shared) {
            fun.cw.visitField(ACC_PUBLIC | ACC_STATIC | ACC_FINAL,
                              "_", "Lyeti/lang/Fun;", null, null).visitEnd();
            valueCtx.fieldInsn(PUTSTATIC, name, "_", "Lyeti/lang/Fun;");
            valueCtx.insn(RETURN);
            valueCtx.closeMethod();
        }
    }

    public void finishGen(Ctx ctx) {
        if (merged) {
            ((Function) body).finishGen(ctx);
            return;
        }
        boolean meth = methodImpl != null;
        int counter = -1;
        // Capture a closure
        for (Capture c = captures; c != null; c = c.next) {
            if (c.uncaptured)
                continue;
            if (capture1) {
                c.captureGen(ctx);
                return;
            }
            ctx.insn(DUP);
            if (meth) {
                ctx.intConst(++counter);
                c.captureGen(ctx);
                ctx.insn(AASTORE);
            } else {
                c.captureGen(ctx);
                ctx.fieldInsn(PUTFIELD, name, c.id, c.captureType());
            }
        }
        ctx.forceType(meth ? "[Ljava/lang/Object;" : "yeti/lang/Fun");
    }

    boolean flagop(int fl) {
        return merged ? ((Function) body).flagop(fl) :
                (fl & (PURE | CONST)) != 0 && (shared || captures == null);
    }

    // Check whether all captures are actually static constants.
    // If so, the function value should also be optimised into shared constant.
    boolean prepareConst(Ctx ctx) {
        if (shared) // already optimised into static constant value
            return true;

        BindExpr bindExpr = null;
        // First try determine if we can reduce into method.
        if (selfBind instanceof BindExpr &&
                (bindExpr = (BindExpr) selfBind).evalId == -1 &&
                bindExpr.result != null) {
            int arityLimit = 99999999;
            for (BindExpr.Ref i = bindExpr.refs; i != null; i = i.next) {
                if (arityLimit > i.arity)
                    arityLimit = i.arity;
            }
            int arity = 0;
            Function impl = this;
            while (++arity < arityLimit && impl.body instanceof Function)
                impl = (Function) impl.body;
            /*
             * Merged ones are a bit tricky - their capture set is
             * merged into their inner one, where is also their own
             * argument. Also their inner ones arg is messed up.
             * Easier to not touch them, although it would be good for speed.
             */
            if (arity > 0 && arityLimit > 0 && (arity > 1 || !merged)) {
                //System.err.println("FF " + arity + " " + arityLimit +
                //                   " " + bindName);
                if (merged) { // steal captures and unmerge :)
                    captures = ((Function) body).captures;
                    merged = false;
                }
                methodImpl = impl.merged ? impl.outer : impl;
                bindExpr.setCaptureType("[Ljava/lang/Object;");
            }
        }

        if (merged) {
            // merged functions are hollow, their juice is in the inner function
            Function inner = (Function) body;
            inner.bindName = bindName;
            inner.publish = publish;
            if (inner.prepareConst(ctx)) {
                name = inner.name; // used by gen
                return true;
            }
            return false;
        }

        // this can be optimised into "const x", so don't touch.
        if (argUsed == 0 && argVar == 1 &&
                methodImpl == null && body.flagop(PURE))
            return false; //captures == null;

        // Uncapture the direct bindings.
        Capture prev = null;
        int liveCaptures = 0;
        for (Capture c = captures; c != null; c = c.next)
            if (c.ref.flagop(DIRECT_BIND)) {
                c.uncaptured = true;
                if (prev == null)
                    captures = c.next;
                else
                    prev.next = c.next;
            } else {
                // Why in the hell are existing uncaptured ones preserved?
                // Does some checks them (selfref, args??) after prepareConst?
                if (!c.uncaptured)
                    ++liveCaptures;
                prev = c;
            }
        
        if (methodImpl != null && liveCaptures == 1) {
            capture1 = true;
            bindExpr.setCaptureType("java/lang/Object");
        }

        // If all captures were uncaptured, then the function can
        // (and will) be optimised into shared static constant.
        if (liveCaptures == 0) {
            shared = true;
            prepareGen(ctx);
        }
        return liveCaptures == 0;
    }

    void gen(Ctx ctx) {
        if (shared) {
            if (methodImpl == null)
                ctx.fieldInsn(GETSTATIC, name, "_", "Lyeti/lang/Fun;");
            else
                ctx.insn(ACONST_NULL);
        } else if (!merged && argUsed == 0 && body.flagop(PURE) &&
                   uncapture(NEVER)) {
            // This lambda can be optimised into "const x", so do it.
            genClosureInit(ctx);
            ctx.typeInsn(NEW, "yeti/lang/Const");
            ctx.insn(DUP);
            body.gen(ctx);
            ctx.visitInit("yeti/lang/Const", "(Ljava/lang/Object;)V");
            ctx.forceType("yeti/lang/Fun");
        } else if (prepareConst(ctx)) {
            ctx.fieldInsn(GETSTATIC, name, "_", "Lyeti/lang/Fun;");
        } else {
            prepareGen(ctx);
            finishGen(ctx);
        }
    }
}