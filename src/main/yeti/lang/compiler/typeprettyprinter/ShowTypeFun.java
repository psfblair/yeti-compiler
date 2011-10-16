package yeti.lang.compiler.typeprettyprinter;

import yeti.lang.AIter;
import yeti.lang.AList;
import yeti.lang.Core;
import yeti.lang.Fun;

class ShowTypeFun extends Fun2 {
    Fun showType;
    Fun formatDoc;
    String indentStep = "   ";

    ShowTypeFun() {
        showType = this;
    }

    private void hstr(StringBuffer to, boolean variant,
                      AList fields, String indent) {
        boolean useNL = false;
        AIter i = fields;
        for (int n = 0; i != null; i = i.next())
            if (++n >= 3 || formatDoc != null && ((String) ((Struct)
                                i.first()).get("description")).length() > 0) {
                useNL = true;
                break;
            }

        String indent_ = indent, oldIndent = indent;
        if (useNL) {
            if (!variant)
                indent = indent.concat(indentStep);
            indent_ = indent.concat(indentStep);
        }

        String sep = variant
            ? useNL ? "\n" + indent + "| " : " | "
            : useNL ? ",\n".concat(indent) : ", ";

        for (i = fields; i != null; i = i.next()) {
            Struct field = (Struct) i.first();
            if (i != fields) // not first
                to.append(sep);
            else if (useNL && !variant)
                to.append('\n').append(indent);
            if (formatDoc != null) {
                String doc = (String) field.get("description");
                if (formatDoc != this) {
                    to.append(formatDoc.apply(indent, doc));
                } else if (useNL && doc.length() > 0) {
                    to.append("// ")
                      .append(Core.replace("\n", "\n" + indent + "//", doc))
                      .append('\n')
                      .append(indent);
                }
            }
            if (!variant) {
                if (field.get("mutable") == Boolean.TRUE)
                    to.append("var ");
                to.append(field.get("tag"));
            }
            to.append(field.get("name")).append(variant ? " " : " is ");
            Tag fieldType = (Tag) field.get("type");
            Object tstr = showType.apply(indent_, fieldType);
            if (variant && fieldType.name == "Function")
                to.append('(').append(tstr).append(')');
            else
                to.append(tstr);
        }
        if (useNL && !variant)
            to.append("\n").append(oldIndent);
    }

    public Object apply(Object indent, Object typeObj) {
        Tag type = (Tag) typeObj;
        String typeTag = type.name;
        if (typeTag == "Simple")
            return type.value;
        if (typeTag == "Alias") {
            Struct t = (Struct) type.value;
            return '(' + (String) t.get("alias") + " is " +
                showType.apply(indent, t.get("type")) + ')';
        }

        AList typeList;
        String typeName = null;
        if (typeTag == "Parametric") {
            Struct t = (Struct) type.value;
            typeName = (String) t.get("type");
            typeList = (AList) t.get("params");
        } else {
            typeList = (AList) type.value;
        }
        if (typeList != null && typeList.isEmpty())
            typeList = null;
        AIter i = typeList;
        StringBuffer to = new StringBuffer();

        if (typeName != null) {
            to.append(typeName).append('<');
            for (; i != null; i = i.next()) {
                if (i != typeList)
                    to.append(", ");
                to.append(showType.apply(indent, i.first()));
            }
            to.append('>');
        } else if (typeTag == "Function") {
            while (i != null) {
                Tag t = (Tag) i.first();
                if (i != typeList)
                    to.append(" -> ");
                i = i.next();
                if (i != null && t.name == "Function")
                    to.append('(')
                      .append(showType.apply(indent, t))
                      .append(')');
                else
                    to.append(showType.apply(indent, t));
            }
        } else if (typeTag == "Struct") {
            to.append('{');
            hstr(to, false, typeList, (String) indent);
            to.append('}');
        } else if (typeTag == "Variant") {
            hstr(to, true, typeList, (String) indent);
        } else {
            throw new IllegalArgumentException("Unknown type kind: " + typeTag);
        }
        return to.toString();
    }
}
