package yeti.lang.compiler.classfinder;

import yeti.lang.compiler.code.CompileCtx;
import yeti.lang.compiler.java.JavaNode;
import yeti.lang.compiler.java.JavaSource;
import yeti.lang.compiler.java.JavaTypeReader;
import yeti.renamed.asm3.ClassReader;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class ClassFinder {
    private ClassPathItem[] classPath;
    private Map defined = new HashMap();
    final Map parsed = new HashMap();
    private final Map existsCache = new HashMap();
    final String pathStr;

    ClassFinder(String cp) {
        this(cp.split(File.pathSeparator));
    }

    ClassFinder(String[] cp) {
        classPath = new ClassPathItem[cp.length];
        StringBuffer buf = new StringBuffer();
        for (int i = 0; i < cp.length; ++i) {
            classPath[i] = cp[i].endsWith(".jar")
                ? (ClassPathItem) new ClassJar(cp[i]) : new ClassDir(cp[i]);
            if (i != 0)
                buf.append(File.pathSeparator);
            buf.append(cp[i]);
        }
        pathStr = buf.toString();
    }

    public Map getParsed() {
        return parsed;
    }

    public String getPathStr() {
        return pathStr;
    }

    public Map getExistsCache() {
        return existsCache;
    }

    public InputStream findClass(String name) {
        Object x = defined.get(name);
        if (x != null) {
            return new ByteArrayInputStream((byte[]) x);
        }
        InputStream in;
        for (int i = 0; i < classPath.length; ++i) {
            try {
                if ((in = classPath[i].getStream(name)) != null)
                    return in;
            } catch (IOException ex) {
            }
        }
        ClassLoader clc = Thread.currentThread().getContextClassLoader();
        in = clc != null ? clc.getResourceAsStream(name) : null;
        return in != null ? in :
                getClass().getClassLoader().getResourceAsStream(name);
    }

    public void define(String name, byte[] content) {
        defined.put(name, content);
    }

    boolean exists(String name) {
        if (parsed.containsKey(name))
            return true;
        Boolean known = (Boolean) existsCache.get(name);
        if (known != null)
            return known.booleanValue();
        String fn = name.concat(".class");
        boolean found = false;
        for (int i = 0; i < classPath.length; ++i)
            if (classPath[i].exists(fn)) {
                found = true;
                break;
            }
        ClassLoader clc;
        InputStream in;
        if (!found &&
              (clc = Thread.currentThread().getContextClassLoader()) != null &&
              (in = clc.getResourceAsStream(fn)) != null) {
            found = true;
            try {
                in.close();
            } catch (Exception ex) {
            }
        }
        existsCache.put(name, Boolean.valueOf(found));
        return found;
    }

    JavaTypeReader readClass(String className) {
        JavaTypeReader t = new JavaTypeReader();
        t.setClassName(className);
        Object classNode = parsed.get(className);
        if (classNode != null) {
            JavaSource.loadClass(this, t, (JavaNode) classNode);
            return t;
        }
        InputStream in = findClass(className + ".class");
        if (in == null)
            return null;
        try {
            new ClassReader(in).accept(t, null,
                    ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        } catch (IOException ex) {
            return null;
        }
        return t;
    }

    static ClassFinder get() {
        return CompileCtx.current().getClassPath();
    }
}

