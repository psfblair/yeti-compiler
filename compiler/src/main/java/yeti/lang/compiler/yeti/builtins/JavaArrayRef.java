package yeti.lang.compiler.yeti.builtins;

final class JavaArrayRef extends Code implements CodeGen {
    Code value, index;
    YType elementType;
    int line;

    JavaArrayRef(YType _type, Code _value, Code _index, int _line) {
        type = JavaType.convertValueType(elementType = _type);
        value = _value;
        index = _index;
        line = _line;
    }

    private void _gen(Ctx ctx, Code store) {
        value.gen(ctx);
        ctx.typeInsn(CHECKCAST, JavaType.descriptionOf(value.type));
        ctx.genInt(index, line);
        String resDescr = elementType.javaType == null
                            ? JavaType.descriptionOf(elementType)
                            : elementType.javaType.description;
        int insn = BALOAD;
        switch (resDescr.charAt(0)) {
        case 'C':
            insn = CALOAD;
            break;
        case 'D':
            insn = DALOAD;
            break;
        case 'F':
            insn = FALOAD;
            break;
        case 'I':
            insn = IALOAD;
            break;
        case 'J':
            insn = LALOAD;
            break;
        case 'S':
            insn = SALOAD;
            break;
        case 'L':
            resDescr = resDescr.substring(1, resDescr.length() - 1);
        case '[':
            insn = AALOAD;
            break;
        }
        if (store != null) {
            insn += 33;
            JavaExpr.genValue(ctx, store, elementType, line);
            if (insn == AASTORE)
                ctx.typeInsn(CHECKCAST, resDescr);
        }
        ctx.insn(insn);
        if (insn == AALOAD) {
            ctx.forceType(resDescr);
        }
    }

    void gen(Ctx ctx) {
        _gen(ctx, null);
        JavaExpr.convertValue(ctx, elementType);
    }

    public void gen2(Ctx ctx, Code setValue, int line) {
        _gen(ctx, setValue);
        ctx.insn(ACONST_NULL);
    }

    Code assign(final Code setValue) {
        return new SimpleCode(this, setValue, null, 0);
    }
}
