package yeti.lang.compiler.yeti.typeattr;

import yeti.lang.compiler.CompileException;
import yeti.renamed.asm3.*;

import java.io.InputStream;

class YetiTypeVisitor implements ClassVisitor {
    yeti.lang.compiler.YetiTypeAttr typeAttr;
    private boolean deprecated;

    public void visit(int version, int access, String name, String signature,
                      String superName, String[] interfaces) {
        deprecated = (access & Opcodes.ACC_DEPRECATED) != 0;
    }

    public void visitEnd() {
    }

    public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
        return null;
    }

    public void visitAttribute(Attribute attr) {
        if (attr.type == "YetiModuleType") {
            if (typeAttr != null) {
                throw new RuntimeException(
                    "Multiple YetiModuleType attributes are forbidden");
            }
            typeAttr = (yeti.lang.compiler.YetiTypeAttr) attr;
        }
    }

    public FieldVisitor visitField(int access, String name, String desc,
                                   String signature, Object value) {
        return null;
    }

    public void visitInnerClass(String name, String outerName,
                                String innerName, int access) {
    }

    public MethodVisitor visitMethod(int access, String name, String desc,
                                     String signature, String[] exceptions) {
        return null;
    }

    public void visitOuterClass(String owner, String name, String desc) {
    }

    public void visitSource(String source, String debug) {
    }

    static yeti.lang.compiler.ModuleType readType(ClassReader reader) {
        yeti.lang.compiler.YetiTypeVisitor visitor = new yeti.lang.compiler.YetiTypeVisitor();
        reader.accept(visitor, new Attribute[] { new yeti.lang.compiler.YetiTypeAttr(null) },
                      ClassReader.SKIP_CODE | ClassReader.SKIP_FRAMES);
        if (visitor.typeAttr == null)
            return null;
        yeti.lang.compiler.ModuleType mt = visitor.typeAttr.moduleType;
        if (mt != null)
            mt.deprecated = visitor.deprecated;
        return mt;
    }

    static yeti.lang.compiler.ModuleType getType(YetiParser.Node node, String name,
                              boolean bySourcePath) {
        CompileCtx ctx = CompileCtx.current();
        yeti.lang.compiler.ModuleType t = (yeti.lang.compiler.ModuleType) ctx.types.get(name);
        if (t != null)
            return t;
        String source = name;
        InputStream in = null;
        if (!bySourcePath) {
            source += ".yeti";
            in = ClassFinder.get().findClass(name + ".class");
        }
        try {
            if (in == null) {
                //System.err.println("|" + name + "|source=" + source + "|" + bySourcePath);
                t = (yeti.lang.compiler.ModuleType) ctx.types.get(ctx.compile(source, 0));
                if (t == null)
                    throw new Exception("Could not compile `" + name
                                      + "' to a module");
            } else {
                t = readType(new ClassReader(in));
                in.close();
                if (t == null)
                    throw new Exception("`" + name + "' is not a yeti module");
                t.name = name;
            }
            ctx.types.put(name, t);
            return t;
        } catch (CompileException ex) {
            if (ex.line == 0) {
                ex.line = node.line;
                ex.col = node.col;
            }
            throw ex;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new CompileException(node, ex.getMessage());
        }
    }
}
