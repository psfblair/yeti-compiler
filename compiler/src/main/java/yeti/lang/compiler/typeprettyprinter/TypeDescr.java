package yeti.lang.compiler.typeprettyprinter;

import yeti.lang.*;
import yeti.lang.compiler.YetiC;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;

public class TypeDescr extends YetiType {
    private int type;
    private String name;
    private TypeDescr value;
    private TypeDescr prev;
    private String alias;
    private Map properties;

    TypeDescr(String name_) {
        name = name_;
    }

    Tag force() {
        if (type == 0)
            return new Tag(name, "Simple");
        AList l = null;
        for (TypeDescr i = value; i != null; i = i.prev)
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

    public static Tag yetiType(YType t, TypePattern defs) {
        return prepare(t, defs, new HashMap(), new HashMap()).force();
    }

    static Tag typeDef(YType[] def, MList param, TypePattern defs) {
        Map vars = new HashMap();
        for (int i = 0, n = 0; i < def.length - 1; ++i) {
            String name = def[i].doc instanceof String
                ? (String) def[i].doc : "t" + ++n;
            vars.put(def[i].deref(), name);
            param.add(name);
        }
        return prepare(def[def.length - 1], defs, vars, new HashMap()).force();
    }

    private static void hdescr(TypeDescr descr, YType tt,
                               TypePattern defs, Map vars, Map refs) {
        Map m = new java.util.TreeMap();
        if (tt.getPartialMembers() != null)
            m.putAll(tt.getPartialMembers());
        if (tt.getFinalMembers() != null) {
            Iterator i = tt.getFinalMembers().entrySet().iterator();
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
            it.put("mutable", Boolean.valueOf(t.getField() == FIELD_MUTABLE));
            it.put("tag",
                tt.getFinalMembers() == null || !tt.getFinalMembers().containsKey(name)
                    ? "." :
                tt.getPartialMembers() != null && tt.getPartialMembers().containsKey(name)
                    ? "`" : "");
            TypeDescr field = prepare(t, defs, vars, refs);
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

    private static TypeDescr prepare(YType t, TypePattern defs,
                                     Map vars, Map refs) {
        final int type = t.getType();
        if (type == VAR) {
            if (t.getRef() != null)
                return prepare(t.getRef(), defs, vars, refs);
            return new TypeDescr(getVarName(t, vars));
        }
        if (type < PRIMITIVES.length)
            return new TypeDescr(TYPE_NAMES[type]);
        if (type == JAVA)
            return new TypeDescr(t.getJavaType().str());
        if (type == JAVA_ARRAY)
            return new TypeDescr(prepare(t.getParam()[0], defs, vars, refs)
                                    .name.concat("[]"));
        TypeDescr descr = (TypeDescr) refs.get(t), item;
        if (descr != null) {
            if (descr.alias == null)
                descr.alias = getVarName(t, vars);
            return new TypeDescr(descr.alias);
        }
        refs.put(t, descr = new TypeDescr(null));
        Map defVars = null;
        TypePattern def = null;
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
                                 : new TypeDescr("?");
                item.prev = descr.value;
                descr.value = item;
            }
            return descr;
        }
        descr.type = type;
        YType[] param = t.getParam();
        switch (type) {
            case FUN:
                for (; t.getType() == FUN; param = t.getParam()) {
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
                t = t.getParam()[0].deref();
                if ((t.flags & FL_ERROR_IS_HERE) != 0)
                    descr.alias = getVarName(t, vars);
                break;
            case MAP:
                int n = 1;
                YType p1 = param[1].deref();
                YType p2 = param[2].deref();
                if (p2.getType() == LIST_MARKER) {
                    descr.name = p1.getType() == NONE ? "list" : p1.getType() == NUM
                                    ? "array" : "list?";
                } else {
                    descr.name = p2.getType() == MAP_MARKER || p1.getType() != NUM
                                    && p1.getType() != VAR ? "hash" : "map";
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
