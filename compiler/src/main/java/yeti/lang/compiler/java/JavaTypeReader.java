package yeti.lang.compiler.java;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.renamed.asm3.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JavaTypeReader implements ClassVisitor, Opcodes {
    Map vars = new HashMap();
    Map fields = new HashMap();
    Map staticFields = new HashMap();
    List methods = new ArrayList();
    List staticMethods = new ArrayList();
    List constructors = new ArrayList();
    JavaType parent;

    private String className;
    private String[] interfaces;
    private int access;

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    String[] getInterfaces() {
        return interfaces;
    }

    void setInterfaces(String[] interfaces) {
        this.interfaces = interfaces;
    }

    int getAccess() {
        return access;
    }

    void setAccess(int access) {
        this.access = access;
    }

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        if (superName != null)
            parent = JavaType.fromDescription('L' + superName + ';');
        this.access = access;
        this.interfaces = interfaces;
/*        System.err.println("visit: ver=" + version + " | access=" + access
            + " | name=" + name + " | sig=" + signature + " super="
            + superName);*/
    }

    public void visitEnd() {
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {
    }

    private static int parseSig(Map vars, List res, int p, char[] s) {
        int arrays = 0;
        for (int l = s.length; p < l && s[p] != '>'; ++p) {
            if (s[p] == '+' || s[p] == '*') {
                continue;
            }
            if (s[p] == '[') {
                ++arrays;
                continue;
            }
            if (s[p] == ')')
                continue;
            YType t = null;
            if (s[p] == 'L') {
                int p1 = p;
                while (p < l && s[p] != ';' && s[p] != '<')
                    ++p;
                t = new YType(new String(s, p1, p - p1).concat(";"));
                if (p < l && s[p] == '<') {
                    List param = new ArrayList();
                    p = parseSig(vars, param, p + 1, s) + 1;
                    /* XXX: workaround for broken generics support
                    //      strips free type vars from classes...
                    for (int i = param.size(); --i >= 0;) {
                        if (((YType) param.get(i)).type
                                == YetiType.VAR) {
                            param.remove(i);
                        }
                    }*/
                    t.setParam((YType[]) param.toArray(new YType[param.size()]));
                }
            } else if (s[p] == 'T') {
                int p1 = p + 1;
                while (++p < l && s[p] != ';' && s[p] != '<');
                /*String varName = new String(s, p1, p - p1);
                t = (YType) vars.get(varName);
                if (t == null) {
                    t = new YType(1000000);
                    vars.put(varName, t);
                }*/
                t = YetiType.OBJECT_TYPE;
            } else {
                t = new YType(new String(s, p, 1));
            }
            for (; arrays > 0; --arrays) {
                t = new YType(YetiType.JAVA_ARRAY,
                            new YType[] { t });
            }
            res.add(t);
        }
        return p;
    }

    private List parseSig(int start, String sig) {
        List res = new ArrayList();
        parseSig(vars, res, start, sig.toCharArray());
        return res;
    }

    static YType[] parseSig1(int start, String sig) {
        List res = new ArrayList();
        parseSig(new HashMap(), res, start, sig.toCharArray());
        return (YType[]) res.toArray(new YType[res.size()]);
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        if ((access & ACC_PRIVATE) == 0) {
/*            System.err.println("visitField: name=" + name + " | desc="
                    + desc + " | sig=" + signature + " | val=" + value
                    + " | access=" + access);*/
            List l = parseSig(0, signature == null ? desc : signature);
            JavaType.Field f =
                new JavaType.Field(name, access, className, (YType) l.get(0));
            if ((access & (ACC_FINAL | ACC_STATIC)) == (ACC_FINAL | ACC_STATIC))
                f.constValue = value;
            (((access & ACC_STATIC) == 0) ? fields : staticFields).put(name, f);
        }
        return null;
    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
/*        System.err.println("visitInnerClass: name=" +
            name + " | outer=" + outerName + " | inner=" + innerName);*/
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        if ((access & ACC_PRIVATE) == 0) {
/*            System.err.println("visitMethod: name=" + name + " | desc=" + desc
                + " | sig=" + signature + " | exc=" +
                (exceptions == null ? "()"
                    : Arrays.asList(exceptions).toString())
                + " | access=" + access);*/
            JavaType.Method m = new JavaType.Method();
//            if (signature == null) {
                signature = desc;
  //          }
            List l = parseSig(1, signature);
            m.sig = name + signature;
            m.name = name.intern();
            m.access = access;
            int argc = l.size() - 1;
            m.returnType = (YType) l.get(argc);
            /* hack for broken generic support
            if (m.returnType.type == YetiType.VAR) {
                m.returnType = YetiType.OBJECT_TYPE;
            }*/
            m.arguments = (YType[])
                l.subList(0, argc).toArray(new YType[argc]);
            m.className = className;
            if (m.name == "<init>") {
                constructors.add(m);
            } else if ((access & ACC_STATIC) == 0) {
                methods.add(m);
            } else {
                staticMethods.add(m);
            }
        }
        return null;
    }

    public void visitOuterClass(String owner, String name, String desc) {
/*        System.err.println("visitOuterClass: owner=" + owner + " | name="
            + name + " | desc=" + desc);*/
    }

    public void visitSource(String source, String debug) {
//        System.err.println("visitSource: src=" + source + " | debug=" + debug);
    }
}
