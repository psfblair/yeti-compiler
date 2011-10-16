package yeti.lang.compiler.struct;

import java.util.Arrays;
import java.util.Map;

final class WithStruct extends Code {
    private Code src;
    private Code override;
    private String[] names;

    WithStruct(YType type, Code src, Code override,
               String[] names) {
        this.type = type;
        this.src = src;
        this.override = override;
        this.names = names;
        this.polymorph = src.polymorph && override.polymorph;
    }

    void gen(Ctx ctx) {
        Map srcFields = src.type.deref().finalMembers;
        if (srcFields != null && override instanceof yeti.lang.compiler.StructConstructor) {
            ((yeti.lang.compiler.StructConstructor) override).genWith(ctx, src, srcFields);
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
        ctx.constants.stringArray(ctx, a);
        ctx.intConst(srcFields != null ? 1 : 0);
        ctx.visitInit("yeti/lang/WithStruct",
               "(Lyeti/lang/Struct;Lyeti/lang/Struct;[Ljava/lang/String;Z)V");
        ctx.forceType("yeti/lang/Struct");
    }
}
