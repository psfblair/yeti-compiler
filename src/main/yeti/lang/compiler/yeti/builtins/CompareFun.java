package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class CompareFun extends BoolBinOp {
    static final int COND_EQ  = 0;
    static final int COND_NOT = 1;
    static final int COND_LT  = 2;
    static final int COND_GT  = 4;
    static final int COND_LE  = COND_NOT | COND_GT;
    static final int COND_GE  = COND_NOT | COND_LT;
    static final int[] OPS = { IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE };
    static final int[] ROP = { IFEQ, IFNE, IFGT, IFLE, IFLT, IFGE };
    int op;
    int line;

    void binGenIf(Ctx ctx, Code arg1, Code arg2, Label to, boolean ifTrue) {
        YType t = arg1.type.deref();
        int op = this.op;
        boolean eq = (op & (COND_LT | COND_GT)) == 0;
        if (!ifTrue) {
            op ^= COND_NOT;
        }
        Label nojmp = null;
        if (t.type == YetiType.VAR || t.type == YetiType.MAP &&
                t.param[2] == YetiType.LIST_TYPE &&
                t.param[1].type != YetiType.NUM) {
            Label nonull = new Label();
            nojmp = new Label();
            arg2.gen(ctx);
            arg1.gen(ctx); // 2-1
            ctx.visitLine(line);
            ctx.insn(DUP); // 2-1-1
            ctx.jumpInsn(IFNONNULL, nonull); // 2-1
            // reach here, when 1 was null
            if (op == COND_GT || op == COND_LE ||
                arg2.flagop(EMPTY_LIST) &&
                    (op == COND_EQ || op == COND_NOT)) {
                // null is never greater and always less or equal
                ctx.insn(POP2);
                ctx.jumpInsn(GOTO,
                    op == COND_LE || op == COND_EQ ? to : nojmp);
            } else {
                ctx.insn(POP); // 2
                ctx.jumpInsn(op == COND_EQ || op == COND_GE
                                    ? IFNULL : IFNONNULL, to);
                ctx.jumpInsn(GOTO, nojmp);
            }
            ctx.visitLabel(nonull);
            if (!eq && ctx.compilation.isGCJ)
                ctx.typeInsn(CHECKCAST, "java/lang/Comparable");
            ctx.insn(SWAP); // 1-2
        } else if (arg2 instanceof StringConstant &&
                   ((StringConstant) arg2).str.length() == 0 &&
                   (op & COND_LT) == 0) {
            arg1.gen(ctx);
            ctx.visitLine(line);
            ctx.typeInsn(CHECKCAST, "java/lang/String");
            ctx.methodInsn(INVOKEVIRTUAL, "java/lang/String", "length", "()I");
            ctx.jumpInsn((op & COND_NOT) == (op >>> 2) ? IFEQ : IFNE, to);
            return;
        } else {
            arg1.gen(ctx);
            ctx.visitLine(line);
            if (arg2.flagop(INT_NUM)) {
                ctx.typeInsn(CHECKCAST, "yeti/lang/Num");
                ((NumericConstant) arg2).genInt(ctx, false);
                ctx.visitLine(line);
                ctx.methodInsn(INVOKEVIRTUAL,
                        "yeti/lang/Num", "rCompare", "(J)I");
                ctx.jumpInsn(ROP[op], to);
                return;
            }
            if (!eq && ctx.compilation.isGCJ)
                ctx.typeInsn(CHECKCAST, "java/lang/Comparable");
            arg2.gen(ctx);
            ctx.visitLine(line);
        }
        if (eq) {
            op ^= COND_NOT;
            ctx.methodInsn(INVOKEVIRTUAL, "java/lang/Object",
                                "equals", "(Ljava/lang/Object;)Z");
        } else {
            ctx.methodInsn(INVOKEINTERFACE, "java/lang/Comparable",
                                "compareTo", "(Ljava/lang/Object;)I");
        }
        ctx.jumpInsn(OPS[op], to);
        if (nojmp != null) {
            ctx.visitLabel(nojmp);
        }
    }
}
