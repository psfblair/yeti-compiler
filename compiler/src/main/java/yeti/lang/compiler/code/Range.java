package yeti.lang.compiler.code;

import yeti.lang.compiler.yeti.type.YetiType;

final class Range extends Code {
    final Code from;
    final Code to;

    Range(Code from, Code to) {
        setType(YetiType.NUM_TYPE);
        this.from = from;
        this.to = to;
    }

    public void gen(Ctx ctx) {
        from.gen(ctx);
        to.gen(ctx);
    }
}
