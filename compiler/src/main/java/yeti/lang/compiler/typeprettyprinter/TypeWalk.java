// ex: set sts=4 sw=4 expandtab:

/**
 * Yeti type pretty-printer.
 *
 * Copyright (c) 2010 Madis Janson
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

package yeti.lang.compiler.typeprettyprinter;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import java.util.Arrays;
import java.util.Map;

class TypeWalk implements Comparable {
    int id;
    private YType type;
    private int st;
    private TypeWalk parent;
    private String[] fields;
    private Map fieldMap;
    String field;
    TypePattern pattern;
    String typename;
    YType[] def;
    int[] defvars;

    TypeWalk(YType t, TypeWalk parent, Map tvars, TypePattern p) {
        pattern = p;
        this.parent = parent;
        type = t = t.deref();
        TypePattern tvar = (TypePattern) tvars.get(t);
        if (tvar != null) {
            id = tvar.var;
            if (id > 0)
                tvar.var = id = -id; // mark used
            return;
        }
        id = t.getType();
        if (id == YetiType.VAR) {
            if (tvars.containsKey(t)) {
                id = Integer.MAX_VALUE; // typedef parameter - match anything
                if (p != null && p.var >= 0)
                    p.var = -p.var; // parameters must be saved
            } else if (parent != null && parent.type.getType() == YetiType.MAP &&
                       parent.st > 1 && (parent.st > 2 ||
                            parent.type.getParam()[2] == YetiType.LIST_TYPE)) {
                id = Integer.MAX_VALUE; // map kind - match anything
                return; // and don't associate
            }
            tvars.put(t, p);
        } else if (id >= YetiType.PRIMITIVES.length) {
            tvars.put(t, p);
        }
        if (id == YetiType.STRUCT || id == YetiType.VARIANT) {
            fieldMap = t.getFinalMembers() != null ? t.getFinalMembers()
                                              : t.getPartialMembers();
            fields = (String[])
                fieldMap.keySet().toArray(new String[fieldMap.size()]);
            Arrays.sort(fields);
        }
    }

    TypeWalk next(Map tvars, TypePattern pattern) {
        if (id < 0 || id == Integer.MAX_VALUE) {
            if (parent != null)
                return parent.next(tvars, pattern);
            if (def != null) {
                pattern.end = this;
                defvars = new int[def.length - 1];
                for (int i = 0; i < defvars.length; ++i)
                    if ((pattern = (TypePattern) tvars.get(def[i])) != null)
                        defvars[i] = pattern.var;
            }
            return null;
        }
        if (fields == null) {
            if (type.getParam() != null && st < type.getParam().length)
                return new TypeWalk(type.getParam()[st++], this, tvars, pattern);
        } else if (st < fields.length) {
            YType t = (YType) fieldMap.get(fields[st]);
            TypeWalk res = new TypeWalk(t, this, tvars, pattern);
            res.field = fields[st++];
            if (t.getField() == YetiType.FIELD_MUTABLE)
                res.field = ";".concat(res.field);
            return res;
        }
        field = null;
        id = Integer.MIN_VALUE;
        return this;
    }

    public int compareTo(Object o) {
        TypeWalk tw = (TypeWalk) o;
        if (field == null)
            return tw.field == null ? id - tw.id : 1;
        if (tw.field == null)
            return -1;
        int cmp = field.compareTo(tw.field);
        return cmp == 0 ? id - tw.id : cmp;
    }
}

