package yeti.lang.compiler.code;

import yeti.lang.compiler.closure.CaptureWrapper;

public final class BindWrapper extends BindRef {
    private BindRef ref;

    public BindWrapper(BindRef ref) {
        this.ref = ref;
        setBinder(ref.getBinder());
        setType(ref.getType());
        setPolymorph(ref.isPolymorph());
        setOrigin(ref.getOrigin());
    }

    protected CaptureWrapper capture() {
        return ref.capture();
    }

    protected boolean flagop(int fl) {
        return (fl & (PURE | ASSIGN | DIRECT_BIND)) != 0 && ref.flagop(fl);
    }

    public void gen(Ctx ctx) {
        ref.gen(ctx);
    }
}