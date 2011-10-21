// ex: set sts=4 sw=4 expandtab:

/**
 * Yeti type analyzer.
 * Uses Hindley-Milner type inference algorithm
 * with extensions for polymorphic structs and variants.
 * Copyright (c) 2007,2008.2009 Madis Janson
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

package yeti.lang.compiler.yeti.type;

import yeti.lang.compiler.CompileException;
import yeti.lang.compiler.YetiC;
import yeti.lang.compiler.parser.YetiParser;

import java.util.*;

public class YetiType implements YetiParser {
    public static final int VAR  = 0;
    public static final int UNIT = 1;
    public static final int STR  = 2;
    public static final int NUM  = 3;
    public static final int BOOL = 4;
    public static final int CHAR = 5;
    public static final int NONE = 6;
    public static final int LIST_MARKER = 7;
    public static final int MAP_MARKER  = 8;
    public static final int FUN  = 9; // a -> b
    public static final int MAP  = 10; // value, index, (LIST | MAP)
    public static final int STRUCT = 11;
    public static final int VARIANT = 12;
    public static final int JAVA = 13;
    public static final int JAVA_ARRAY = 14;

    public static final int FL_ORDERED_REQUIRED = 1;
    public static final int FL_TAINTED_VAR = 2;
    protected static final int FL_ERROR_IS_HERE = 0x100;
    static final int FL_ANY_PATTERN = 0x4000;
    static final int FL_PARTIAL_PATTERN  = 0x8000;

    static final int FIELD_NON_POLYMORPHIC = 1;
    public static final int FIELD_MUTABLE = 2;

    public static final YType[] NO_PARAM = {};
    public static final YType UNIT_TYPE = new YType(UNIT, NO_PARAM);
    public static final YType NUM_TYPE  = new YType(NUM,  NO_PARAM);
    public static final YType STR_TYPE  = new YType(STR,  NO_PARAM);
    public static final YType BOOL_TYPE = new YType(BOOL, NO_PARAM);
    static final YType CHAR_TYPE = new YType(CHAR, NO_PARAM);
    public static final YType NO_TYPE   = new YType(NONE, NO_PARAM);
    public static final YType LIST_TYPE = new YType(LIST_MARKER, NO_PARAM);
    static final YType MAP_TYPE  = new YType(MAP_MARKER, NO_PARAM);
    static final YType ORDERED = orderedVar(1);
    static final YType A = new YType(1);
    static final YType B = new YType(1);
    static final YType C = new YType(1);
    static final YType EQ_TYPE = fun2Arg(A, A, BOOL_TYPE);
    static final YType LG_TYPE = fun2Arg(ORDERED, ORDERED, BOOL_TYPE);
    static final YType NUMOP_TYPE = fun2Arg(NUM_TYPE, NUM_TYPE, NUM_TYPE);
    static final YType BOOLOP_TYPE = fun2Arg(BOOL_TYPE, BOOL_TYPE, BOOL_TYPE);
    static final YType A_B_LIST_TYPE =
        new YType(MAP, new YType[] { A, B, LIST_TYPE });
    static final YType NUM_LIST_TYPE =
        new YType(MAP, new YType[] { NUM_TYPE, B, LIST_TYPE });
    static final YType A_B_MAP_TYPE =
        new YType(MAP, new YType[] { B, A, MAP_TYPE });
    static final YType A_B_C_MAP_TYPE =
        new YType(MAP, new YType[] { B, A, C });
    static final YType A_LIST_TYPE =
        new YType(MAP, new YType[] { A, NO_TYPE, LIST_TYPE });
    static final YType C_LIST_TYPE =
        new YType(MAP, new YType[] { C, NO_TYPE, LIST_TYPE });
    static final YType STRING_ARRAY =
        new YType(MAP, new YType[] { STR_TYPE, NUM_TYPE, LIST_TYPE });
    static final YType CONS_TYPE = fun2Arg(A, A_B_LIST_TYPE, A_LIST_TYPE);
    static final YType LAZYCONS_TYPE =
        fun2Arg(A, fun(UNIT_TYPE, A_B_LIST_TYPE), A_LIST_TYPE);
    static final YType A_TO_BOOL = fun(A, BOOL_TYPE);
    static final YType LIST_TO_A = fun(A_B_LIST_TYPE, A);
    static final YType MAP_TO_BOOL = fun(A_B_C_MAP_TYPE, BOOL_TYPE);
    static final YType LIST_TO_LIST = fun(A_B_LIST_TYPE, A_LIST_TYPE);
    static final YType IN_TYPE = fun2Arg(A, A_B_C_MAP_TYPE, BOOL_TYPE);
    static final YType COMPOSE_TYPE = fun2Arg(fun(B, C), fun(A, B), fun(A, C));
    static final YType BOOL_TO_BOOL = fun(BOOL_TYPE, BOOL_TYPE);
    static final YType NUM_TO_NUM = fun(NUM_TYPE, NUM_TYPE);
    static final YType STR_TO_NUM_TO_STR =
        fun2Arg(STR_TYPE, NUM_TYPE, STR_TYPE);
    static final YType FOR_TYPE =
        fun2Arg(A_B_LIST_TYPE, fun(A, UNIT_TYPE), UNIT_TYPE);
    static final YType STR2_PRED_TYPE = fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE);
    public static final YType SYNCHRONIZED_TYPE = fun2Arg(A, fun(UNIT_TYPE, B), B);
    static final YType CLASS_TYPE = new YType("Ljava/lang/Class;");
    public static final YType OBJECT_TYPE = new YType("Ljava/lang/Object;");
    static final YType WITH_EXIT_TYPE = fun(fun(fun(A, B), A), A);

    public static final YType[] PRIMITIVES =
        { null, UNIT_TYPE, STR_TYPE, NUM_TYPE, BOOL_TYPE, CHAR_TYPE,
          NO_TYPE, LIST_TYPE, MAP_TYPE };

    protected static final String[] TYPE_NAMES =
        { "var", "()", "string", "number", "boolean", "char",
          "none", "list", "hash", "fun", "list", "struct", "variant",
          "object" };

    static final Scope ROOT_SCOPE =
        bindCompare("==", EQ_TYPE, CompareFun.COND_EQ, // equals is 0 for false
        bindCompare("!=", EQ_TYPE, CompareFun.COND_NOT, // equals is 0 for false
        bindCompare("<" , LG_TYPE, CompareFun.COND_LT,
        bindCompare("<=", LG_TYPE, CompareFun.COND_LE,
        bindCompare(">" , LG_TYPE, CompareFun.COND_GT,
        bindCompare(">=", LG_TYPE, CompareFun.COND_GE,
        bindScope("_argv", new BuiltIn(1),
        bindScope("randomInt", new BuiltIn(22),
        bindPoly(".", COMPOSE_TYPE, new BuiltIn(6),
        bindPoly("in", IN_TYPE, new BuiltIn(2),
        bindPoly("::", CONS_TYPE, new BuiltIn(3),
        bindPoly(":.", LAZYCONS_TYPE, new BuiltIn(4),
        bindPoly("for", FOR_TYPE, new BuiltIn(5),
        bindPoly("nullptr?", A_TO_BOOL, new BuiltIn(8),
        bindPoly("defined?", A_TO_BOOL, new BuiltIn(9),
        bindPoly("empty?", MAP_TO_BOOL, new BuiltIn(10),
        bindPoly("same?", EQ_TYPE, new BuiltIn(21),
        bindPoly("head", LIST_TO_A, new BuiltIn(11),
        bindPoly("tail", LIST_TO_LIST, new BuiltIn(12),
        bindPoly("synchronized", SYNCHRONIZED_TYPE, new BuiltIn(7),
        bindPoly("withExit", WITH_EXIT_TYPE, new BuiltIn(24),
        bindArith("+", "add", bindArith("-", "sub",
        bindArith("*", "mul", bindArith("/", "div",
        bindArith("%", "rem", bindArith("div", "intDiv",
        bindArith("shl", "shl", bindArith("shr", "shr",
        bindArith("b_and", "and", bindArith("b_or", "or",
        bindArith("xor", "xor",
        bindScope("=~", new BuiltIn(13),
        bindScope("!~", new BuiltIn(14),
        bindScope("not", new BuiltIn(15),
        bindScope("and", new BoolOpFun(false),
        bindScope("or", new BoolOpFun(true),
        bindScope("undef_bool", new BuiltIn(17),
        bindScope("false", new BuiltIn(18),
        bindScope("true", new BuiltIn(19),
        bindScope("negate", new BuiltIn(20),
        bindScope("undef_str", new BuiltIn(23),
        bindScope("strChar", new BuiltIn(16),
        bindStr("strLength", fun(STR_TYPE, NUM_TYPE), "length", "()I",
        bindStr("strUpper", fun(STR_TYPE, STR_TYPE), "toUpperCase",
                "()Ljava/lang/String;",
        bindStr("strLower", fun(STR_TYPE, STR_TYPE), "toLowerCase",
                "()Ljava/lang/String;",
        bindStr("strTrim", fun(STR_TYPE, STR_TYPE), "trim",
                "()Ljava/lang/String;",
        bindStr("strSlice",
                fun2Arg(STR_TYPE, NUM_TYPE, fun(NUM_TYPE, STR_TYPE)),
                "substring", "(II)Ljava/lang/String;",
        bindStr("strRight", fun2Arg(STR_TYPE, NUM_TYPE, STR_TYPE),
                "substring", "(I)Ljava/lang/String;",
        bindStr("strStarts?", fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE),
                "startsWith", "(Ljava/lang/String;)Z",
        bindStr("strEnds?", fun2Arg(STR_TYPE, STR_TYPE, BOOL_TYPE),
                "endsWith", "(Ljava/lang/String;)Z",
        bindStr("strIndexOf",
                fun2Arg(STR_TYPE, STR_TYPE, fun(NUM_TYPE, NUM_TYPE)),
                "indexOf", "(Ljava/lang/String;I)I",
        bindStr("strLastIndexOf",
                fun2Arg(STR_TYPE, STR_TYPE, fun(NUM_TYPE, NUM_TYPE)),
                "lastIndexOf", "(Ljava/lang/String;I)I",
        bindStr("strLastIndexOf'",
                fun2Arg(STR_TYPE, STR_TYPE, NUM_TYPE),
                "lastIndexOf", "(Ljava/lang/String;)I",
        bindRegex("strSplit", "yeti/lang/StrSplit",
                  fun2Arg(STR_TYPE, STR_TYPE, STRING_ARRAY), 
        bindRegex("like", "yeti/lang/Like",
                  fun2Arg(STR_TYPE, STR_TYPE, fun(UNIT_TYPE, STRING_ARRAY)), 
        bindRegex("substAll", "yeti/lang/SubstAll",
                  fun2Arg(STR_TYPE, STR_TYPE, fun(STR_TYPE, STR_TYPE)), 
        bindRegex("matchAll", "yeti/lang/MatchAll",
                  fun2Arg(STR_TYPE, fun(STRING_ARRAY, A),
                  fun2Arg(fun(STR_TYPE, A), STR_TYPE, A_LIST_TYPE)), 
        bindImport("EmptyArray", "yeti/lang/EmptyArrayException",
        bindImport("Failure", "yeti/lang/FailureException",
        bindImport("NoSuchKey", "yeti/lang/NoSuchKeyException",
        bindImport("Exception", "java/lang/Exception",
        bindImport("RuntimeException", "java/lang/RuntimeException",
        bindImport("NumberFormatException", "java/lang/NumberFormatException",
        bindImport("IllegalArgumentException",
                   "java/lang/IllegalArgumentException",
        bindImport("Math", "java/lang/Math",
        bindImport("Object", "java/lang/Object",
        bindImport("Boolean", "java/lang/Boolean",
        bindImport("Integer", "java/lang/Integer",
        bindImport("Long", "java/lang/Long",
        bindImport("Double", "java/lang/Double",
        bindImport("String", "java/lang/String",
   null))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))))));

    static final Scope ROOT_SCOPE_SYS =
        bindImport("System", "java/lang/System",
        bindImport("Class", "java/lang/Class", ROOT_SCOPE));

    static final JavaType COMPARABLE =
        JavaType.fromDescription("Ljava/lang/Comparable;");

    static Scope bindScope(String name, Binder binder, Scope scope) {
        return new Scope(scope, name, binder);
    }

    static Scope bindCompare(String op, YType type, int code, Scope scope) {
        return bindPoly(op, type, new Compare(type, code, op), scope);
    }

    static Scope bindArith(String op, String method, Scope scope) {
        return bindScope(op, new ArithOp(op, method, NUMOP_TYPE), scope);
    }

    static Scope bindStr(String name, YType type, String method, String sig,
                         Scope scope) {
        return bindScope(name, new StrOp(name, method, sig, type), scope);
    }

    static Scope bindRegex(String name, String impl, YType type, Scope scope) {
        return bindPoly(name, type, new Regex(name, impl, type), scope);
    }

    static Scope bindImport(String name, String className, Scope scope) {
        scope = new Scope(scope, name, null);
        scope.importClass = new ClassBinding(new YType('L' + className + ';'));
        return scope;
    }

    static YType fun(YType a, YType res) {
        return new YType(FUN, new YType[] { a, res });
    }

    static YType fun2Arg(YType a, YType b, YType res) {
        return new YType(FUN,
            new YType[] { a, new YType(FUN, new YType[] { b, res }) });
    }

    static YType variantOf(String[] na, YType[] ta) {
        YType t = new YType(VARIANT, ta);
        t.partialMembers = new HashMap(na.length);
        for (int i = 0; i < na.length; ++i) {
            t.partialMembers.put(na[i], ta[i]);
        }
        return t;
    }

    static YType mutableFieldRef(YType src) {
        YType t = new YType(src.depth);
        t.ref = src.ref;
        t.flags = src.flags;
        t.field = FIELD_MUTABLE;
        return t;
    }

    static YType fieldRef(int depth, YType ref, int kind) {
        YType t = new YType(depth);
        t.ref = ref.deref();
        t.field = kind;
        return t;
    }

    public static class ClassBinding {
        final YType type;

        ClassBinding(YType classType) {
            this.type = classType;
        }

        BindRef[] getCaptures() {
            return null;
        }

        // proxies - List<Closure>
        ClassBinding dup(List proxies) {
            return this;
        }
    }

    public static final class ScopeCtx {
        String packageName;
        String className;
    }

    static YType orderedVar(int maxDepth) {
        YType type = new YType(maxDepth);
        type.flags = FL_ORDERED_REQUIRED;
        return type;
    }

    static void limitDepth(YType type, int maxDepth) {
        type = type.deref();
        if (type.type != VAR) {
            if (type.seen) {
                return;
            }
            type.seen = true;
            for (int i = type.param.length; --i >= 0;) {
                limitDepth(type.param[i], maxDepth);
            }
            type.seen = false;
        } else if (type.depth > maxDepth) {
            type.depth = maxDepth;
        }
    }

    static void mismatch(YType a, YType b) throws TypeException {
        throw new TypeException(a, b);
    }

    static void finalizeStruct(YType partial, YType src) throws TypeException {
        if (src.finalMembers == null || partial.partialMembers == null /*||
                partial.finalMembers != null*/) {
            return; // nothing to check
        }
        Object[] members = partial.partialMembers.entrySet().toArray();
        for (int i = 0; i < members.length; ++i) {
            Map.Entry entry = (Map.Entry) members[i];
            YType ff = (YType) src.finalMembers.get(entry.getKey());
            if (ff == null) {
                throw new TypeException(src, " => ",
                       partial, " (member missing: " + entry.getKey() + ")");
            }
            YType partField = (YType) entry.getValue();
            if (partField.field == FIELD_MUTABLE && ff.field != FIELD_MUTABLE) {
                throw new TypeException("Field '" + entry.getKey()
                    + "' constness mismatch: " + src + " => " + partial);
            }
            unify(partField, ff);
        }
    }

    static void unifyMembers(YType a, YType b) throws TypeException {
        YType oldRef = b.ref;
        try {
            b.ref = a; // just fake ref now to avoid cycles...
            Map ff;
            if (((a.flags ^ b.flags) & FL_ORDERED_REQUIRED) != 0) {
                // VARIANT types are sometimes ordered.
                // when all their variant parameters are ordered types.
                if ((a.flags & FL_ORDERED_REQUIRED) != 0) {
                    requireOrdered(b);
                } else {
                    requireOrdered(a);
                }
            }
            if (a.finalMembers == null) {
                ff = b.finalMembers;
            } else if (b.finalMembers == null) {
                ff = a.finalMembers;
            } else {
                // unify final members
                ff = new HashMap(a.finalMembers);
                for (Iterator i = ff.entrySet().iterator(); i.hasNext();) {
                    Map.Entry entry = (Map.Entry) i.next();
                    YType f = (YType) b.finalMembers.get(entry.getKey());
                    if (f != null) {
                        YType t = (YType) entry.getValue();
                        unify(f, t);
                        // constness spreads
                        if (t.field != f.field) {
                            if (t.field == 0) {
                                entry.setValue(t = f);
                            }
                            t.field = FIELD_NON_POLYMORPHIC;
                        }
                    } else {
                        i.remove();
                    }
                }
                if (ff.isEmpty()) {
                    mismatch(a, b);
                }
            }
            finalizeStruct(a, b);
            finalizeStruct(b, a);
            if (a.partialMembers == null) {
                a.partialMembers = b.partialMembers;
            } else if (b.partialMembers != null) {
                // join partial members
                Object[] aa = a.partialMembers.entrySet().toArray();
                for (int i = 0; i < aa.length; ++i) {
                    Map.Entry entry = (Map.Entry) aa[i];
                    YType f = (YType) b.partialMembers.get(entry.getKey());
                    if (f != null) {
                        unify((YType) entry.getValue(), f);
                        // mutability spreads
                        if (f.field >= FIELD_NON_POLYMORPHIC) {
                            entry.setValue(f);
                        }
                    }
                }
                a.partialMembers.putAll(b.partialMembers);
            }
            a.finalMembers = ff;
            if (ff == null) {
                ff = a.partialMembers;
            } else if (a.partialMembers != null) {
                ff = new HashMap(ff);
                ff.putAll(a.partialMembers);
            }
            unify(a.param[0], b.param[0]);
            structParam(a, ff, a.param[0].deref());
            b.type = VAR;
            b.ref = a;
        } catch (TypeException ex) {
            b.ref = oldRef;
            throw ex;
        }
    }

    static void structParam(YType st, Map values, YType depth) {
        if (depth.type != VAR || depth.ref != null)
            throw new IllegalStateException("non-freevar struct depth: " + depth);
        YType[] a = new YType[values.size() + 1];
        a[0] = depth;
        Iterator i = values.values().iterator();
        for (int j = 1; i.hasNext(); ++j) {
            a[j] = (YType) i.next();
        }
        st.param = a;
    }

    static void unifyJava(YType jt, YType t) throws TypeException {
        String descr = jt.javaType.description;
        if (t.type != JAVA) {
            if (t.type == UNIT && descr == "V")
                return;
            mismatch(jt, t);
        }
        if (descr == t.javaType.description) {
            return;
        }
        mismatch(jt, t);
    }

    static void requireOrdered(YType type) throws TypeException {
        switch (type.type) {
            case VARIANT:
                if ((type.flags & FL_ORDERED_REQUIRED) == 0) {
                    if (type.partialMembers != null) {
                        Iterator i = type.partialMembers.values().iterator();
                        while (i.hasNext()) {
                            requireOrdered((YType) i.next());
                        }
                    }
                    if (type.finalMembers != null) {
                        Iterator i = type.finalMembers.values().iterator();
                        while (i.hasNext()) {
                            requireOrdered((YType) i.next());
                        }
                        type.flags |= FL_ORDERED_REQUIRED;
                    }
                }
                return;
            case MAP:
                requireOrdered(type.param[2]);
                requireOrdered(type.param[0]);
                return;
            case VAR:
                if (type.ref != null) {
                    requireOrdered(type.ref);
                } else {
                    type.flags |= FL_ORDERED_REQUIRED;
                }
            case NUM:
            case STR:
            case UNIT:
            case LIST_MARKER:
                return;
            case JAVA:
                try {
                    if (COMPARABLE.isAssignable(type.javaType) >= 0)
                        return;
                } catch (JavaClassNotFoundException ex) {
                    throw new TypeException("Unknown class: " +
                                                ex.getMessage());
                }
        }
        TypeException ex = new TypeException(type + " is not an ordered type");
        ex.special = true;
        throw ex;
    }

    static void occursCheck(YType type, YType var) throws TypeException {
        type = type.deref();
        if (type == var) {
            TypeException ex =
                new TypeException("Cyclic types are not allowed");
            ex.special = true;
            throw ex;
        }
        if (type.param != null && type.type != VARIANT && type.type != STRUCT) {
            for (int i = type.param.length; --i >= 0;) {
                occursCheck(type.param[i], var);
            }
        }
    }

    static void unifyToVar(YType var, YType from) throws TypeException {
        occursCheck(from, var);
        if ((var.flags & FL_ORDERED_REQUIRED) != 0)
            requireOrdered(from);
        if ((var.flags & FL_TAINTED_VAR) != 0)
            from.flags |= FL_TAINTED_VAR;
        
        limitDepth(from, var.depth);
        var.ref = from;
    }

    static void unify(YType a, YType b) throws TypeException {
        a = a.deref();
        b = b.deref();
        if (a == b) {
        } else if (a.type == VAR) {
            unifyToVar(a, b);
        } else if (b.type == VAR) {
            unifyToVar(b, a);
        } else if (a.type == JAVA) {
            unifyJava(a, b);
        } else if (b.type == JAVA) {
            unifyJava(b, a);
        } else if (a.type != b.type) {
            mismatch(a, b);
        } else if (a.type == STRUCT || a.type == VARIANT) {
            unifyMembers(a, b);
        } else {
            for (int i = 0, cnt = a.param.length; i < cnt; ++i)
                unify(a.param[i], b.param[i]);
        }
    }

    static void unify(YType a, YType b, Node where, Scope scope,
                      YType param1, YType param2, String error) {
        try {
            unify(a, b);
        } catch (TypeException ex) {
            throw new CompileException(where, scope, param1, param2, error, ex);
        }
    }

    static void unify(YType a, YType b, Node where, Scope scope, String error) {
        unify(a, b, where, scope, a, b, error);
    }

    static YType mergeOrUnify(YType to, YType val) throws TypeException {
        YType t = JavaType.mergeTypes(to, val);
        if (t != null) {
            return t;
        }
        unify(to, val);
        return to;
    }

    static Map copyTypeMap(Map types, Map free, Map known) {
        Map result = new HashMap(types.size());
        for (Iterator i = types.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            YType t = (YType) entry.getValue();
            YType nt = copyType(t, free, known);
            // looks like a hack, but fixing here avoids unnecessery refs
            if (t.field != nt.field) {
                // don't accidently create new free var (nt.ref == null)
                if (t.field != 0 && nt.ref != null)
                    nt = mutableFieldRef(nt);
                nt.field = t.field;
                nt.flags = t.flags;
            }
            result.put(entry.getKey(), nt);
        }
        return result;
    }

    static YType copyType(YType type_, Map free, Map known) {
        YType type = type_.deref();
        if (type.type == VAR) {
            YType var = (YType) free.get(type);
            return var == null ? type : var;
        }
        if (type.param.length == 0) {
            return type_;
        }
        YType copy = (YType) known.get(type);
        if (copy != null) {
            return copy;
        }
        YType[] param = new YType[type.param.length];
        copy = new YType(type.type, param);
        copy.doc = type_;
        YType res = copy;
        if (type_.field >= FIELD_NON_POLYMORPHIC) {
            res = mutableFieldRef(type_);
            res.field = type_.field;
            res.ref = copy;
        }
        known.put(type, res);
        for (int i = param.length; --i >= 0;) {
            param[i] = copyType(type.param[i], free, known);
        }
        if (type.partialMembers != null) {
            copy.partialMembers = copyTypeMap(type.partialMembers, free, known);
        }
        if (type.finalMembers != null) {
            copy.finalMembers = copyTypeMap(type.finalMembers, free, known);
        }
        return res;
    }

    static Map createFreeVars(YType[] freeTypes, int depth) {
        IdentityHashMap vars = new IdentityHashMap(freeTypes.length);
        for (int i = freeTypes.length; --i >= 0;) {
            YType t = new YType(depth);
            YType free = freeTypes[i];
            t.flags = free.flags;
            t.field = free.field;
            vars.put(free, t);
        }
        return vars;
    }

    private static BindRef resolveRef(String sym, Node where,
                                      Scope scope, Scope[] r) {
        for (; scope != null; scope = scope.outer) {
            if (scope.name == sym && scope.binder != null) {
                r[0] = scope;
                return scope.binder.getRef(where.line);
            }
            if (scope.closure != null) {
                return scope.closure.refProxy(
                            resolveRef(sym, where, scope.outer, r));
            }
        }
        throw new CompileException(where, "Unknown identifier: " + sym);
    }

    static BindRef resolve(String sym, Node where, Scope scope, int depth) {
        Scope[] r = new Scope[1];
        BindRef ref = resolveRef(sym, where, scope, r);
        // We have to copy even polymorph refs with NO free variables,
        // because the goddamn structs are wicked with their
        // provided/requested member lists.
        if (r[0].free != null && (ref.polymorph || r[0].free.length != 0)) {
            ref = ref.unshare();
            Map vars = createFreeVars(r[0].free, depth);
            ref.type = copyType(ref.type, vars, new IdentityHashMap());
        }
        return ref;
    }

    static YType resolveClass(String name, Scope scope, boolean shadow) {
        if (name.indexOf('/') >= 0) {
            return JavaType.typeOfClass(null, name);
        }
        for (; scope != null; scope = scope.outer)
            if (scope.name == name) {
                if (scope.importClass != null)
                    return scope.importClass.type;
                if (shadow)
                    break;
            }
        return null;
    }

    static ClassBinding resolveFullClass(String name, Scope scope,
                                         boolean refs, Node checkPerm) {
        String packageName = scope.ctx.packageName;
        YType t;
        if (name.indexOf('/') >= 0) {
            packageName = null;
        } else if (refs) {
            List proxies = new ArrayList();
            for (Scope s = scope; s != null; s = s.outer) {
                if (s.name == name && s.importClass != null)
                    return s.importClass.dup(proxies);
                if (s.closure != null)
                    proxies.add(s.closure);
            }
        } else if ((t = resolveClass(name, scope, false)) != null) {
            return new ClassBinding(t);
        }
        if (checkPerm != null &&
            (CompileCtx.current().flags & YetiC.CF_NO_IMPORT) != 0)
            throw new CompileException(checkPerm, name + " is not imported");
        return new ClassBinding(JavaType.typeOfClass(packageName, name));
    }

    static YType resolveFullClass(String name, Scope scope) {
        YType t = resolveClass(name, scope, false);
        return t == null ?
                JavaType.typeOfClass(scope.ctx.packageName, name) : t;
    }

    static boolean hasMutableStore(Map bindVars, YType result, boolean store) {
        if (!result.seen) {
            if (result.field >= FIELD_NON_POLYMORPHIC)
                store = true;
            YType t = result.deref();
            if (t.type == VAR) 
                return store && bindVars.containsKey(t);
            if (t.type == MAP && t.param[1] != NO_TYPE)
                store = true;
            result.seen = true;
            for (int i = t.param.length; --i >= 0;)
                if (hasMutableStore(bindVars, t.param[i],
                                    store || i == 0 && t.type == FUN)) {
                    result.seen = false;
                    return true;
                }
            result.seen = false;
        }
        return false;
    }

    // difference from getFreeVar is that functions don't protect
    static void restrictArg(YType type, int depth, boolean active) {
        if (type.seen)
            return;
        if (type.field >= FIELD_NON_POLYMORPHIC)
            active = true; // anything under mutable field is evil
        YType t = type.deref(), k;
        int tt = t.type;
        if (tt != VAR) {
            type.seen = true;
            for (int i = t.param.length; --i >= 0;) {
                if (i == 1 && !active)
                    active = tt == MAP && (k = t.param[1].deref()) != NO_TYPE
                        && (k.type != VAR || t.param[2] != LIST_TYPE);
                // array/hash value is in mutable store and evil
                restrictArg(t.param[i], depth, active);
            }
            type.seen = false;
        } else if (active && t.depth >= depth) {
            t.flags |= FL_TAINTED_VAR;
        }
    }

    private static final int RESTRICT_PROTECT = 1;
    private static final int RESTRICT_CONTRA  = 2;
    static final int RESTRICT_ALL  = 4;
    static final int RESTRICT_POLY = 8;

    static void getFreeVar(List vars, List deny, YType type,
                           int flags, int depth) {
        if (type.seen)
            return;
        if ((flags & RESTRICT_PROTECT) == 0 &&
                type.field >= FIELD_NON_POLYMORPHIC)
            flags |= RESTRICT_ALL;
        YType t = type.deref();
        int tt = t.type;
        if (tt != VAR) {
            if (tt == FUN)
                flags |= RESTRICT_PROTECT;
            type.seen = true;
            for (int i = t.param.length; --i >= 0;) {
                // array/hash value is in mutable store and evil
                if (i == 0 && tt == FUN)
                    flags |= RESTRICT_CONTRA;
                else if (i == 1 && tt == MAP && t.param[1].deref() != NO_TYPE)
                    flags |= (flags & RESTRICT_PROTECT) == 0
                        ? RESTRICT_ALL : RESTRICT_CONTRA;
                getFreeVar(vars, deny, t.param[i], flags, depth);
            }
            type.seen = false;
        } else if (t.depth > depth) {
            if ((flags & RESTRICT_ALL) != 0) {
                t.flags |= FL_TAINTED_VAR;
                vars = deny;
            } else if ((flags & (RESTRICT_CONTRA | RESTRICT_POLY)) ==
                            RESTRICT_CONTRA &&
                       (t.flags & FL_TAINTED_VAR) != 0) {
                vars = deny;
            }
            if (vars.indexOf(t) < 0)
                vars.add(t);
        } else if ((flags & RESTRICT_ALL) != 0 && t.depth == depth) {
            t.flags |= FL_TAINTED_VAR;
        }
    }

    static void removeStructs(YType t, List vars) {
        if (!t.seen) {
            if (t.type != VAR) {
                int i = 0;
                if (t.type == STRUCT || t.type == VARIANT) {
                    vars.remove(t.param[0].deref());
                    i = 1;
                }
                t.seen = true;
                for (; i < t.param.length; ++i) {
                   removeStructs(t.param[i], vars);
                }
                t.seen = false;
            } else if (t.ref != null) {
                removeStructs(t.ref, vars);
            }
        }
    }

    static Scope bind(String name, YType valueType, Binder value,
                      int flags, int depth, Scope scope) {
        List free = new ArrayList(), deny = new ArrayList();
        getFreeVar(free, deny, valueType, flags, depth);
        if (deny.size() != 0)
            for (int i = free.size(); --i >= 0; )
                if (deny.indexOf(free.get(i)) >= 0)
                    free.remove(i);
        scope = new Scope(scope, name, value);
        if ((flags & RESTRICT_ALL) == 0)
            scope.free = (YType[]) free.toArray(new YType[free.size()]);
        return scope;
    }

    static Scope bindPoly(String name, YType valueType, Binder value,
                          Scope scope) {
        return bind(name, valueType, value, RESTRICT_POLY, 0, scope);
    }

    static YType resolveTypeDef(Scope scope, String name, YType[] param,
                                int depth, Node where) {
        for (; scope != null; scope = scope.outer) {
            if (scope.typeDef != null && scope.name == name) {
                if (scope.typeDef.length - 1 != param.length) {
                    throw new CompileException(where,
                        "Type " + name + " expects "
                        + (scope.typeDef.length == 2 ?  "1 parameter"
                            : (scope.typeDef.length - 1) + " parameters")
                        + ", not " + param.length);
                }
                Map vars = createFreeVars(scope.free, depth);
                for (int i = param.length; --i >= 0;)
                    vars.put(scope.typeDef[i], param[i]);
                return copyType(scope.typeDef[param.length], vars,
                                new IdentityHashMap());
            }
        }
        throw new CompileException(where, "Unknown type: " + name);
    }
}
