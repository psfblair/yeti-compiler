// ex: se sts=4 sw=4 expandtab:

/**
 * Yeti language parser.
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

package yeti.lang.compiler.parser;

class TypeNode extends Node {
    String name;
    TypeNode[] param;
    boolean var;
    String doc;

    TypeNode(String name, TypeNode[] param) {
        this.name = name;
        this.param = param;
    }

    String str() {
        if (name == "->")
            return "(" + param[0].str() + " -> " + param[1].str() + ")";
        StringBuffer buf = new StringBuffer();
        if (name == "|") {
            for (int i = 0; i < param.length; ++i) {
                if (i != 0)
                    buf.append(" | ");
                buf.append(param[i].str());
            }
            return buf.toString();
        }
        if (name == "") {
            buf.append('{');
            for (int i = 0; i < param.length; ++i) {
                if (i != 0) {
                    buf.append("; ");
                }
                buf.append(param[i].name);
                buf.append(" is ");
                buf.append(param[i].param[0].str());
            }
            buf.append('}');
            return buf.toString();
        }
        if (param == null || param.length == 0)
            return name;
        if (Character.isUpperCase(name.charAt(0))) {
            return "(" + name + " " + param[0].str() + ")";
        }
        buf.append(name);
        buf.append('<');
        for (int i = 0; i < param.length; ++i) {
            if (i != 0)
                buf.append(", ");
            buf.append(param[i].str());
        }
        buf.append('>');
        return buf.toString();
    }
}

