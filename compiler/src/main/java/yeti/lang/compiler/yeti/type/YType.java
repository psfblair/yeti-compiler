package yeti.lang.compiler.yeti.type;

import java.util.Map;
import yeti.lang.compiler.java.JavaType;

public class YType {
    private int type;
    private YType[] param;
    private JavaType javaType;
    private int field;
    private Map finalMembers;

    Map partialMembers;
    YType ref;
    int depth;
    int flags;

    boolean seen;
    Object doc;

    public YType deref() {
        YType res = this;
        while (res.ref != null) {
            res = res.ref;
        }
        for (YType next, type = this; type.ref != null; type = next) {
            next = type.ref;
            type.ref = res;
        }
        if (res.doc == null)
            res.doc = this;
        return res;
    }

    public int getType() {
        return type;
    }

    public YType[] getParam() {
        return param;
    }

    public JavaType getJavaType() {
        return javaType;
    }

    public int getField() {
        return field;
    }

    public Map getFinalMembers() {
        return finalMembers;
    }

    YType(int depth) {
        this.depth = depth;
    }

    YType(int type, YType[] param) {
        this.type = type;
        this.param = param;
    }

    YType(String javaSig) {
        type = yeti.lang.compiler.yeti.type.YetiType.JAVA;
        this.javaType = JavaType.fromDescription(javaSig);
        param = yeti.lang.compiler.yeti.type.YetiType.NO_PARAM;
    }

    public String toString() {
        return (String) new ShowTypeFun().apply("",
                    TypeDescr.yetiType(this, null));
    }

    public String toString(Scope scope) {
        return (String) new ShowTypeFun().apply("",
                    TypeDescr.yetiType(this, TypePattern.toPattern(scope)));
    }

    String doc() {
        for (YType t = this; t != null; t = t.ref)
            if (t.doc != null) {
                String doc;
                if (t.doc instanceof YType) {
                    YType ref = (YType) t.doc;
                    t.doc = null;
                    doc = ref.doc();
                } else {
                    doc = (String) t.doc;
                }
                if (doc != null) {
                    if ((doc = doc.trim()).length() != 0) {
                        t.doc = doc;
                        return doc;
                    }
                    t.doc = null;
                }
            }
        return null;
    }
}
