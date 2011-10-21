package yeti.lang.compiler.struct;

import yeti.lang.compiler.code.Code;
import yeti.lang.compiler.code.Ctx;
import yeti.lang.compiler.yeti.type.YType;

import java.util.Arrays;
import java.util.Map;

final class WithStruct extends Code {
    private Code src;
    private Code override;
    private String[] names;

    WithStruct(YType type, Code src, Code override,
               String[] names) {
        setType(type);
        this.src = src;
        this.override = override;
        this.names = names;
        setPolymorph(src.isPolymorph() && override.isPolymorph());
    }

    public void gen(Ctx ctx) {
        Map srcFields = src.getType().deref().getFinalMembers();
        if (srcFields != null && override instanceof StructConstructor) {
            ((StructConstructor) override).genWith(ctx, src, srcFields);
            return;
        }

        ctx.typeInsn(NEW, "yeti/lang/WithStruct");
        ctx.insn(DUP);
        src.gen(ctx);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Struct");
        override.gen(ctx);
        ctx.typeInsn(CHECKCAST, "yeti/lang/Struct");
        Arrays.sort(names);
        String[] a = new String[names.length + 1];
        System.arraycopy(names, 0, a, 1, names.length);
        ctx.getConstants().stringArray(ctx, a);
        ctx.intConst(srcFields != null ? 1 : 0);
        ctx.visitInit("yeti/lang/WithStruct",
               "(Lyeti/lang/Struct;Lyeti/lang/Struct;[Ljava/lang/String;Z)V");
        ctx.forceType("yeti/lang/Struct");
    }
}
