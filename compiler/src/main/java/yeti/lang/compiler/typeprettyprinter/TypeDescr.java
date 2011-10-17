package yeti.lang.compiler.typeprettyprinter;

import yeti.lang.AList;
import yeti.lang.Core;
import yeti.lang.LList;
import yeti.lang.MList;
import yeti.lang.compiler.YetiC;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

class TypeDescr extends YetiType {
    private int type;
    private String name;
    private yeti.lang.compiler.TypeDescr value;
    private yeti.lang.compiler.TypeDescr prev;
    private String alias;
    private Map properties;

    TypeDescr(String name_) {
        name = name_;
    }

    Tag force() {
        if (type == 0)
            return new Tag(name, "Simple");
        AList l = null;
        for (yeti.lang.compiler.TypeDescr i = value; i != null; i = i.prev)
            if (i.properties != null) {
                i.properties.put("type", i.force());
                l = new LList(new GenericStruct(i.properties), l);
            } else {
                l = new LList(i.force(), l);
            }
        Object val = l;
        String tag = null;
        switch (type) {
        case FUN:
            tag = "Function"; break;
        case MAP:
            val = YetiC.pair("params", l, "type", name);
            tag = "Parametric"; break;
        case STRUCT:
            tag = "Struct"; break;
        case VARIANT:
            tag = "Variant"; break;
        }
        Tag res = new Tag(val, tag);
        if (alias == null)
            return res;
        return new Tag(YetiC.pair("alias", alias, "type", res), "Alias");
    }

    static Tag yetiType(YType t, yeti.lang.compiler.TypePattern defs) {
        return prepare(t, defs, new HashMap(), new HashMap()).force();
    }

    static Tag typeDef(YType[] def, MList param, yeti.lang.compiler.TypePattern defs) {
        Map vars = new HashMap();
        for (int i = 0, n = 0; i < def.length - 1; ++i) {
            String name = def[i].doc instanceof String
                ? (String) def[i].doc : "t" + ++n;
            vars.put(def[i].deref(), name);
            param.add(name);
        }
        return prepare(def[def.length - 1], defs, vars, new HashMap()).force();
    }

    private static void hdescr(yeti.lang.compiler.TypeDescr descr, YType tt,
                               yeti.lang.compiler.TypePattern defs, Map vars, Map refs) {
        Map m = new java.util.TreeMap();
        if (tt.partialMembers != null)
            m.putAll(tt.partialMembers);
        if (tt.finalMembers != null) {
            Iterator i = tt.finalMembers.entrySet().iterator();
            while (i.hasNext()) {
                Map.Entry e = (Map.Entry) i.next();
                YType t = (YType) e.getValue();
                Object v = m.put(e.getKey(), t);
                if (v != null && t.doc == null)
                    t.doc = v;
            }
        }
        for (Iterator i = m.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry e = (Map.Entry) i.next();
            Object name = e.getKey();
            YType t = (YType) e.getValue();
            Map it = new IdentityHashMap(5);
            String doc = t.doc();
            it.put("name", name);
            it.put("description", doc == null ? Core.UNDEF_STR : doc);
            it.put("mutable", Boolean.valueOf(t.field == FIELD_MUTABLE));
            it.put("tag",
                tt.finalMembers == null || !tt.finalMembers.containsKey(name)
                    ? "." :
                tt.partialMembers != null && tt.partialMembers.containsKey(name)
                    ? "`" : "");
            yeti.lang.compiler.TypeDescr field = prepare(t, defs, vars, refs);
            field.properties = it;
            field.prev = descr.value;
            descr.value = field;
        }
    }

    private static String getVarName(YType t, Map vars) {
        String v = (String) vars.get(t);
        if (v == null) {
            // 26^7 > 2^32, should be enough ;)
            char[] buf = new char[10];
            int p = buf.length;
            if ((t.flags & FL_ERROR_IS_HERE) != 0)
                buf[--p] = '*';
            int n = vars.size() + 1;
            while (n > 26) {
                buf[--p] = (char) ('a' + n % 26);
                n /= 26;
            }
            buf[--p] = (char) (96 + n);
            if ((t.flags & FL_TAINTED_VAR) != 0)
                buf[--p] = '_';
            buf[--p] = (t.flags & FL_ORDERED_REQUIRED) == 0 ? '\'' : '^';
            v = new String(buf, p, buf.length - p);
            vars.put(t, v);
        }
        return v;
    }

    private static yeti.lang.compiler.TypeDescr prepare(YType t, yeti.lang.compiler.TypePattern defs,
                                     Map vars, Map refs) {
        final int type = t.type;
        if (type == VAR) {
            if (t.ref != null)
                return prepare(t.ref, defs, vars, refs);
            return new yeti.lang.compiler.TypeDescr(getVarName(t, vars));
        }
        if (type < PRIMITIVES.length)
            return new yeti.lang.compiler.TypeDescr(TYPE_NAMES[type]);
        if (type == JAVA)
            return new yeti.lang.compiler.TypeDescr(t.javaType.str());
        if (type == JAVA_ARRAY)
            return new yeti.lang.compiler.TypeDescr(prepare(t.param[0], defs, vars, refs)
                                    .name.concat("[]"));
        yeti.lang.compiler.TypeDescr descr = (yeti.lang.compiler.TypeDescr) refs.get(t), item;
        if (descr != null) {
            if (descr.alias == null)
                descr.alias = getVarName(t, vars);
            return new yeti.lang.compiler.TypeDescr(descr.alias);
        }
        refs.put(t, descr = new yeti.lang.compiler.TypeDescr(null));
        Map defVars = null;
        yeti.lang.compiler.TypePattern def = null;
        if (defs != null &&
                (def = defs.match(t, defVars = new IdentityHashMap())) != null
                && def.end != null) {
            descr.name = def.end.typename;
            if (def.end.defvars.length == 0)
                return descr;
            descr.type = MAP; // Parametric
            Map param = new HashMap();
            for (Iterator i = defVars.entrySet().iterator(); i.hasNext();) {
                Map.Entry e = (Map.Entry) i.next();
                param.put(e.getValue(), e.getKey());
            }
            for (int i = def.end.defvars.length; --i >= 0; ) {
                t = (YType) param.get(Integer.valueOf(def.end.defvars[i]));
                item = t != null ? prepare(t, defs, vars, refs)
                                 : new yeti.lang.compiler.TypeDescr("?");
                item.prev = descr.value;
                descr.value = item;
            }
            return descr;
        }
        descr.type = type;
        YType[] param = t.param;
        switch (type) {
            case FUN:
                for (; t.type == FUN; param = t.param) {
                    item = prepare(param[0], defs, vars, refs);
                    item.prev = descr.value;
                    descr.value = item;
                    t = param[1].deref();
                }
                (item = prepare(t, defs, vars, refs)).prev = descr.value;
                descr.value = item;
                break;
            case STRUCT:
            case VARIANT:
                hdescr(descr, t, defs, vars, refs);
                t = t.param[0].deref();
                if ((t.flags & FL_ERROR_IS_HERE) != 0)
                    descr.alias = getVarName(t, vars);
                break;
            case MAP:
                int n = 1;
                YType p1 = param[1].deref();
                YType p2 = param[2].deref();
                if (p2.type == LIST_MARKER) {
                    descr.name = p1.type == NONE ? "list" : p1.type == NUM
                                    ? "array" : "list?";
                } else {
                    descr.name = p2.type == MAP_MARKER || p1.type != NUM
                                    && p1.type != VAR ? "hash" : "map";
                    n = 2;
                }
                while (--n >= 0) {
                    item = prepare(param[n], defs, vars, refs);
                    item.prev = descr.value;
                    descr.value = item;
                }
                break;
            default:
                descr.name = "?" + type + '?';
                break;
        }
        return descr;
    }
}
