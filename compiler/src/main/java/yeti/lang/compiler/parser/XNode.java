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

public class XNode extends Node {
    Node[] expr;

    XNode(String kind) {
        this.kind = kind;
    }

    XNode(String kind, Node[] expr) {
        this.kind = kind;
        this.expr = expr;
    }
    
    XNode(String kind, Node expr) {
        this.kind = kind;
        this.expr = new Node[] { expr };
        setLine(expr.getLine());
        setCol(expr.getCol());
    }

    public Node[] getExpr() {
        return expr;
    }

    String str() {
        if (expr == null)
            return "`".concat(kind);
        StringBuffer buf = new StringBuffer("(`");
        buf.append(kind);
        for (int i = 0; i < expr.length; ++i) {
            buf.append(' ');
            buf.append(expr[i].str());
        }
        buf.append(')');
        return buf.toString();
    }

    static XNode struct(Node[] fields) {
        for (int i = 0; i < fields.length; ++i) {
            IsOp op = null;
            Sym s = null;
            if (fields[i] instanceof Sym) {
                s = (Sym) fields[i];
            } else if (fields[i] instanceof IsOp) {
                op = (IsOp) fields[i];
                op.right.sym();
                s = (Sym) op.right;
            }
            if (s != null) {
                Bind bind = new Bind();
                bind.name = s.sym;
                bind.expr = s;
                bind.setCol(s.getCol());
                bind.setLine(s.getLine());
                bind.noRec = true;
                if (op != null)
                    bind.type = op.type;
                fields[i] = bind;
            }
        }
        return new XNode("struct", fields);
    }

    static XNode lambda(Node arg, Node expr, Node name) {
        XNode lambda = new XNode("lambda", name == null
            ? new Node[] { arg, expr } : new Node[] { arg, expr, name });
        lambda.setLine(arg.getLine());
        lambda.setCol(arg.getCol());
        return lambda;
    }
}
