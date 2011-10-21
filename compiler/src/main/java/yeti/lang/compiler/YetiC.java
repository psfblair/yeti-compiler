package yeti.lang.compiler;

public interface YetiC {
    public static final int CF_COMPILE_MODULE   = 1;
    public static final int CF_PRINT_PARSE_TREE = 2;
    public static final int CF_EVAL             = 4;
    public static final int CF_EVAL_RESOLVE     = 8;
    public static final int CF_NO_IMPORT        = 16;
    public static final int CF_EVAL_STORE       = 32;
    public static final int CF_EVAL_BIND        = 40;

    static final String[] PRELOAD = { "yeti/lang/std", "yeti/lang/io" };
}