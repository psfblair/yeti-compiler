package yeti.lang.compiler.yeti.builtins;

import yeti.renamed.asm3.Label;

final class For extends Core2 {
    For(int line) {
        super("for", YetiType.FOR_TYPE, line);
    }

    void genApply2(Ctx ctx, Code list, Code fun, int line) {
        Function f;
        LoadVar arg = new LoadVar();
        if (!list.flagop(LIST_RANGE) && fun instanceof Function &&
                    (f = (Function) fun).uncapture(arg)) {
            Label retry = new Label(), end = new Label();
            list.gen(ctx);
            ctx.visitLine(line);
            ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
            ctx.insn(DUP);
            ctx.jumpInsn(IFNULL, end);
            ctx.insn(DUP);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                                "isEmpty", "()Z");
            ctx.jumpInsn(IFNE, end);
            // start of loop
            ctx.visitLabel(retry);
            ctx.insn(DUP);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                                "first", "()Ljava/lang/Object;");
            // invoke body block
            ctx.varInsn(ASTORE, arg.var = ctx.localVarCount++);
            ++ctx.tainted; // disable argument-nulling - we're in cycle
            // new closure has to be created on each cycle
            // as closure vars could be captured
            f.genClosureInit(ctx);
            f.body.gen(ctx);
            --ctx.tainted;
            ctx.visitLine(line);
            ctx.insn(POP); // ignore return value
            // next
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AIter",
                                "next", "()Lyeti/lang/AIter;");
            ctx.insn(DUP);
            ctx.jumpInsn(IFNONNULL, retry);
            ctx.visitLabel(end);
        } else {
            Label nop = new Label(), end = new Label();
            list.gen(ctx);
            fun.gen(ctx);
            ctx.visitLine(line);
            ctx.insn(SWAP);
            ctx.typeInsn(CHECKCAST, "yeti/lang/AList");
            ctx.insn(DUP_X1);
            ctx.jumpInsn(IFNULL, nop);
            ctx.methodInsn(INVOKEVIRTUAL, "yeti/lang/AList",
                                "forEach", "(Ljava/lang/Object;)V");
            ctx.jumpInsn(GOTO, end);
            ctx.visitLabel(nop);
            ctx.insn(POP2);
            ctx.visitLabel(end);
            ctx.insn(ACONST_NULL);
        }
    }
}
