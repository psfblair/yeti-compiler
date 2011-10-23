package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;
import yeti.lang.compiler.yeti.typeattr.ModuleType;

public final class LoadModule extends Code {
    String moduleName;
    ModuleType moduleType;
    boolean checkUsed;
    private boolean used;

    LoadModule(String moduleName, ModuleType type, int depth) {
        setType(type.copy(depth));
        this.moduleName = moduleName;
        moduleType = type;
        setPolymorph(true);
    }

    public void gen(Ctx ctx) {
        if (checkUsed && !used)
            ctx.insn(ACONST_NULL);
        else
            ctx.methodInsn(INVOKESTATIC, moduleName,
                "eval", "()Ljava/lang/Object;");
    }

    Binder bindField(final String name, final YType type) {
        return new Binder() {
            public BindRef getRef(final int line) {
                String directRef = (String) moduleType.getDirectFields().get(name);
                if (directRef != null && !directRef.equals("."))
                    return new StaticRef(directRef, "_", type,
                                         this, true, line);
                if (directRef == null)
                    used = true;

                // constant field
                if (directRef != null || // "." - static final on module
                        !moduleType.getDirectFields().containsKey(name))
                    return new StaticRef(moduleName, mangle(name), type, this, true, line);

                // property or mutable field
                final boolean mutable = type.getField() == YetiType.FIELD_MUTABLE;
                return new SelectMember(type, LoadModule.this, name, line, false) {
                    boolean mayAssign() {
                        return mutable;
                    }

                    public boolean flagop(int fl) {
                        return (fl & DIRECT_BIND) != 0 ||
                               (fl & ASSIGN) != 0 && mutable;
                    }
                };
            }
        };
    }
}

