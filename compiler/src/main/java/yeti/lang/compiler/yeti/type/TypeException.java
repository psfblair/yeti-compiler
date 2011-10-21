package yeti.lang.compiler.yeti.type;

import yeti.lang.compiler.yeti.type.YType;

public class TypeException extends Exception {
    private boolean special;
    private YType a, b;
    String sep, ext;

    public boolean isSpecial() {
        return special;
    }

    TypeException(String what) {
        super(what);
    }

    TypeException(YType a_, YType b_) {
        a = a_;
        b = b_;
        sep = " is not ";
        ext = "";
    }

    TypeException(YType a_, String sep_, YType b_, String ext_) {
        a = a_;
        b = b_;
        sep = sep_;
        ext = ext_;
    }

    public String getMessage() {
        return getMessage(null);
    }

    public String getMessage(Scope scope) {
        if (a == null)
            return super.getMessage();
        return "Type mismatch: " + a.toString(scope) +
               sep + b.toString(scope) + ext;
    }
}
