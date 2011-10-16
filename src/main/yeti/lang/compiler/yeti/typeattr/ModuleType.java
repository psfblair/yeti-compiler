package yeti.lang.compiler.yeti.typeattr;

import yeti.lang.compiler.yeti.type.YType;
import yeti.lang.compiler.yeti.type.YetiType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModuleType {
    YType type;
    Map typeDefs;
    private Map directFields;
    private String name;
    private boolean deprecated;
    Scope typeScope;
    String topDoc;
    private YType[] free;

    ModuleType(YType type, Map typeDefs, Map directFields) {
        this.type = type;
        this.typeDefs = typeDefs;
        this.directFields = directFields;
    }

    public String getName() {
        return name;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public Map getDirectFields() {
        return directFields;
    }

    public void setDirectFields(Map directFields) {
        this.directFields = directFields;
    }

    public YType copy(int depth) {
        if (depth == -1)
            return type;
        if (free == null) {
            List freeVars = new ArrayList();
            YetiType.getFreeVar(freeVars, freeVars, type,
                    YetiType.RESTRICT_POLY, -1);
            free = (YType[]) freeVars.toArray(new YType[freeVars.size()]);
        }
        return YetiType.copyType(type, YetiType.createFreeVars(free, depth),
                                 new HashMap());
    }

    Tag yetiType() {
        return TypeDescr.yetiType(type, typeScope != null
                ? TypePattern.toPattern(typeScope)
                : TypePattern.toPattern(typeDefs));
    }
}
