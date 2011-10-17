package yeti.lang.compiler.yeti.type;

class TypeException extends Exception {
    boolean special;
    private yeti.lang.compiler.YType a, b;
    String sep, ext;

    TypeException(String what) {
        super(what);
    }

    TypeException(yeti.lang.compiler.YType a_, yeti.lang.compiler.YType b_) {
        a = a_;
        b = b_;
        sep = " is not ";
        ext = "";
    }

    TypeException(yeti.lang.compiler.YType a_, String sep_, yeti.lang.compiler.YType b_, String ext_) {
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
