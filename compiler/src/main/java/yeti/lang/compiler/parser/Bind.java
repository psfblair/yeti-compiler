package yeti.lang.compiler.parser;

import java.util.List;

public final class Bind extends Node {
    String name;
    Node expr;
    TypeNode type;
    boolean var;
    boolean property;
    boolean noRec;
    String doc;

    Bind() {
    }

    Bind(List args, Node expr, boolean inStruct, String doc) {
        this.doc = doc;
        int first = 0;
        Node nameNode = null;
        while (first < args.size()) {
            nameNode = (Node) args.get(first);
            ++first;
            if (nameNode.kind == "var") {
                var = true;
            } else if (nameNode.kind == "norec") {
                noRec = true;
            } else {
                break;
            }
        }
        if (!var && nameNode instanceof Sym) {
            String s = ((Sym) nameNode).sym;
            if (inStruct && args.size() > first) {
                if (s == "get") {
                    property = true;
                    nameNode = (Node) args.get(first++);
                } else if (s == "set") {
                    property = true;
                    var = true;
                    nameNode = (Node) args.get(first++);
                }
            }
        }
        if (first == 0 || first > args.size()) {
            throw new ParseException(nameNode,
                    "Variable name is missing");
        }
        if (!(nameNode instanceof Sym)) {
            throw new ParseException(nameNode,
                    "Illegal binding name: " + nameNode
                     + " (missing ; after expression?)");
        }
        setLine(nameNode.getLine());
        setCol(nameNode.getCol());
        this.name = ((Sym) nameNode).sym;
        if (first < args.size() && args.get(first) instanceof BinOp &&
                ((BinOp) args.get(first)).op == Parser.FIELD_OP) {
            throw new ParseException((BinOp) args.get(first),
                "Bad argument on binding (use := for assignment, not =)");
        }
        int i = args.size() - 1;
        if (i >= first && args.get(i) instanceof IsOp) {
            type = ((IsOp) args.get(i)).type;
            --i;
        }
        for (; i >= first; --i) {
            expr = XNode.lambda((Node) args.get(i), expr,
                    i == first ? nameNode : null);
        }
        this.expr = expr;
    }

    String str() {
        StringBuffer s = new StringBuffer("(`let ");
        if (doc != null) {
            s.append("/**");
            s.append(doc);
            s.append(" */ ");
        }
        if (noRec)
            s.append("`norec ");
        if (property)
            s.append(var ? "`set " : "`get ");
        else if (var)
            s.append("`var ");
        s.append(name);
        s.append(' ');
        s.append(expr.str());
        s.append(')');
        return s.toString();
    }
}

