package yeti.lang.compiler.struct;

import yeti.lang.compiler.code.BindRef;
import yeti.lang.compiler.code.Code;
import yeti.renamed.asm3.Opcodes;

public final class StructField implements Opcodes {
    private String name;
    private int property; // 0 - not property, 1 - property, -1 - constant property
    private boolean mutable;
    boolean inherited; // inherited field in with { ... }
    Code value;
    Code setter;
    BindRef binder;
    String javaName;
    StructField nextProperty;
    int index;
    int line;

    public String getName() {
        return name;
    }

    public int getProperty() {
        return property;
    }

    public boolean isMutable() {
        return mutable;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setProperty(int property) {
        this.property = property;
    }

    public void setMutable(boolean mutable) {
        this.mutable = mutable;
    }
}
