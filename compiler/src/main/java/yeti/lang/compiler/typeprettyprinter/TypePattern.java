package yeti.lang.compiler.typeprettyprinter;

import yeti.lang.compiler.yeti.type.Scope;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

import java.util.*;

class TypePattern {
    // Integer.MIN_VALUE is type end marker
    // Integer.MAX_VALUE matches any type
    private int[] idx;
    private TypePattern[] next;
    // struct/variant field match, next[idx.length] when no such field
    private String field;
    private boolean mutable;
    int var; // if var < 0 then match stores type in typeVars as var
    TypeWalk end; // end result

    TypePattern(int var) {
        this.var = var;
    }

    TypePattern match(YType type, Map typeVars) {
        int i;

        type = type.deref();
        Object tv = typeVars.get(type);
        if (tv != null) {
            i = Arrays.binarySearch(idx, ((Integer) tv).intValue());
            if (i >= 0)
                return next[i];
        }
        i = Arrays.binarySearch(idx, type.type);
        if (i < 0) {
            if (idx[i = idx.length - 1] != Integer.MAX_VALUE)
                return null;
            if (var < 0)
                typeVars.put(type, Integer.valueOf(var));
            return next[i];
        }
        if (var < 0)
            typeVars.put(type, Integer.valueOf(var));
        TypePattern pat = next[i];
        if (pat.field == null) {
            YType[] param = type.getParam();
            if (param != null)
                for (i = 0; i < param.length && pat != null; ++i)
                    pat = pat.match(param[i], typeVars);
        } else {
            // TODO check final/partial if necessary
            Map m = type.getFinalMembers();
            if (m == null)
                m = type.partialMembers;
            i = m.size();
            while (--i >= 0 && pat != null) {
                if (pat.field == null)
                    return null;
                type = (YType) m.get(pat.field);
                if (type != null &&
                        type.getField() == YetiType.FIELD_MUTABLE == mutable) {
                    pat = pat.match(type, typeVars);
                } else {
                    pat = pat.next[pat.idx.length];
                    ++i; // was not matched
                }
            }
        }
        // go for type end marker
        if (pat != null && pat.idx[0] == Integer.MIN_VALUE)
            return pat.next[0];
        return null;
    }

    static TypePattern toPattern(Map typedefs) {
        int j = 0, varAlloc = 1;
        TypePattern presult = new TypePattern(varAlloc);
        TypeWalk[] w = new TypeWalk[typedefs.size()];
        Map tvars = new IdentityHashMap();
        for (Iterator i = typedefs.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            YType[] def = (YType[]) e.getValue();
            YType t = def[def.length - 1].deref();
            if (t.getType() < YetiType.PRIMITIVES.length)
                continue;
            for (int k = def.length - 1; --k >= 0; )
                tvars.put(def[k].deref(), null); // mark as param
            w[j] = new TypeWalk(t, null, tvars, presult);
            w[j].typename = (String) e.getKey();
            w[j++].def = def;
        }
        if (j == 0)
            return null;
        TypeWalk[] wg = new TypeWalk[j];
        System.arraycopy(w, 0, wg, 0, j);
        int[] ids = new int[j];
        TypePattern[] patterns = new TypePattern[j];
        List walkers = new ArrayList();
        walkers.add(wg); // types
        walkers.add(presult); // resulting pattern
        walkers.add(tvars);
        while (walkers.size() > 0) {
            List current = walkers;
            walkers = new ArrayList();
            for (int i = 0, cnt = current.size(); i < cnt; i += 3) {
                w = (TypeWalk[]) current.get(i);
                Arrays.sort(w);
                // group by different types
                // next - target for group in next cycle
                TypePattern next = new TypePattern(++varAlloc),
                    target = (TypePattern) current.get(i + 1);
                String field = w.length != 0 ? w[0].field : null;
                int start = 0, n = 0, e;
                for (j = 1; j <= w.length; ++j) {
                    if (j < w.length && w[j].id == w[j - 1].id &&
                            (field == w[j].field || field.equals(w[j].field)))
                        continue; // skip until same
                    // add branch
                    tvars = new IdentityHashMap((Map) current.get(i + 2));
                    ids[n] = w[j - 1].id;
                    for (int k = e = start; k < j; ++k)
                        if ((w[e] = w[k].next(tvars, next)) != null)
                            ++e;
                    wg = new TypeWalk[e - start];
                    System.arraycopy(w, start, wg, 0, wg.length);
                    walkers.add(wg);
                    walkers.add(patterns[n++] = next);
                    walkers.add(tvars);
                    next = new TypePattern(++varAlloc);
                    start = j;
                    if (j < w.length &&
                            (field == w[j].field || field.equals(w[j].field)))
                        continue; // continue same pattern
                    target.idx = new int[n];
                    System.arraycopy(ids, 0, target.idx, 0, n);
                    if (field != null) {
                        if (field.charAt(0) == ';') {
                            field = field.substring(1).intern();
                            target.mutable = true;
                        }
                        target.field = field;
                        target.next = new TypePattern[n + 1];
                        System.arraycopy(patterns, 0, target.next, 0, n);
                        if (j < w.length) {
                            field = w[j].field;
                            target.next[n] = next;
                            target = next;
                            next = new TypePattern(++varAlloc);
                        }
                    } else {
                        target.next = new TypePattern[n];
                        System.arraycopy(patterns, 0, target.next, 0, n);
                    }
                    n = 0;
                }
            }
        }
        return presult;
    }

    static TypePattern toPattern(Scope scope) {
        Map typedefs = new HashMap();
        for (; scope != null; scope = scope.outer)
            if (scope.typeDef != null) {
                Object old = typedefs.put(scope.name, scope.typeDef);
                if (old != null)
                    typedefs.put(scope.name, old);
            }
        return toPattern(typedefs);
    }
/*
    public String toString() {
        StringBuffer sb = new StringBuffer();
        if (var < 0)
            sb.append(var).append(':');
        if (field != null)
            sb.append(field).append(' ');
        sb.append('{');
        if (next == null)
            sb.append('-');
        else for (int i = 0; i < next.length; ++i) {
            if (i != 0)
                sb.append(", ");
            if (i >= idx.length) {
                sb.append('!');
            } else if (idx[i] == Integer.MIN_VALUE) {
                sb.append('.');
            } else if (idx[i] == Integer.MAX_VALUE) {
                sb.append('_');
            } else {
                sb.append(idx[i]);
            }
            sb.append(" => ").append(next[i]);
        }
        sb.append('}');
        if (end != null) {
            sb.append(':').append(end.typename).append('<');
            for (int i = 0; i < end.defvars.length; ++i) {
                if (i != 0)
                    sb.append(',');
                sb.append(end.defvars[i]);
            }
            sb.append('>');
        }
        return sb.toString();
    }

    static String showres(TypePattern res, Map vars) {
        if (res == null)
            return "FAIL";
        Map rvars = new HashMap();
        for (Iterator i = vars.entrySet().iterator(); i.hasNext();) {
            Map.Entry e = (Map.Entry) i.next();
            rvars.put(e.getValue(), e.getKey());
        }
        StringBuffer r = new StringBuffer(res.end.typename).append('<');
        for (int i = 0; i < res.end.defvars.length; ++i) {
            if (i != 0)
                r.append(", ");
            YType t = (YType) rvars.get(Integer.valueOf(res.end.defvars[i]));
            if (t != null)
                r.append(t);
            else
                r.append("t" + i);
        }
        return r + ">";
    }

    public static void main(String[] _) {
        YType st = new YType(YetiType.STRUCT, null);
        st.finalMembers = new HashMap();
        st.finalMembers.put("close", YetiType.fun(YetiType.UNIT_TYPE, YetiType.UNIT_TYPE));
        st.finalMembers.put("read", YetiType.fun(YetiType.A, YetiType.STR_TYPE));
        YType st2 = new YType(YetiType.STRUCT, null);
        st2.finalMembers = new HashMap();
        st2.finalMembers.put("close", YetiType.fun(YetiType.UNIT_TYPE, YetiType.UNIT_TYPE));
        st2.finalMembers.put("write", YetiType.fun(YetiType.STR_TYPE, YetiType.UNIT_TYPE));
        //YType[] types = {YetiType.CONS_TYPE, st};
        Map defs = new HashMap();
        defs.put("cons", new YType[] { YetiType.A, YetiType.CONS_TYPE });
        defs.put("str_pred", new YType[] { YetiType.STR2_PRED_TYPE });
        defs.put("str_array", new YType[] { YetiType.STRING_ARRAY });
        defs.put("my_struct", new YType[] { YetiType.A, st });
        defs.put("a_struct", new YType[] { st2 });
        TypePattern res, pat = toPattern(defs);
        System.err.println(pat);
        for (Iterator i = defs.values().iterator(); i.hasNext(); ) {
            YType[] def = (YType[]) i.next();
            YType t = def[def.length - 1];
            Map vars = new IdentityHashMap();
            System.out.println(t + " " + showres(pat.match(t, vars), vars));
        }
        YType intlist = new YType(YetiType.MAP, new YType[] {
            YetiType.NUM_TYPE, YetiType.NO_TYPE, YetiType.LIST_TYPE });
        YType il2il = YetiType.fun2Arg(YetiType.NUM_TYPE, intlist, intlist);
        Map vars = new IdentityHashMap();
        res = pat.match(il2il, vars);
        System.out.println(il2il + " " + showres(pat.match(il2il, vars), vars));
    }*/
}
