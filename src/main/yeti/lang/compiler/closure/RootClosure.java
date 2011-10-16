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
import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.code.LoadModule;
import yeti.lang.compiler.yeti.typeattr.ModuleType;

public final class RootClosure extends AClosure {
    private Code code;
    private boolean isModule;
    private ModuleType moduleType;
    LoadModule[] preload;

    public BindRef refProxy(BindRef code) {
        return code;
    }

    public Code getCode() {
        return code;
    }

    public boolean isModule() {
        return isModule;
    }

    public ModuleType getModuleType() {
        return moduleType;
    }

    public void gen(Ctx ctx) {
        genClosureInit(ctx);
        for (int i = 0; i < preload.length; ++i) {
            if (preload[i] != null) {
                preload[i].gen(ctx);
                ctx.insn(POP);
            }
        }
        code.gen(ctx);
    }
}
