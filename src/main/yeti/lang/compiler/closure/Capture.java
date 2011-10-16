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

final class Capture extends CaptureRef implements CaptureWrapper, CodeGen {
    String id;
    Capture next;
    CaptureWrapper wrapper;
    Object identity;
    int localVar = -1; // -1 - use this (TryCatch captures use 0 localVar)
    boolean uncaptured;
    boolean ignoreGet;
    private String refType;

    public void gen(Ctx ctx) {
        if (uncaptured) {
            ref.gen(ctx);
            return;
        }
        genPreGet(ctx);
        genGet(ctx);
    }

    String getId(Ctx ctx) {
        if (id == null) {
            id = "_".concat(Integer.toString(ctx.getFieldCounter()));
            ctx.incrementFieldCounterBy(1);
        }
        return id;
    }

    protected boolean flagop(int fl) {
        /*
         * DIRECT_BIND is allowed, because with code like
         * x = 1; try x finally yrt
         * the 1 won't get directly brought into try closure
         * unless the mergeCaptures uncaptures the DIRECT_BIND ones
         * (the variable doesn't (always) know that it will be
         * a direct binding when it's captured, as this determined
         * later using prepareConst())
         */
        return (fl & (PURE | ASSIGN | DIRECT_BIND)) != 0 && ref.flagop(fl);
    }

    public void gen2(Ctx ctx, Code value, int _) {
        if (uncaptured) {
            ref.assign(value).gen(ctx);
        } else {
            genPreGet(ctx);
            wrapper.genSet(ctx, value);
            ctx.insn(ACONST_NULL);
        }
    }

    protected Code assign(final Code value) {
        if (!ref.flagop(ASSIGN))
            return null;
        return new SimpleCode(this, value, null, 0);
    }

    public void genPreGet(Ctx ctx) {
        if (uncaptured) {
            wrapper.genPreGet(ctx);
        } else if (localVar < 0) {
            ctx.load(0);
            if (localVar < -1) {
                ctx.intConst(-2 - localVar);
                ctx.insn(AALOAD);
            } else {
                ctx.fieldInsn(GETFIELD, ctx.getClassName(), id, captureType());
            }
        } else {
            ctx.load(localVar);
            // hacky way to forceType on try-catch, but not on method argument
            if (!ignoreGet) {
                ctx.forceType(captureType().charAt(0) == '['
                        ? refType : refType.substring(1, refType.length() - 1));
            }
        }
    }

    public void genGet(Ctx ctx) {
        if (wrapper != null && !ignoreGet) {
            /*
             * The object got from capture might not be the final value.
             * for example captured mutable variables are wrapped into array
             * by the binding, so the wrapper must get correct array index
             * out of the array in that case.
             */
            wrapper.genGet(ctx);
        }
    }

    public void genSet(Ctx ctx, Code value) {
        wrapper.genSet(ctx, value);
    }

    public CaptureWrapper capture() {
        if (uncaptured) {
            return ref.capture();
        }
        return wrapper == null ? null : this;
    }

    public Object captureIdentity() {
        return wrapper == null ? this : wrapper.captureIdentity();
    }

    public String captureType() {
        if (refType == null) {
            if (wrapper != null) {
                refType = wrapper.captureType();
                if (refType == null)
                    throw new IllegalStateException("captureType:" + wrapper);
            } else if (getOrigin() != null) {
                refType = ((BindExpr) getBinder()).captureType();
            } else {
                refType = 'L' + javaType(ref.getType()) + ';';
            }
        }
        return refType;
    }

    void captureGen(Ctx ctx) {
        if (wrapper == null) {
            ref.gen(ctx);
        } else {
            wrapper.genPreGet(ctx);
        }
        // stupid AALOAD in genPreGet returns shit,
        // so have to captureCast for it...
        ctx.captureCast(captureType());
    }

    BindRef unshare() {
        return new BindWrapper(this);
    }
}

