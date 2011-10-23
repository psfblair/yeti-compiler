package yeti.lang.compiler.code;

import yeti.lang.compiler.java.JavaType;
import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

//SHOULD THIS BE SPLIT WITH PART BEING A JAVAEXPR?
public final class NewArrayExpr extends Code {
    private Code count;
    private int line;

    public NewArrayExpr(YType type, Code count, int line) {
        setType(type);
        this.count = count;
        this.line = line;
    }

    public void gen(Ctx ctx) {
        if (count != null)
            ctx.genInt(count, line);
        ctx.visitLine(line);
        YType yType = getType().getParam()[0];
        if (yType.getType() != YetiType.JAVA) { // array of arrays
            ctx.typeInsn(ANEWARRAY, JavaType.descriptionOf(yType));
            return;
        }
        JavaType jt = yType.getJavaType();
        int t;
        switch (jt.getDescription().charAt(0)) {
        case 'B': t = T_BYTE; break;
        case 'C': t = T_CHAR; break;
        case 'D': t = T_DOUBLE; break;
        case 'F': t = T_FLOAT; break;
        case 'I': t = T_INT; break;
        case 'J': t = T_LONG; break;
        case 'S': t = T_SHORT; break;
        case 'Z': t = T_BOOLEAN; break;
        case 'L':
            ctx.typeInsn(ANEWARRAY, jt.className());
            return;
        default:
            throw new IllegalStateException("ARRAY<" + jt.getDescription() + '>');
        }
        ctx.visitIntInsn(NEWARRAY, t);
    }
}

