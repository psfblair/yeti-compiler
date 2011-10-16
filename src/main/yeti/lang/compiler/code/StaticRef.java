package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YType;

class StaticRef extends BindRef {
    String className;
    protected String funFieldName;
    int line;
   
    StaticRef(String className, String fieldName, YType type, Binder binder, boolean polymorph, int line) {
        setType(type);
        setBinder(binder);
        this.className = className;
        this.funFieldName = fieldName;
        this.polymorph = polymorph;
        this.line = line;
    }
    
    public void gen(Ctx ctx) {
        ctx.visitLine(line);
        ctx.fieldInsn(GETSTATIC, className, funFieldName,
                             'L' + javaType(getType()) + ';');
    }

    protected boolean flagop(int fl) {
        return (fl & DIRECT_BIND) != 0;
    }
}