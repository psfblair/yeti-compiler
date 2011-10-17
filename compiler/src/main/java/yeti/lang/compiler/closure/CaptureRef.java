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

/*
 * Bind reference that is actually some wrapper created by closure (capturer).
 * This class is mostly useful as a place where tail call optimization happens.
 */
abstract class CaptureRef extends BindRef {
    Function capturer;
    BindRef ref;
    Binder[] args;
    Capture[] argCaptures;

    final class SelfApply extends Apply {
        boolean tail;
        int depth;

        SelfApply(YType type, Code f, Code arg,
                  int line, int depth) {
            super(type, f, arg, line);
            this.depth = depth;
            if (getOrigin() != null) {
                this.arity = args.length - depth + 1;
                getOrigin().setArity(this.arity);
                this.setRef(getOrigin());
            }
        }

        // evaluates call arguments and pushes values into stack
        void genArg(Ctx ctx, int i) {
            if (i > 0)
                ((SelfApply) fun).genArg(ctx, i - 1);
            arg.gen(ctx);
        }

        public void gen(Ctx ctx) {
            if (!tail || depth != 0 ||
                capturer.argCaptures != argCaptures ||
                capturer.restart == null) {
                // regular apply, if tail call optimisation can't be done
                super.gen(ctx);
                return;
            }
            // push all argument values into stack - they must be evaluated
            // BEFORE modifying any of the arguments for tail-"call"-jump.
            genArg(ctx, argCaptures == null ? 0 : argCaptures.length);
            ctx.varInsn(ASTORE, capturer.argVar);
            // Now assign the call argument values into argument registers.
            if (argCaptures != null)
                for (int i = argCaptures.length; --i >= 0;)
                    if (argCaptures[i] != null)
                        ctx.varInsn(ASTORE, argCaptures[i].localVar);
                    else
                        ctx.insn(POP);
            // And just jump into the start of the function...
            ctx.jumpInsn(GOTO, capturer.restart);
        }

        protected void markTail() {
            tail = true;
        }

        Code apply(Code arg, YType res, int line) {
            if (depth < 0)
                return super.apply(arg, res, line);
            if (depth == 1 && capturer.argCaptures == null) {
                /*
                 * All arguments have been applied, now we have to search
                 * their captures in the inner function (by looking for
                 * captures matching the function arguments).
                 * Resulting list will be also given to the inner function,
                 * so it could copy those captures into local registers
                 * to allow tail call.
                 *
                 * NB. To understand this, remember that this is self-apply,
                 * so current scope is also the scope of applied function.
                 */
                argCaptures = new Capture[args.length];
                for (Capture c = capturer.captures; c != null; c = c.next)
                    for (int i = args.length; --i >= 0;)
                        if (c.getBinder() == args[i]) {
                            argCaptures[i] = c;
                            break;
                        }
                capturer.argCaptures = argCaptures;
            }
            return new SelfApply(res, this, arg, line, depth - 1);
        }
    }

    protected Code apply(Code arg, YType res, int line) {
        if (args != null) {
            return new SelfApply(res, this, arg, line, args.length);
        }

        /*
         * We have application with arg x like ((f x) y) z.
         * Now we take the inner function of our scope and travel
         * through its outer functions until there is one.
         *
         * If function that recognizes f as itself is met,
         * we know that this is self-application and how many
         * arguments are needed to do tail-call optimisation.
         * SelfApply with arguments count is given in that case.
         *
         * SelfApply.apply reduces the argument count until final
         * call is reached, in which case tail-call can be done,
         * if the application happens to be in tail position.
         */
        int n = 0;
        for (Function f = capturer; f != null; ++n, f = f.outer)
            if (f.selfBind == ref.getBinder()) {
                args = new Binder[n];
                f = capturer.outer;
                for (int i = n; --i >= 0; f = f.outer)
                    args[i] = f;
                return new SelfApply(res, this, arg, line, n);
            }
        return super.apply(arg, res, line);
    }
}
