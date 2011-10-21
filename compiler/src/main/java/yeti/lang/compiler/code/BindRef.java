package yeti.lang.compiler.code;

import yeti.lang.compiler.closure.Apply;
import yeti.lang.compiler.closure.CaptureWrapper;
import yeti.lang.compiler.yeti.type.YType;

public abstract class BindRef extends Code {
    private Binder binder;
    private BindExpr.Ref origin;

    public Binder getBinder() {
        return binder;
    }

    public void setBinder(Binder binder) {
        this.binder = binder;
    }

    public BindExpr.Ref getOrigin() {
        return origin;
    }

    public void setOrigin(BindExpr.Ref origin) {
        this.origin = origin;
    }

    protected Code apply(Code arg, YType res, int line) {
        Apply a = new Apply(res, this, arg, line);
        a.setRef(origin);
        if (origin != null)
            origin.setArity(1);
        return a;
    }

    // some bindrefs care about being captured. most wont.
    public CaptureWrapper capture() {
        return null;
    }

    // unshare. normally bindrefs are not shared
    // Capture shares refs and therefore has to copy for unshareing
    BindRef unshare() {
        return this;
    }

    public Code unref(boolean force) {
        return null;
    }

    // Some bindings can be forced into direct mode
    void forceDirect() {
        throw new UnsupportedOperationException();
    }
}
