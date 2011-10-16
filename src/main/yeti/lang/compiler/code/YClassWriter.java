package yeti.lang.compiler.code;

import yeti.renamed.asm3.ClassWriter;

final class YClassWriter extends ClassWriter {
    YClassWriter(int flags) {
        super(COMPUTE_MAXS | flags);
    }

    // Overload to avoid using reflection on non-standard-library classes
    protected String getCommonSuperClass(String type1, String type2) {
        if (type1.equals(type2)) {
            return type1;
        }
        if (type1.startsWith("java/lang/") && type2.startsWith("java/lang/") ||
            type1.startsWith("yeti/lang/") && type2.startsWith("yeti/lang/")) {
            return super.getCommonSuperClass(type1, type2);
        }
        return "java/lang/Object";
    }
}

