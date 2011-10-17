package yeti.lang.compiler.code;

import yeti.lang.Fun;
import yeti.lang.compiler.CompileException;
import yeti.lang.compiler.YetiC;
import yeti.lang.compiler.classfinder.ClassFinder;
import yeti.lang.compiler.closure.RootClosure;
import yeti.lang.compiler.java.JavaSource;
import yeti.lang.compiler.parser.ParseException;
import yeti.lang.compiler.source.SourceReader;
import yeti.lang.compiler.struct.StructConstructor;
import yeti.lang.compiler.writer.CodeWriter;
import yeti.lang.compiler.yeti.YetiAnalyzer;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.lang.compiler.yeti.typeattr.ModuleType;
import yeti.lang.compiler.yeti.typeattr.YetiTypeAttr;
import yeti.renamed.asm3.ClassWriter;
import yeti.renamed.asm3.Label;
import yeti.renamed.asm3.Opcodes;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;

public final class CompileCtx implements Opcodes {
    static final ThreadLocal currentCompileCtx = new ThreadLocal();
    private static ClassLoader JAVAC;

    CodeWriter writer;
    private SourceReader reader;
    private Map compiled = new HashMap();
    private List warnings = new ArrayList();
    private String currentSrc;
    private Map definedClasses = new HashMap();
    private List unstoredClasses;
    List postGen = new ArrayList();
    boolean isGCJ;
    private ClassFinder classPath;

    Map types = new HashMap();
    String[] preload = new String[] { "yeti/lang/std", "yeti/lang/io" };
    int classWriterFlags = ClassWriter.COMPUTE_FRAMES;
    int flags;

    public static CompileCtx current() {
        return (CompileCtx) currentCompileCtx.get();
    }

    public ClassFinder getClassPath() {
        return classPath;
    }

    CompileCtx(SourceReader reader, CodeWriter writer) {
        this.reader = reader;
        this.writer = writer;
        // GCJ bytecode verifier is overly strict about INVOKEINTERFACE
        isGCJ = System.getProperty("java.vm.name").indexOf("gcj") >= 0;
//            isGCJ = true;
    }

    void warn(CompileException ex) {
        ex.setFn(currentSrc);
        warnings.add(ex);
    }

    String createClassName(Ctx ctx, String outerClass, String nameBase) {
        boolean anon = nameBase == "" && ctx != null;
        String name = nameBase = outerClass + '$' + nameBase;
        if (anon) {
            do {
                name = nameBase + ctx.constants.anonymousClassCounter++;
            } while (definedClasses.containsKey(name));
        } else {
            for (int i = 0; definedClasses.containsKey(name); ++i)
                name = nameBase + i;
        }
        return name;
    }

    public void enumWarns(Fun f) {
        for (int i = 0, cnt = warnings.size(); i < cnt; ++i) {
            f.apply(warnings.get(i));
        }
    }

    private void generateModuleFields(Map fields, Ctx ctx, Map ignore) {
        if (ctx.compilation.isGCJ)
            ctx.typeInsn(CHECKCAST, "yeti/lang/Struct");
        for (Iterator i = fields.entrySet().iterator(); i.hasNext();) {
            Map.Entry entry = (Map.Entry) i.next();
            String name = (String) entry.getKey();
            if (ignore.containsKey(name))
                continue;
            String jname = Code.mangle(name);
            String type = Code.javaType((YType) entry.getValue());
            String descr = 'L' + type + ';';
            ctx.cw.visitField(ACC_PUBLIC | ACC_STATIC, jname,
                    descr, null, null).visitEnd();
            ctx.insn(DUP);
            ctx.ldcInsn(name);
            ctx.methodInsn(INVOKEINTERFACE, "yeti/lang/Struct",
                "get", "(Ljava/lang/String;)Ljava/lang/Object;");
            ctx.typeInsn(CHECKCAST, type);
            ctx.fieldInsn(PUTSTATIC, ctx.className, jname, descr);
        }
    }

    String compileAll(String[] sources, int flags, String[] javaArg)
            throws Exception {
        String[] fn = new String[1];
        List java = null;
        int i, yetiCount = 0;
        for (i = 0; i < sources.length; ++i)
            if (sources[i].endsWith(".java")) {
                fn[0] = sources[i];
                char[] s = reader.getSource(fn, true);
                new JavaSource(fn[0], s, classPath.getParsed());
                if (java == null) {
                    java = new ArrayList();
                    boolean debug = true;
                    for (int j = 0; j < javaArg.length; ++j) {
                        if (javaArg[j].startsWith("-g"))
                            debug = false;
                        java.add(javaArg[j]);
                    }
                    if (!java.contains("-encoding")) {
                        java.add("-encoding");
                        java.add("utf-8");
                    }
                    if (debug)
                        java.add("-g");
                    if (classPath.getPathStr().length() != 0) {
                        java.add("-cp");
                        java.add(classPath.getPathStr());
                    }
                }
                java.add(sources[i]);
            } else {
                sources[yetiCount++] = sources[i];
            }
        String mainClass = null;
        for (i = 0; i < yetiCount; ++i) {
            String className = compile(sources[i], flags);
            if (!types.containsKey(className))
                mainClass = className;
        }
        if (java != null) {
            javaArg = (String[]) java.toArray(new String[javaArg.length]);
            Class javac = null;
            try {
                javac = Class.forName("com.sun.tools.javac.Main", true,
                                      getClass().getClassLoader());
            } catch (Exception ex) {
            }
            java.lang.reflect.Method m;
            try {
                if (javac == null) { // find javac...
                    synchronized (currentCompileCtx) {
                        if (JAVAC == null)
                            JAVAC = new URLClassLoader(new URL[] {
                                new URL("file://" + new File(System.getProperty(
                                             "java.home"), "/../lib/tools.jar")
                                    .getAbsolutePath().replace('\\', '/')) });
                    }
                    javac =
                        Class.forName("com.sun.tools.javac.Main", true, JAVAC);
                }
                m = javac.getMethod("compile", new Class[] { String[].class });
            } catch (Exception ex) {
                throw new ParseException(null, "Couldn't find Java compiler");
            }
            Object o = javac.newInstance();
            if (((Integer) m.invoke(o, new Object[] {javaArg})).intValue() != 0)
                throw new ParseException(null,
                            "Error while compiling Java sources");
        }
        return yetiCount != 0 ? mainClass : "";
    }

    String compile(String sourceName, int flags) throws Exception {
        String className = (String) compiled.get(sourceName);
        if (className != null)
            return className;
        String[] srcName = { sourceName };
        char[] src;
        try {
            src = reader.getSource(srcName, false);
        } catch (IOException ex) {
            throw new ParseException(null, ex.getMessage());
        }
        int dot = srcName[0].lastIndexOf('.');
        className = dot < 0 ? srcName[0] : srcName[0].substring(0, dot);
        dot = className.lastIndexOf('.');
        if (dot >= 0) {
            dot = Math.max(className.indexOf('/', dot),
                           className.indexOf('\\', dot));
            if (dot >= 0)
                className = className.substring(dot + 1);
        }
        compile(srcName[0], className, src, flags);
        className = (String) compiled.get(srcName[0]);
        compiled.put(sourceName, className);
        return className;
    }

    ModuleType compile(String sourceName, String name,
                          char[] code, int flags) throws Exception {
        // TODO
        // The circular dep check doesn't work always, as the SourceReader +
        // compile(sourceName, flags) gets the proposed classname wrong
        // when the sourcereader modifies the path - even when the original
        // sourceName contained correct name base for classname provided
        // by the YetiTypeAttr when loading a module.
        // The source reading logic is a mess and needs a refactoring, badly.
        if (definedClasses.containsKey(name)) {
            throw new ParseException(0, 0, (definedClasses.get(name) == null
                ? "Circular module dependency: "
                : "Duplicate module name: ") + name.replace('/', '.'));
        }
        boolean module = (flags & YetiC.CF_COMPILE_MODULE) != 0;
        RootClosure codeTree;
        Object oldCompileCtx = currentCompileCtx.get();
        currentCompileCtx.set(this);
        String oldCurrentSrc = currentSrc;
        currentSrc = sourceName;
        if (flags != 0)
            this.flags = flags;
        List oldUnstoredClasses = unstoredClasses;
        unstoredClasses = new ArrayList();
        try {
            try {
                codeTree = YetiAnalyzer.toCode(sourceName, name, code, this, preload);
            } finally {
                currentCompileCtx.set(oldCompileCtx);
            }
            if (codeTree.getModuleType().getName() != null)
                name = codeTree.getModuleType().getName();
            module = module || codeTree.isModule();
            Constants constants = new Constants();
            constants.sourceName = sourceName == null ? "<>" : sourceName;
            Ctx ctx = new Ctx(this, constants, null, null).newClass(ACC_PUBLIC
                    | ACC_SUPER | (module && codeTree.getModuleType().isDeprecated()
                        ? ACC_DEPRECATED : 0), name,
                   (flags & YetiC.CF_EVAL) != 0 ? "yeti/lang/Fun" : null, null);
            constants.ctx = ctx;
            if (module) {
                ctx.cw.visitField(ACC_PRIVATE | ACC_STATIC, "$",
                                  "Ljava/lang/Object;", null, null).visitEnd();
                ctx.cw.visitField(ACC_PRIVATE | ACC_STATIC,
                                  "_$", "Z", null, Boolean.FALSE);
                ctx = ctx.newMethod(ACC_PUBLIC | ACC_STATIC | ACC_SYNCHRONIZED,
                                    "eval", "()Ljava/lang/Object;");
                ctx.fieldInsn(GETSTATIC, name, "_$", "Z");
                Label eval = new Label();
                ctx.jumpInsn(IFEQ, eval);
                ctx.fieldInsn(GETSTATIC, name, "$",
                                     "Ljava/lang/Object;");
                ctx.insn(ARETURN);
                ctx.visitLabel(eval);
                Code codeTail = codeTree.getCode();
                while (codeTail instanceof SeqExpr)
                    codeTail = ((SeqExpr) codeTail).result;
                if (codeTail instanceof StructConstructor) {
                    ((StructConstructor) codeTail).publish();
                    codeTree.gen(ctx);
                    codeTree.getModuleType().setDirectFields(
                        ((StructConstructor) codeTail).getDirect(constants));
                } else {
                    codeTree.gen(ctx);
                }
                ctx.cw.visitAttribute(new YetiTypeAttr(codeTree.getModuleType()));
                if (codeTree.getType().getType() == YetiType.STRUCT) {
                    generateModuleFields(codeTree.getType().getFinalMembers(), ctx,
                                         codeTree.getModuleType().getDirectFields());
                }
                ctx.insn(DUP);
                ctx.fieldInsn(PUTSTATIC, name, "$",
                                     "Ljava/lang/Object;");
                ctx.intConst(1);
                ctx.fieldInsn(PUTSTATIC, name, "_$", "Z");
                ctx.insn(ARETURN);
                types.put(name, codeTree.getModuleType());
            } else if ((flags & YetiC.CF_EVAL) != 0) {
                ctx.createInit(ACC_PUBLIC, "yeti/lang/Fun");
                ctx = ctx.newMethod(ACC_PUBLIC, "apply",
                                    "(Ljava/lang/Object;)Ljava/lang/Object;");
                codeTree.gen(ctx);
                ctx.insn(ARETURN);
            } else {
                ctx = ctx.newMethod(ACC_PUBLIC | ACC_STATIC, "main",
                                    "([Ljava/lang/String;)V");
                ctx.incrementLocalVarCountBy(1);
                ctx.load(0).methodInsn(INVOKESTATIC, "yeti/lang/Core",
                                            "setArgv", "([Ljava/lang/String;)V");
                Label codeStart = new Label();
                ctx.visitLabel(codeStart);
                codeTree.gen(ctx);
                ctx.insn(POP);
                ctx.insn(RETURN);
                Label exitStart = new Label();
                ctx.tryCatchBlock(codeStart, exitStart, exitStart,
                                       "yeti/lang/ExitError");
                ctx.visitLabel(exitStart);
                ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/ExitError",
                                    "getExitCode", "()I");
                ctx.methodInsn(INVOKESTATIC, "java/lang/System",
                                    "exit", "(I)V");
                ctx.insn(RETURN);
            }
            ctx.closeMethod();
            constants.close();
            compiled.put(sourceName, name);
            write();
            unstoredClasses = oldUnstoredClasses;
            classPath.getExistsCache().clear();
            currentSrc = oldCurrentSrc;
            return codeTree.getModuleType();
        } catch (CompileException ex) {
            if (ex.getFn() == null)
                ex.setFn(sourceName);
            throw ex;
        }
    }

    void addClass(String name, Ctx ctx) {
        if (definedClasses.put(name, ctx) != null) {
            throw new IllegalStateException("Duplicate class: "
                                            + name.replace('/', '.'));
        }
        if (ctx != null) {
            unstoredClasses.add(ctx);
        }
    }

    private void write() throws Exception {
        if (writer == null)
            return;
        int i, cnt = postGen.size();
        for (i = 0; i < cnt; ++i)
            ((Runnable) postGen.get(i)).run();
        postGen.clear();
        cnt = unstoredClasses.size();
        for (i = 0; i < cnt; ++i) {
            Ctx c = (Ctx) unstoredClasses.get(i);
            definedClasses.put(c.className, "");
            String name = c.className + ".class";
            byte[] content = c.cw.toByteArray();
            writer.writeClass(name, content);
            classPath.define(name, content);
        }
        unstoredClasses = null;
    }
}
