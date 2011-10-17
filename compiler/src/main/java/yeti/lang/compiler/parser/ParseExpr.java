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

import yeti.lang.compiler.parser.ParseException;

final class ParseExpr {
    private boolean lastOp = true;
    private BinOp root = new BinOp(null, -1, false);
    private BinOp cur = root;

    private void apply(Node node) {
        BinOp apply = new BinOp("", 2, true);
        apply.line = node.line;
        apply.col = node.col;
        addOp(apply);
    }

    private void addOp(BinOp op) {
        BinOp to = cur;
        if (op.op == "-" && lastOp || op.op == "\\"
                || op.op == "throw" || op.op == "not") {
            if (!lastOp) {
                apply(op);
                to = cur;
            }
            if (op.op == "-") {
                op.prio = 1;
            }
            to.left = to.right;
        } else if (lastOp) {
            throw new ParseException(op, "Do not stack operators");
        } else {
            while (to.parent != null && (to.postfix || to.prio < op.prio ||
                        to.prio == op.prio && op.toRight)) {
                to = to.parent;
            }
            op.right = to.right;
        }
        op.parent = to;
        to.right = op;
        cur = op;
        lastOp = !op.postfix;
    }

    void add(Node node) {
        if (node instanceof BinOp && ((BinOp) node).parent == null) {
            addOp((BinOp) node);
        } else {
            if (!lastOp) {
                apply(node);
            }
            lastOp = false;
            cur.left = cur.right;
            cur.right = node;
        }
    }

    Node result() {
        if (cur.left == null && cur.prio != -1 && cur.prio != 1 &&
                cur.prio != Parser.NOT_OP_LEVEL &&
                !cur.postfix || cur.right == null)
            throw new ParseException(cur, "Expecting some value");
        return root.right;
    }
}

