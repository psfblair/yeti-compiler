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
import yeti.lang.compiler.code.Ctx;

public abstract class CapturingClosure extends AClosure {
    Capture captures;

    Capture captureRef(BindRef code) {
        for (Capture c = captures; c != null; c = c.next)
            if (c.getBinder() == code.getBinder()) {
                // evil hack... ref sharing broke fun-method
                // optimisation accounting of ref usage
                c.setOrigin(code.getOrigin());
                return c;
            }
        Capture c = new Capture();
        c.setBinder(code.getBinder());
        c.setType(code.getType());
        c.setPolymorph(code.isPolymorph());
        c.ref = code;
        c.wrapper = code.capture();
        c.setOrigin(code.getOrigin());
        c.next = captures;
        captures = c;
        return c;
    }

    public BindRef refProxy(BindRef code) {
        return code.flagop(DIRECT_BIND) ? code : captureRef(code);
    }

    // Called by mergeCaptures to initialize a capture.
    // It must be ok to copy capture after that.
    abstract void captureInit(Ctx fun, Capture c, int n);

    // mergeCaptures seems to drop only some uncaptured ones
    // (looks like because so is easy to do, currently
    // this seems to cause extra check only in Function.finishGen).
    int mergeCaptures(Ctx ctx, boolean cleanup) {
        int counter = 0;
        Capture prev = null;
    next_capture:
        for (Capture c = captures; c != null; c = c.next) {
            Object identity = c.identity = c.captureIdentity();
            if (cleanup && (c.uncaptured || c.ref.flagop(DIRECT_BIND))) {
                c.uncaptured = true;
                if (prev == null)
                    captures = c.next;
                else
                    prev.next = c.next;
            }
            if (c.uncaptured)
                continue;
            // remove shared captures
            for (Capture i = captures; i != c; i = i.next) {
                if (i.identity == identity) {
                    c.id = i.id; // copy old one's id
                    c.localVar = i.localVar;
                    prev.next = c.next;
                    continue next_capture;
                }
            }
            captureInit(ctx, c, counter++);
            prev = c;
        }
        return counter;
    }
}

