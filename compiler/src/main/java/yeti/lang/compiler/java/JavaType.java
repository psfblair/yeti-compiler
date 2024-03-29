// ex: se sts=4 sw=4 expandtab:

/*
 * Yeti language compiler java class type reader.
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
package yeti.lang.compiler.java;

import yeti.lang.compiler.CompileException;
import yeti.lang.compiler.classfinder.ClassFinder;
import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.parser.Node;
import yeti.lang.compiler.parser.ObjectRefOp;
import yeti.lang.compiler.yeti.type.Scope;
import yeti.lang.compiler.yeti.type.TypeException;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.Opcodes;

import java.util.*;

public class JavaType implements Cloneable {

    public static JavaType fromDescription(String sig) {
        synchronized (CACHE) {
            JavaType t = (JavaType) CACHE.get(sig);
            if (t == null) {
                t = new JavaType(sig);
                CACHE.put(sig, t);
            }
            return t;
        }
    }

    static class Field {
        int access;
        String name;
        YType type;
        YType classType;
        String className;
        Object constValue;

        public Field(String name, int access,
                     String className, YType type) {
            this.access = access;
            this.type = type;
            this.name = name;
            this.className = className;
        }

        YType convertedType() {
            return convertValueType(type);
        }

        void check(Node where, String packageName) {
            classType.getJavaType().checkPackage(where, packageName);
            if ((access & classType.getJavaType().publicMask) == 0)
                checkPackage(where, packageName, className, "field", name);
        }
    }

    public static class Method {
        int access;
        String name;
        YType[] arguments;
        YType returnType;
        YType classType;
        String className; // name of the class the method actually belongs to
        String sig;
        String descr;

        Method dup(Method[] arr, int n, YType classType) {
            if (classType == this.classType ||
                className.equals(classType.getJavaType().className())) {
                this.classType = classType;
                return this;
            }
            Method m = new Method();
            m.access = access;
            m.name = name;
            m.arguments = arguments;
            m.returnType = returnType;
            m.classType = classType;
            m.className = className;
            m.sig = sig;
            m.descr = descr;
            arr[n] = m;
            return m;
        }

        Method check(Node where, String packageName, int extraMask) {
            classType.getJavaType().checkPackage(where, packageName);
            if ((access & (classType.getJavaType().publicMask | extraMask)) == 0)
                checkPackage(where, packageName, className, "method", name);
            return this;
        }

        public String toString() {
            StringBuffer s =
                new StringBuffer(returnType.getType() == YetiType.UNIT
                                    ? "void" : returnType.toString());
            s.append(' ');
            s.append(name);
            s.append('(');
            for (int i = 0; i < arguments.length; ++i) {
                if (i != 0)
                    s.append(", ");
                s.append(arguments[i]);
            }
            s.append(")");
            return s.toString();
        }

        YType convertedReturnType() {
            return convertValueType(returnType);
        }

        String argDescr(int arg) {
            return descriptionOf(arguments[arg]);
        }

        String descr(String extra) {
            if (descr != null) {
                return descr;
            }
            StringBuffer result = new StringBuffer("(");
            for (int i = 0; i < arguments.length; ++i) {
                result.append(argDescr(i));
            }
            if (extra != null)
                result.append(extra);
            result.append(')');
            if (returnType.getType() == YetiType.UNIT) {
                result.append('V');
            } else {
                result.append(descriptionOf(returnType));
            }
            return descr = result.toString();
        }
    }

    private static final JavaType[] EMPTY_JTARR = {};
    static final Map JAVA_PRIM = new HashMap();

    private final String description;
    private boolean resolved;
    private Map fields;
    private Map staticFields;
    private Method[] methods;
    private Method[] staticMethods;
    private Method[] constructors;
    private JavaType parent;
    private HashMap interfaces;
    private static HashMap CACHE = new HashMap();
    private int publicMask = Opcodes.ACC_PUBLIC;
    private int access;
    JavaClass implementation;

    public String className() {
        if (!description.startsWith("L")) {
            throw new RuntimeException("No className for " + description);
        }
        return description.substring(1, description.length() - 1);
    }

    public String getDescription() {
        return description;
    }

    void setPublicMask(int publicMask) {
        this.publicMask = publicMask;
    }

    int getAccess() {
        return access;
    }

    static void checkPackage(Node where, String packageName,
                             String name, String what, String item) {
        if (!JavaType.packageOfClass(name).equals(packageName))
            throw new CompileException(where,
                "Non-public " + what + ' ' + name.replace('/', '.')
                + (item == null ? "" : "#".concat(item))
                + " cannot be accessed from different package ("
                + packageName.replace('/', '.') + ")");
    }

    public void checkPackage(Node where, String packageName) {
        if ((access & Opcodes.ACC_PUBLIC) == 0)
            checkPackage(where, packageName, className(),
                         (access & Opcodes.ACC_INTERFACE) != 0
                            ? "interface" : "class", null);
    }

    public boolean isInterface() {
        return (access & Opcodes.ACC_INTERFACE) != 0;
    }

    public static String descriptionOf(YType t) {
        if (t.getType() == YetiType.VAR) {
            if (t.getRef() != null) {
                return descriptionOf(t.getRef());
            }
            return "Ljava/lang/Object;";
        }
        String r = "";
        while (t.getType() == YetiType.JAVA_ARRAY) {
            r = r.concat("[");
            t = t.getParam()[0];
        }
        if (t.getType() != YetiType.JAVA) {
            return "Ljava/lang/Object;";
        }
        return r.concat(t.getJavaType().getDescription());
    }

    static YType convertValueType(YType t) {
        if (t.getType() != YetiType.JAVA) {
            return t;
        }
        String descr = t.getJavaType().getDescription();
        if (descr == "Ljava/lang/String;" || descr == "C") {
            return YetiType.STR_TYPE;
        }
        if (descr == "Ljava/lang/Boolean;" || descr == "Z") {
            return YetiType.BOOL_TYPE;
        }
        if (descr == "Lyeti/lang/Num;" ||
            descr.length() == 1 && "BDFIJS".indexOf(descr.charAt(0)) >= 0) {
            return YetiType.NUM_TYPE;
        }
        if (descr == "V") {
            return YetiType.UNIT_TYPE;
        }
        return t;
    }

    private static JavaType getClass(YType t) {
        switch (t.getType()) {
        case YetiType.JAVA:
            return t.getJavaType();
        case YetiType.NUM:
            return fromDescription("Lyeti/lang/Num;");
        case YetiType.STR:
            return fromDescription("Ljava/lang/String;");
        case YetiType.BOOL:
            return fromDescription("Ljava/lang/Boolean;");
        case YetiType.MAP:
            switch (t.getParam()[2].getType()) {
            case YetiType.LIST_MARKER:
                return fromDescription(t.getParam()[1].getType() == YetiType.NUM
                            ? "Lyeti/lang/MList;" : "Lyeti/lang/AList;");
            case YetiType.MAP_MARKER:
                return fromDescription("Ljava/util/Map;");
            }
            return fromDescription("Lyeti/lang/ByKey;");
        case YetiType.FUN:
            return fromDescription("Lyeti/lang/Fun;");
        case YetiType.VARIANT:
            return fromDescription("Lyeti/lang/Tag;");
        case YetiType.STRUCT:
            return fromDescription("Lyeti/lang/Struct;");
        case YetiType.UNIT:
        case YetiType.VAR:
            return fromDescription("Ljava/lang/Object;");
        }
        return null;
    }

    static void checkUnsafeCast(Node cast,
                                YType from, YType to) {
        if (from.getType() != YetiType.JAVA && from.getType() != YetiType.VAR && to.getType() != YetiType.JAVA) {
            throw new CompileException(cast,
                "Illegal cast from " + from + " to " + to +
                " (neither side is java object)");
        }
        JavaType src = getClass(from);
        if (src == null)
            throw new CompileException(cast, "Illegal cast from " + from);
        JavaType dst = getClass(to);
        if (dst == null) {
            if (src.description == "Ljava/lang/Object;" &&
                    to.deref().getType() == YetiType.JAVA_ARRAY)
                return;
            throw new CompileException(cast, "Illegal cast to " + to);
        }
        if (to.getType() == YetiType.JAVA &&
            (src.access & Opcodes.ACC_INTERFACE) != 0) {
            return;
        }
        try {
            if (dst.isAssignable(src) < 0 &&
                (from.getType() != YetiType.JAVA || src.isAssignable(dst) < 0)) {
                throw new CompileException(cast,
                    "Illegal cast from " + from + " to " + to);
            }
        } catch (JavaClassNotFoundException ex) {
            throw new CompileException(cast, ex);
        }
    }

    static void checkThrowable(Node node, YType t) {
        t = t.deref();
        try {
            if (t.getType() != YetiType.JAVA ||
                JavaType.fromDescription("Ljava/lang/Throwable;")
                    .isAssignable(t.getJavaType()) == -1) {
                throw new CompileException(node,
                                "Not a Throwable instance (" + t + ")");
            }
        } catch (JavaClassNotFoundException ex) {
            throw new CompileException(node, ex);
        }
    }

    private JavaType(String description) {
        this.description = description.intern();
    }

    static JavaType createNewClass(String className, JavaClass impl) {
        JavaType t = new JavaType('L' + className + ';');
        t.implementation = impl;
        return t;
    }

    boolean isCollection() {
        return description == "Ljava/util/List;" ||
               description == "Ljava/util/Collection;" ||
               description == "Ljava/util/Set;";
    }

    public String dottedName() {
        return className().replace('/', '.');
    }

    private static void putMethods(Map mm, Method[] methods) {
        for (int i = methods.length; --i >= 0;) {
            Method m = methods[i];
            mm.put(m.sig, m);
        }
    }

    private static void putMethods(Map mm, List methods) {
        for (int i = methods.size(); --i >= 0;) {
            Method m = (Method) methods.get(i);
            mm.put(m.sig, m);
        }
    }

    private static Method[] methodArray(Collection c) {
        return (Method[]) c.toArray(new Method[c.size()]);
    }

    private synchronized void resolve() throws JavaClassNotFoundException {
        if (resolved)
            return;
        if (!description.startsWith("L")) {
            resolved = true;
            return;
        }
        JavaTypeReader t = ClassFinder.get().readClass(className());
        if (t == null) {
            throw new JavaClassNotFoundException(dottedName());
        }
        resolve(t);
    }

    void resolve(JavaTypeReader t) throws JavaClassNotFoundException {
        access = t.getAccess();
        interfaces = new HashMap();
        if (t.getInterfaces() != null) {
            for (int i = t.getInterfaces().length; --i >= 0;) {
                JavaType it = fromDescription('L' + t.getInterfaces()[i] + ';');
                it.resolve();
                interfaces.putAll(it.interfaces);
                interfaces.put(it.description, it);
            }
        }
        fields = new HashMap();
        staticFields = new HashMap();
        HashMap mm = new HashMap();
        HashMap smm = new HashMap();
        if (t.parent != null) {
            parent = t.parent;
            parent.resolve();
        }
        for (Iterator i = interfaces.values().iterator(); i.hasNext();) {
            JavaType ii = (JavaType) i.next();
            staticFields.putAll(ii.staticFields);
            putMethods(mm, ii.methods);
        }
        if (parent != null) {
            interfaces.putAll(parent.interfaces);
            fields.putAll(parent.fields);
            staticFields.putAll(parent.staticFields);
            putMethods(mm, parent.methods);
            putMethods(smm, parent.staticMethods);
        }
        fields.putAll(t.fields);
        staticFields.putAll(t.staticFields);
        putMethods(mm, t.methods);
        putMethods(smm, t.staticMethods);
        constructors = methodArray(t.constructors);
        methods = methodArray(mm.values());
        staticMethods = methodArray(smm.values());
        resolved = true;
    }

    void checkAbstract() {
        if ((access & Opcodes.ACC_ABSTRACT) != 0)
            return;
        for (int i = methods.length; --i >= 0;) {
            if ((methods[i].access & Opcodes.ACC_ABSTRACT) != 0) {
                access |= Opcodes.ACC_ABSTRACT;
                return;
            }
        }
    }

    static final String[] NUMBER_TYPES = {
        "Ljava/lang/Byte;",
        "Ljava/lang/Short;",
        "Ljava/lang/Float;",
        "Ljava/lang/Integer;",
        "Ljava/lang/Long;",
        "Ljava/math/BigInteger;",
        "Ljava/math/BigDecimal;"
    };

    //static final String[] NUMBER_X = { "B", "S", "F", "D", "I", "J", "i", "d" };

    int isAssignable(JavaType from) throws JavaClassNotFoundException {
        from.resolve();
        if (this == from) {
            return 0;
        }
        if (from.description.length() == 1) {
            return -1;
        }
        if (from.interfaces.containsKey(description)) {
            return 1; // I'm an interface implemented by from
        }
        from = from.parent;
        // I'm from or one of from's parents?
        for (int i = 1; from != null; ++i) {
            if (this == from) {
                return i;
            }
            from.resolve();
            from = from.parent;
        }
        return -1;
    }

    YType[] TRY_SMART =
        { YetiType.BOOL_TYPE, YetiType.STR_TYPE, YetiType.NUM_TYPE };

    // -1 not assignable. 0 - perfect match. > 0 convertable.
    int isAssignableJT(YType to, YType from, boolean smart)
            throws JavaClassNotFoundException, TypeException {
        int ass;
        if (from.getType() != YetiType.JAVA && description == "Ljava/lang/Object;") {
            return from.getType() == YetiType.VAR ? 1 : 10;
        }
        switch (from.getType()) {
        case YetiType.STR:
            return "Ljava/lang/String;" == description ? 0 :
                   "Ljava/lang/CharSequence;" == description ? 1 :
                   "Ljava/lang/StringBuffer;" == description ||
                   "Ljava/lang/StringBuilder;" == description ? 2 :
                   "C" == description ? 3 : -1;
        case YetiType.NUM:
            if (description == "D" || description == "Ljava/lang/Double;")
                return 3;
            if (description.length() == 1)
                return "BFIJS".indexOf(description.charAt(0)) < 0 ? -1 : 4;
            for (int i = NUMBER_TYPES.length; --i >= 0;)
                if (NUMBER_TYPES[i] == description)
                    return 4;
            return description == "Ljava/lang/Number;" ? 1 :
                   description == "Lyeti/lang/Num;" ? 0 : -1;
        case YetiType.BOOL:
            return description == "Z" || description == "Ljava/lang/Boolean;"
                    ? 0 : -1;
        case YetiType.FUN:
            return description == "Lyeti/lang/Fun;" ? 0 : -1;
        case YetiType.MAP: {
            switch (from.getParam()[2].deref().getType()) {
            case YetiType.MAP_MARKER:
                return "Ljava/util/Map;" == description &&
                       (to.getParam().length == 0 ||
                        isAssignable(to.getParam()[1], from.getParam()[0], smart) == 0 &&
                        isAssignable(from.getParam()[1], to.getParam()[0], smart) >= 0)
                       ? 0 : -1;
            case YetiType.LIST_MARKER:
                if ("Ljava/util/List;" == description ||
                    "Ljava/util/Collection;" == description ||
                    "Ljava/util/Set;" == description ||
                    "Lyeti/lang/AList;" == description ||
                    "Lyeti/lang/AIter;" == description)
                    break;
            default:
                    return -1;
            }
            return to.getParam().length == 0 ||
                   (ass = isAssignable(to.getParam()[0], from.getParam()[0], smart)) == 0
                   || ass > 0 && from.getParam()[1].getType() == YetiType.NONE ? 1 : -1;
        }
        case YetiType.STRUCT:
            return description == "Lyeti/lang/Struct;" ? 0 : -1;
        case YetiType.VARIANT:
            return description == "Lyeti/lang/Tag;" ? 0 : -1;
        case YetiType.JAVA:
            return isAssignable(from.getJavaType());
        case YetiType.JAVA_ARRAY:
            return ("Ljava/util/Collection;" == description ||
                    "Ljava/util/List;" == description) &&
                   (to.getParam().length == 0 ||
                    isAssignable(to.getParam()[0], from.getParam()[0], smart) == 0)
                   ? 1 : -1;
        case YetiType.VAR:
            if (smart) {
                for (int i = 0; i < TRY_SMART.length; ++i) {
                    int r = isAssignableJT(to, TRY_SMART[i], false);
                    if (r >= 0) {
                        YetiType.unify(from, TRY_SMART[i]);
                        return r;
                    }
                }
                YetiType.unify(from, to);
                return 1;
            }
        }
        return description == "Ljava/lang/Object;" ? 10 : -1;
    }

    static int isAssignable(YType to, YType from, boolean smart)
            throws JavaClassNotFoundException, TypeException {
        int ass;
//        System.err.println(" --> isAssignable(" + to + ", " + from + ")");
        to = to.deref();
        from = from.deref();
        if (to.getType() == YetiType.JAVA) {
            return to.getJavaType().isAssignableJT(to, from, smart);
        }
        if (to.getType() == YetiType.JAVA_ARRAY) {
            YType of = to.getParam()[0];
            switch (from.getType()) {
            case YetiType.STR:
                return of.getType() == YetiType.JAVA &&
                       of.getJavaType().getDescription() == "C" ? 1 : -1;
            case YetiType.MAP: {
                return from.getParam()[2].getType() == YetiType.LIST_MARKER &&
                       (ass = isAssignable(to.getParam()[0], from.getParam()[0], smart))
                            >= 0 ? 1 : -1;
            }
            case YetiType.JAVA_ARRAY:
                return isAssignable(to.getParam()[0], from.getParam()[0], smart);
            }
        }
        if (to.getType() == YetiType.STR &&
            from.getType() == YetiType.JAVA &&
            from.getJavaType().getDescription() == "Ljava/lang/String;")
            return 0;
        return -1;
    }

    static int isAssignable(Node where, YType to,
                            YType from, boolean smart) {
        from = from.deref();
        if (smart && from.getType() == YetiType.UNIT) {
            return 0;
        }
        try {
            return isAssignable(to, from, smart);
        } catch (JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        } catch (TypeException ex) {
            throw new CompileException(where, ex.getMessage());
        }
    }

    static boolean isSafeCast(Node where,
                              YType to, YType from) {
        to = to.deref();
        from = from.deref();
        // automatic array wrapping
        YType mapKind;
        if (from.getType() == YetiType.JAVA_ARRAY && to.getType() == YetiType.MAP &&
            ((mapKind = to.getParam()[2].deref()).getType() == YetiType.LIST_MARKER ||
             mapKind.getType() == YetiType.VAR)) {
            YType fp = from.getParam()[0].deref();
            String fromDesc = fp.getJavaType().getDescription();
            if (fromDesc == "C")
                return false;
            YType tp = to.getParam()[0].deref();
            try {
                if (fromDesc.length() == 1) {
                    YetiType.unify(to.getParam()[1], YetiType.NO_TYPE);
                    YetiType.unify(to.getParam()[0], YetiType.NUM_TYPE);
                } else if (tp.getType() == YetiType.VAR) {
                    if (fp != tp)
                        YetiType.unifyToVar(tp, fp);
                } else if (isAssignable(where, tp, fp, false) < 0) {
                    return false;
                }
            } catch (TypeException ex) {
                return false;
            }
            mapKind.setType(YetiType.LIST_MARKER);
            mapKind.setParam(YetiType.NO_PARAM);
            YType index = to.getParam()[1].deref();
            if (index.getType() == YetiType.VAR) {
                index.setType(YetiType.NUM);
                index.setParam(YetiType.NO_PARAM);
            }
            return true;
        }
        boolean smart = true;
        boolean mayExact = false;
        while (from.getType() == YetiType.MAP &&
               from.getParam()[2].getType() == YetiType.LIST_MARKER &&
               (to.getType() == YetiType.MAP &&
                from.getParam()[1].getType() == YetiType.NONE &&
                to.getParam()[2].getType() == YetiType.LIST_MARKER &&
                to.getParam()[1].getType() != YetiType.NUM ||
                to.getType() == YetiType.JAVA_ARRAY)) {
            if (to.getType() == YetiType.JAVA_ARRAY)
                mayExact = true;
            from = from.getParam()[0].deref();
            to = to.getParam()[0].deref();
            smart = false;
        }
        if (to.getType() == YetiType.STR && smart &&
                (from.getType() == YetiType.JAVA &&
                    from.getJavaType().getDescription() == "Ljava/lang/String;"/* ||
                 from.getType() == YetiType.JAVA_ARRAY &&
                    from.param[0].deref().getJavaType().getDescription() == "C"*/))
            return true;
        if (from.getType() != YetiType.JAVA)
            return false;
        try {
            return to.getType() == YetiType.JAVA &&
                    (to.getJavaType() != from.getJavaType() || mayExact) &&
                   (smart ? isAssignable(where, to, from, true)
                          : to.getJavaType().isAssignable(from.getJavaType())) >= 0;
        } catch (JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        }
    }

    private Method resolveByArgs(Node n, Method[] ma,
                                 String name, Code[] args,
                                 YType objType) {
        name = name.intern();
        int rAss = Integer.MAX_VALUE;
        int res = -1;
        int suitable[] = new int[ma.length];
        int suitableCounter = 0;
        for (int i = ma.length; --i >= 0;) {
            Method m = ma[i];
            if (m.name == name && m.arguments.length == args.length) {
                suitable[suitableCounter++] = i;
            }
        }
        boolean single = suitableCounter == 1;
    find_match:
        while (--suitableCounter >= 0) {
            int index = suitable[suitableCounter];
            Method m = ma[index];
            int mAss = 0;
            for (int j = 0; j < args.length; ++j) {
                int ass = isAssignable(n, m.arguments[j], args[j].getType(), single);
                if (ass < 0) {
                    continue find_match;
                }
                if (ass != 0) {
                    mAss += ass + 1;
                }
            }
            if (m.returnType.getJavaType() != null &&
                (m.returnType.getJavaType().resolve(n).access &
                    Opcodes.ACC_PUBLIC) == 0)
                mAss += 10;
            if (mAss == 0) {
                res = index;
                break;
            }
            if (mAss < rAss) {
                res = index;
                rAss = mAss;
            }
        }
        if (res != -1) {
            return ma[res].dup(ma, res, objType);
        }
        StringBuffer err = new StringBuffer("No suitable method ")
                                .append(name).append('(');
        for (int i = 0; i < args.length; ++i) {
            if (i != 0)
                err.append(", ");
            err.append(args[i].getType());
        }
        err.append(") found in ").append(dottedName());
        boolean fst = true;
        for (int i = ma.length; --i >= 0;) {
            if (ma[i].name != name)
                continue;
            if (fst) {
                err.append("\nMethods named ").append(name).append(':');
                fst = false;
            }
            err.append("\n    ").append(ma[i]);
        }
        throw new CompileException(n, err.toString());
    }

    public JavaType resolve(Node where) {
        try {
            resolve();
        } catch (JavaClassNotFoundException ex) {
            throw new CompileException(where, ex);
        }
        return this;
    }

    private static JavaType javaTypeOf(Node where,
                                       YType objType, String err) {
        if (objType.getType() != YetiType.JAVA) {
            throw new CompileException(where,
                        err + objType + ", java object expected");
        }
        return objType.getJavaType().resolve(where);
    }

    static Method resolveConstructor(Node call,
                                     YType t, Code[] args,
                                     boolean noAbstract) {
        JavaType jt = t.getJavaType().resolve(call);
        if ((jt.access & Opcodes.ACC_INTERFACE) != 0)
            throw new CompileException(call, "Cannot instantiate interface "
                                             + jt.dottedName());
        if (noAbstract && (jt.access & Opcodes.ACC_ABSTRACT) != 0) {
            StringBuffer msg =
                new StringBuffer("Cannot construct abstract class ");
            msg.append(jt.dottedName());
            int n = 0;
            for (int i = 0; i < jt.methods.length; ++i)
                if ((jt.methods[i].access & Opcodes.ACC_ABSTRACT) != 0) {
                    if (++n == 1) {
                        msg.append("\nAbstract methods found in ");
                        msg.append(jt.dottedName());
                        msg.append(':');
                    } else if (n > 2) {
                        msg.append("\n    ...");
                        break;
                    }
                    msg.append("\n    ");
                    msg.append(jt.methods[i]);
                }
            throw new CompileException(call, msg.toString());
        }
        return jt.resolveByArgs(call, jt.constructors, "<init>", args, t);
    }

    static Method resolveMethod(ObjectRefOp ref,
                                YType objType, Code[] args,
                                boolean isStatic) {
        objType = objType.deref();
        JavaType jt = javaTypeOf(ref, objType, "Cannot call method on ");
        return jt.resolveByArgs(ref, isStatic ? jt.staticMethods : jt.methods,
                                ref.getName(), args, objType);
    }

    static Field resolveField(ObjectRefOp ref,
                              YType objType,
                              boolean isStatic) {
        objType = objType.deref();
        JavaType jt = javaTypeOf(ref, objType, "Cannot access field on ");
        Map fm = isStatic ? jt.staticFields : jt.fields;
        Field field = (Field) fm.get(ref.getName());
        if (field == null) {
            throw new CompileException(ref,
                        (isStatic ? "Static field " : "Field ") +
                        ref.getName() + " not found in " + jt.dottedName());
        }
        if (field.classType != objType) {
            if (!field.className.equals(objType.getJavaType().className())) {
                field = new Field(field.name, field.access,
                                  field.className, field.type);
                fm.put(field.name, field);
            }
            field.classType = objType;
        }
        return field;
    }

    JavaType dup() {
        if (!resolved)
            throw new IllegalStateException("Cannot clone unresolved class");
        try {
            return (JavaType) clone();
        } catch (CloneNotSupportedException ex) {
            throw new IllegalStateException(ex);
        }
    }

    static YType typeOfClass(String packageName, String className) {
        if (packageName != null && packageName.length() != 0) {
            className = packageName + '/' + className;
        }
        return new YType("L" + className + ';');
    }

    public String str() {
        switch (description.charAt(0)) {
            case 'Z': return "boolean";
            case 'B': return "byte";
            case 'C': return "char";
            case 'D': return "double";
            case 'F': return "float";
            case 'I': return "int";
            case 'J': return "long";
            case 'S': return "short";
            case 'V': return "void";
            case 'L': return "~".concat(dottedName());
        }
        return "~".concat(description);
    }

    static String packageOfClass(String className) {
        if (className == null || className.length() == 0) {
            return "";
        }
        int p = className.lastIndexOf('/');
        return p < 0 ? "" : className.substring(0, p);
    }

/*    static YType toStructType(YType object) {
        return null;
    }*/

    private static List parentList(JavaType t) {
        List a = new ArrayList();
        while (t != null) {
            a.add(t);
            t = t.parent;
        }
        return a;
    }

    static YType mergeTypes(YType a, YType b) {
        a = a.deref();
        b = b.deref();
        if (a.getType() != YetiType.JAVA || b.getType() != YetiType.JAVA) {
            // immutable lists can be recursively merged
            if (a.getType() == YetiType.MAP && b.getType() == YetiType.MAP &&
                a.getParam()[1].getType() == YetiType.NONE &&
                a.getParam()[2].getType() == YetiType.LIST_MARKER &&
                b.getParam()[1].getType() == YetiType.NONE &&
                b.getParam()[2].getType() == YetiType.LIST_MARKER) {
                YType t = mergeTypes(a.getParam()[0], b.getParam()[0]);
                if (t != null) {
                    return new YType(YetiType.MAP, new YType[] {
                                    t, YetiType.NO_TYPE, YetiType.LIST_TYPE });
                }
            }
            if (a.getType() == YetiType.UNIT &&
                (b.getType() == YetiType.JAVA &&
                    b.getJavaType().getDescription().length() != 1 ||
                 b.getType() == YetiType.JAVA_ARRAY))
                return b;
            if (b.getType() == YetiType.UNIT &&
                (a.getType() == YetiType.JAVA &&
                    a.getJavaType().getDescription().length() != 1 ||
                 a.getType() == YetiType.JAVA_ARRAY))
                return a;
            return null;
        }
        if (a.getJavaType() == b.getJavaType()) {
            return a;
        }
        List aa = parentList(a.getJavaType()), ba = parentList(b.getJavaType());
        JavaType common = null;
        for (int i = aa.size(), j = ba.size();
             --i >= 0 && --j >= 0 && aa.get(i) == ba.get(j);) {
            common = (JavaType) aa.get(i);
        }
        if (common == null) {
            return null;
        }
        JavaType aj = a.getJavaType(), bj = b.getJavaType();
        if (common.description == "Ljava/lang/Object;") {
            int mc = -1;
            if (bj.interfaces.containsKey(aj.description)) {
                return a;
            }
            if (aj.interfaces.containsKey(bj.description)) {
                return b;
            }
            Map m = bj.interfaces;
            Iterator i = aj.interfaces.keySet().iterator();
            while (i.hasNext()) {
                Object o;
                if ((o = m.get(i.next())) != null) {
                    JavaType jt = (JavaType) o;
                    int n = jt.methods.length;
                    if (n > mc) {
                        common = jt;
                    }
                }
            }
        }
        YType t = new YType(YetiType.JAVA, YetiType.NO_PARAM);
        t.setJavaType(common);
        return t;
    }

    static YType typeOfName(String name, Scope scope) {
        int arrays = 0;
        while (name.endsWith("[]")) {
            ++arrays;
            name = name.substring(0, name.length() - 2);
        }
        String descr = (String) JAVA_PRIM.get(name);
        YType t = descr != null ? new YType(descr) :
                   YetiType.resolveFullClass(arrays == 0 ? name : name.intern(),
                                             scope);
        while (--arrays >= 0)
            t = new YType(YetiType.JAVA_ARRAY, new YType[] { t });
        return t;
    }

    static {
        Map p = JAVA_PRIM;
        p.put("int",     "I");
        p.put("long",    "J");
        p.put("boolean", "Z");
        p.put("byte",    "B");
        p.put("char",    "C");
        p.put("double",  "D");
        p.put("float",   "F");
        p.put("short",   "S");
        p.put("number",  "Lyeti/lang/Num;");
    }
}
