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

public class Node {
    private int line;
    private int col;
    String kind;

    String str() {
        return toString();
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public int getCol() {
        return col;
    }

    public void setCol(int col) {
        this.col = col;
    }

    Node pos(int line, int col) {
        this.line = line;
        this.col = col;
        return this;
    }

    public String toString() {
        char[] s = (char[]) Parser.currentSrc.get();
        if (s == null)
            return getClass().getName();
        int p = 0, l = line;
        if (--l > 0)
            while (p < s.length && (s[p++] != '\n' || --l > 0));
        p += col - 1;
        if (p < 0)
            p = 0;
        int e = p;
        char c;
        while (++e < s.length && ((c = s[e]) > ' ' && c != ':' &&
                 c != ';' && c != '.' && c != ',' && c != '(' && c != ')' &&
                 c != '[' && c != ']' && c != '{' && c != '}'));
        return '`' + new String(s, p, Math.min(e, s.length) - p) + '\''; 
    }

    String sym() {
        throw new ParseException(this,
            "Expected symbol here, not " + this);
    }
}